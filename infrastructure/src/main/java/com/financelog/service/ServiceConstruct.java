package com.financelog.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import com.financelog.core.ApplicationEnvironment;
import com.financelog.network.NetworkOutputParameters;
import java.util.Map;
import software.amazon.awscdk.CfnCondition;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ec2.CfnSecurityGroup;
import software.amazon.awscdk.services.ec2.CfnSecurityGroupIngress;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.CfnService;
import software.amazon.awscdk.services.ecs.CfnTaskDefinition;
import software.amazon.awscdk.services.elasticloadbalancingv2.CfnListenerRule;
import software.amazon.awscdk.services.elasticloadbalancingv2.CfnTargetGroup;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

/**
 * High-level ECS service construct that wires together all runtime infrastructure
 * required to run a Dockerized application on Amazon ECS using Fargate.
 *
 * <p>
 * This construct is responsible for turning a static Docker image into a
 * fully operational, load-balanced, self-healing service.
 * </p>
 *
 * <p>
 * Conceptually, this construct performs the following steps:
 * </p>
 *
 * <ol>
 *   <li>Create a Target Group that represents running ECS tasks</li>
 *   <li>Attach routing rules to existing Load Balancer listeners</li>
 *   <li>Create centralized logging infrastructure</li>
 *   <li>Create IAM roles for ECS and for the application runtime</li>
 *   <li>Resolve and authorize the Docker image source</li>
 *   <li>Define how containers should be started (Task Definition)</li>
 *   <li>Create security boundaries for the running tasks</li>
 *   <li>Start and manage the application using an ECS Service</li>
 * </ol>
 *
 * <p>
 * This construct intentionally does <strong>not</strong> create foundational
 * network resources such as:
 * </p>
 *
 * <ul>
 *   <li>VPC</li>
 *   <li>ECS Cluster</li>
 *   <li>Application Load Balancer</li>
 * </ul>
 *
 * <p>
 * Those are expected to be created by a dedicated {@code NetworkConstruct}
 * and passed in via {@link NetworkOutputParameters}.
 * </p>
 *
 * <p>
 * The result is a clean separation between:
 * </p>
 *
 * <ul>
 *   <li><strong>Platform infrastructure</strong> (network, cluster, ALB)</li>
 *   <li><strong>Service runtime</strong> (tasks, scaling, routing, IAM)</li>
 * </ul>
 */
public class ServiceConstruct extends Construct{

    /**
     * Creates a fully configured ECS service and all supporting resources
     * required to run a containerized application on Fargate.
     *
     * <p>
     * Construction order matters and reflects real runtime dependencies:
     * </p>
     *
     * <ol>
     *   <li>Target group must exist before listener rules can reference it</li>
     *   <li>Listener rules must exist before ECS service registration</li>
     *   <li>IAM roles must exist before task definition creation</li>
     *   <li>Task definition must exist before ECS service creation</li>
     * </ol>
     *
     * @param scope
     *   Parent construct (usually a Stack)
     *
     * @param constructId
     *   Logical ID within the construct tree
     *
     * @param awsEnvironment
     *   AWS account and region information
     *
     * @param applicationEnvironment
     *   Logical environment descriptor (dev, staging, prod) used
     *   for naming and tagging
     *
     * @param serviceInputParameters
     *   All tunable configuration for the service runtime
     *
     * @param networkOutputParameters
     *   Network-level identifiers created by the Network construct
     */
    public ServiceConstruct(
            final Construct scope,
            final String constructId,
            final Environment awsEnvironment,
            final ApplicationEnvironment applicationEnvironment,
            final ServiceInputParameters serviceInputParameters,
            final NetworkOutputParameters networkOutputParameters
    ){
        super(scope, constructId);

        // Represents the dynamically changing set of running ECS tasks
        CfnTargetGroup targetGroup = createTargetGroup(serviceInputParameters, networkOutputParameters);

        // Defines how incoming traffic is routed from the ALB to this service
        List<CfnListenerRule> listenerRules = createListenerRules(targetGroup, serviceInputParameters, networkOutputParameters);

        // Centralized CloudWatch Log Group for all container logs
        LogGroup logGroup = createLogGroup(applicationEnvironment, serviceInputParameters);

        // IAM role assumed by ECS itself (image pulls, log shipping)
        Role ecsTaskExecutionRole = createTaskExecutionRole(applicationEnvironment);
        // IAM role assumed by the application running inside the container
        Role ecsTaskRole = createTaskRole(applicationEnvironment, serviceInputParameters);

        // Resolve Docker image URI and grant pull permissions if necessary
        String dockerImage = resolveDockerImage(serviceInputParameters,ecsTaskExecutionRole);

        // Declarative blueprint describing how containers should be started
        CfnTaskDefinition taskDefinition = createTaskDefinition(
                awsEnvironment,
                applicationEnvironment,
                serviceInputParameters,
                logGroup,
                ecsTaskExecutionRole,
                ecsTaskRole,
                dockerImage
        );

        // Network-level firewall attached directly to each running ECS task
        CfnSecurityGroup ecsSecurityGroup = createEcsSecurityGroup(
                serviceInputParameters,
                networkOutputParameters
        );

        // Control plane that keeps tasks running, healthy, and load-balanced
        CfnService ecsService = createEcsService(
                serviceInputParameters,
                networkOutputParameters,
                taskDefinition,
                ecsSecurityGroup,
                targetGroup,
                applicationEnvironment
        );

        // Ensure listener rules exist before ECS service
        // listenerRules.forEach(rule ->
        //     ecsService.getNode().addDependency(rule)
        // );
        ecsService.getNode().addDependency(targetGroup);

        // Apply environment-wide tags for cost allocation and observability
        applicationEnvironment.tag(this);
    }

    /**
     * Creates an Application Load Balancer target group that represents
     * the running ECS tasks of this service.
     *
     * <p>
     * Because ECS Fargate tasks use {@code awsvpc} networking, targets
     * are registered by private IP address rather than instance ID.
     * </p>
     */
    private CfnTargetGroup createTargetGroup(
            ServiceInputParameters serviceInputParameters,
            NetworkOutputParameters networkOutputParameters
    ){
        List<CfnTargetGroup.TargetGroupAttributeProperty> attributes = buildTargetGroupAttributes(serviceInputParameters);
        return CfnTargetGroup.Builder.create(this, "TargetGroup")
                .vpcId(networkOutputParameters.getVpcId())
                .port(serviceInputParameters.containerPort)
                .protocol(serviceInputParameters.containerProtocol)
                .targetType("ip")
                .healthCheckPath(serviceInputParameters.healthCheckPath)
                .healthCheckPort(String.valueOf(serviceInputParameters.healthCheckPort))
                .healthCheckProtocol(serviceInputParameters.healthCheckProtocol)
                .healthCheckIntervalSeconds(serviceInputParameters.healthCheckIntervalSeconds)
                .healthCheckTimeoutSeconds(serviceInputParameters.healthCheckTimeoutSeconds)
                .healthyThresholdCount(serviceInputParameters.healthyThresholdCount)
                .unhealthyThresholdCount(serviceInputParameters.unhealthyThresholdCount)
                .targetGroupAttributes(attributes)
                .build();
    }

    /**
     * Builds optional target group attributes such as deregistration delay
     * and session stickiness.
     *
     * <p>
     * These attributes control how the load balancer behaves during
     * deployments and user session routing.
     * </p>
     */
    private List<CfnTargetGroup.TargetGroupAttributeProperty> buildTargetGroupAttributes(ServiceInputParameters params) {
        // Allow in-flight requests to finish before deregistering a task
        List<CfnTargetGroup.TargetGroupAttributeProperty> attributes = new ArrayList<>();
        attributes.add(
                CfnTargetGroup.TargetGroupAttributeProperty.builder()
                        .key("deregistration_delay.timeout_seconds") // wait 5 seconds before stopping traffic to this task if failed health check.
                        .value("5")
                        .build()
        );

        /*
         * Sticky sessions ensure that once a client is routed to a specific
         * ECS task, subsequent requests from that client are routed to the
         * same task.
         *
         * This is crucial for applications that store session state
         * in memory (e.g. classic Spring MVC monoliths).
         *
         * The ALB manages stickiness via its own cookies and does not
         * rely on application-level session cookies.
         */
        if (params.stickySessionsEnabled) {
            attributes.addAll(List.of(
                    attr("stickiness.enabled", "true"), //enable sticky session
                    attr("stickiness.type", "lb_cookie"), // type of cookie to be used (now this is the only option left in cdk v2)
                    attr("stickiness.lb_cookie.duration_seconds", "3600") // invalidate the cookie after this make seconds
            ));
        }
        return attributes;
    }

    private CfnTargetGroup.TargetGroupAttributeProperty attr(String key, String value) {
        return CfnTargetGroup.TargetGroupAttributeProperty.builder()
                .key(key)
                .value(value)
                .build();
    }

    /**
     * Creates routing rules that attach this service to existing
     * Application Load Balancer listeners.
     *
     * <p>
     * A listener accepts incoming traffic, but listener rules decide
     * what happens to that traffic (forward, redirect, or respond).
     * </p>
     */
    private List<CfnListenerRule> createListenerRules(
            CfnTargetGroup targetGroup,
            ServiceInputParameters inputParameters,
            NetworkOutputParameters outputParameters
    ) {
        List<CfnListenerRule> rules = new ArrayList<>();

        // Forward matching requests to this service's target group
        CfnListenerRule.ActionProperty action = CfnListenerRule.ActionProperty.builder()
                .type("forward")
                .targetGroupArn(targetGroup.getRef())
                .build();

        // Match all paths; can be narrowed for microservices
        CfnListenerRule.RuleConditionProperty condition = CfnListenerRule.RuleConditionProperty.builder()
                .field("path-pattern")
                .values(List.of("*"))
                .build();

        // HTTP listener rule
        CfnListenerRule httpRule = CfnListenerRule.Builder.create(this, "HttpListenerRule")
                .listenerArn(outputParameters.getHttpListenerArn())
                .priority(inputParameters.httpListenerPriority)
                .actions(List.of(action))
                .conditions(List.of(condition))
                .build();

        rules.add(httpRule);

        // HTTPS listener rule (optional)
        outputParameters.getHttpsListenerArn().ifPresent( httpsArn -> {
            CfnListenerRule httpsRule = CfnListenerRule.Builder.create(this, "HttpsListenerRule")
                    .listenerArn(httpsArn)
                    .priority(1)
                    .actions(List.of(action))
                    .conditions(List.of(condition))
                    .build();
            // Creating a safety check condition to only create the Listener only if the value is not "null"
            CfnCondition conditionResource = CfnCondition.Builder.create(this, "HttpsListenerCondition")
                    .expression(Fn.conditionNot(Fn.conditionEquals(httpsArn, "null")))
                    .build();
            httpsRule.getCfnOptions().setCondition(conditionResource);
            rules.add(httpsRule);
        });

        return rules;
    }

    /**
     * Creates a CloudWatch Log Group used by all ECS tasks of this service.
     *
     * <p>
     * Each running task creates one or more log streams inside this group.
     * </p>
     */
    private LogGroup createLogGroup(
            ApplicationEnvironment env,
            ServiceInputParameters parms
    ){
        return LogGroup.Builder.create(this, "EcsLogGroup")
                .logGroupName(env.prefix("log-group"))
                .retention(parms.logRetention) // How long should cloudwatch keep these logs before deleteing them automatically
                .removalPolicy(RemovalPolicy.DESTROY) // What to do with logs when the resource is deleted.
                .build();
    }

    /**
     * IAM role assumed by the ECS control plane to:
     *
     * <ul>
     *   <li>Pull Docker images from ECR</li>
     *   <li>Publish container logs to CloudWatch</li>
     * </ul>
     */
    private Role createTaskExecutionRole(ApplicationEnvironment env){
        return Role.Builder.create(this, "EcsTaskExecutionRole")
                .assumedBy(ServicePrincipal.Builder.create("ecs-tasks.amazonaws.com").build())
                .inlinePolicies( Map.of(
                                env.prefix("ecs-taskExecutionRolePolicy"), PolicyDocument.Builder.create()
                                        .statements(
                                                List.of(
                                                        PolicyStatement.Builder.create()
                                                                .effect(Effect.ALLOW)
                                                                .resources(List.of("*"))
                                                                .actions(
                                                                        List.of(
                                                                                // Pull Docker Images from ECR
                                                                                "ecr:GetAuthorizationToken",
                                                                                "ecr:BatchCheckLayerAvailability",
                                                                                "ecr:GetDownloadUrlForLayer",
                                                                                "ecr:BatchGetImage",
                                                                                // Push container logs to CloudWatch
                                                                                "logs:CreateLogStream",
                                                                                "logs:PutLogEvents"
                                                                        )
                                                                ).build()
                                                )
                                        ).build()
                        )
                )
                .build();
    }

    /**
     * IAM role assumed by the application code running inside the container.
     *
     * <p>
     * This role defines what AWS services the application itself
     * is allowed to access.
     * </p>
     */
    private Role createTaskRole(
            ApplicationEnvironment env,
            ServiceInputParameters params
    ){
        Role.Builder roleBuilder = Role.Builder.create(this, "EcsTaskRole")
                .assumedBy(ServicePrincipal.Builder.create("ecs-tasks.amazonaws.com").build());
        if (!params.taskRolePolicyStatements.isEmpty()) {
            roleBuilder.inlinePolicies(Map.of(
                    env.prefix("ecsTaskRolePolicy"),
                    PolicyDocument.Builder.create()
                            .statements(params.taskRolePolicyStatements)
                            .build()
            ));
        }
        return roleBuilder.build();
    }

    /**
     * Resolves the Docker image URI and grants pull permissions if the
     * image is stored in Amazon ECR.
     */
    private String resolveDockerImage(
            ServiceInputParameters params,
            Role executionRole
    ){
        if(params.dockerImageSource.isEcrSource()){
            IRepository repo = Repository.fromRepositoryName(
                    this,
                    "EcrRepository",
                    params.dockerImageSource.getDockerRepositoryName()
            );
            repo.grantPull(executionRole); // tells IAM what that role is allowed to do.
            return repo.repositoryUriForTag(params.dockerImageSource.getDockerImageTag()); // returns the full image uri that will be used by the task definition.
        }
        return params.dockerImageSource.dockerImageUrl;
    }

    /**
     * Creates the ECS Task Definition that describes how containers
     * should be started.
     */
    private CfnTaskDefinition createTaskDefinition(
            Environment awsEnv,
            ApplicationEnvironment appEnv,
            ServiceInputParameters params,
            LogGroup logGroup,
            Role executionRole,
            Role taskRole,
            String image
    ){
        CfnTaskDefinition.ContainerDefinitionProperty container =
                CfnTaskDefinition.ContainerDefinitionProperty.builder()
                        .name(containerName(appEnv))
                        .image(image)
                        .cpu(params.cpu)
                        .memory(params.memory)
                        .portMappings(List.of(
                                CfnTaskDefinition.PortMappingProperty.builder()
                                        .containerPort(params.containerPort) // container listens on this property
                                        .build()
                        ))
                        .environment(toKeyValuePairs(params.environmentVariables)) // environment variable that we may need to pass to the application inside the container
                        .logConfiguration(
                                CfnTaskDefinition.LogConfigurationProperty.builder()
                                        .logDriver("awslogs") // Use CloudWatch Logs to send stdout/stderr
                                        .options(Map.of(
                                                "awslogs-group", logGroup.getLogGroupName(), // LogGroup you created earlier
                                                "awslogs-region", awsEnv.getRegion(), // AWS region
                                                "awslogs-stream-prefix", appEnv.prefix("stream"), // prefix to attach to the stream
                                                "awslogs-datetime-format", params.awslogsDateTimeFormat
                                        ))
                                        .build()
                        )
                        .build();

        return CfnTaskDefinition.Builder.create(this, "TaskDefinition")
                .networkMode("awsvpc")
                .requiresCompatibilities(List.of("FARGATE"))
                .cpu(String.valueOf(params.cpu))
                .memory(String.valueOf(params.memory))
                .executionRoleArn(executionRole.getRoleArn()) // role the ECS assumes inorder to pull images and write logs.
                .taskRoleArn(taskRole.getRoleArn())
                .containerDefinitions(List.of(container))
                .build();
    }

    /**
     * Creates the security group that acts as the network firewall
     * for each ECS task.
     */
    private CfnSecurityGroup createEcsSecurityGroup(
            ServiceInputParameters inputParams,
            NetworkOutputParameters outputParams
    ) {
        CfnSecurityGroup sg = CfnSecurityGroup.Builder.create(this, "EcsSecurityGroup")
                .vpcId(outputParams.getVpcId())
                .groupDescription("SecurityGroup for ECS tasks")
                .build();

        // Allow one ECS task to talk other ECS tsak in the same Subnet
        CfnSecurityGroupIngress.Builder.create(this, "EcsIngressSelf")
                .groupId(sg.getAttrGroupId())
                .sourceSecurityGroupId(sg.getAttrGroupId())
                .ipProtocol("-1")
                .build();

        // Allow traffic from ALB to this ECS.
        CfnSecurityGroupIngress.Builder.create(this, "EcsIngressFromAlb")
                .groupId(sg.getAttrGroupId())
                .sourceSecurityGroupId(outputParams.getLoadbalancerSecurityGroupId())
                .ipProtocol("-1")
                .build();

        // Allow ECS tasks to initiate connections to OTHER resources
        int i = 1;
        for(String securityGroupId: inputParams.securityGroupIdsToGrantIngressFromEcs){
            CfnSecurityGroupIngress.Builder.create(this, "EcsSecurityGroupIngress"+i)
                    .sourceSecurityGroupId(sg.getAttrGroupId())
                    .groupId(securityGroupId)
                    .ipProtocol("-1")
                    .build();
            i++;
        }

        return sg;
    }

    /**
     * Creates the ECS Service that keeps the application running,
     * healthy, and load-balanced.
     */
    private CfnService createEcsService(
            ServiceInputParameters inputParams,
            NetworkOutputParameters outputParams,
            CfnTaskDefinition taskDefinition,
            CfnSecurityGroup securityGroup,
            CfnTargetGroup targetGroup,
            ApplicationEnvironment appEnv
    ) {

        CfnService service = CfnService.Builder.create(this, "EcsService")
                .cluster(outputParams.getEcsClusterName())
                .launchType("FARGATE")
                .desiredCount(inputParams.desiredInstancesCount)
                .taskDefinition(taskDefinition.getRef())
                .healthCheckGracePeriodSeconds(120)
                .deploymentConfiguration(
                        CfnService.DeploymentConfigurationProperty.builder()
                                .maximumPercent(inputParams.maximumInstancesPercent)
                                .minimumHealthyPercent(inputParams.minimumHealthyInstancesPercent)
                                .build()
                )
                .networkConfiguration(
                        CfnService.NetworkConfigurationProperty.builder()
                                .awsvpcConfiguration(
                                        CfnService.AwsVpcConfigurationProperty.builder()
                                                // .assignPublicIp("ENABLED") // necessary if task must call external API, pull from public registries
                                                // .subnets(outputParams.getPublicSubnets())
                                                .assignPublicIp("DISABLED") // when ECS is in the private subnet
                                                .subnets(outputParams.getIsolatedSubnets())
                                                .securityGroups(List.of(securityGroup.getAttrGroupId()))
                                                .build()
                                )
                                .build()
                )
                .loadBalancers(List.of(
                        CfnService.LoadBalancerProperty.builder()
                                .containerName(containerName(appEnv))
                                .containerPort(inputParams.containerPort)
                                .targetGroupArn(targetGroup.getRef())
                                .build()
                ))
                .build();

        return service;
    }

    private String containerName(ApplicationEnvironment applicationEnvironment) {
        return applicationEnvironment.prefix("container");
    }

    public List<CfnTaskDefinition.KeyValuePairProperty> toKeyValuePairs(Map<String, String> map) {
        List<CfnTaskDefinition.KeyValuePairProperty> keyValuePairs = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            keyValuePairs.add(toKeyValuePair(entry.getKey(), entry.getValue()));
        }
        return keyValuePairs;
    }

    private CfnTaskDefinition.KeyValuePairProperty toKeyValuePair(String key, String value) {
        return CfnTaskDefinition.KeyValuePairProperty.builder()
                .name(key)
                .value(value)
                .build();
    }

    /**
     * Configuration object that defines how an application is deployed and run
     * as an ECS Fargate Service.
     *
     * <p>
     * This class represents the complete set of "knobs and dials" that control:
     * </p>
     *
     * <ul>
     *   <li>Which Docker image is deployed</li>
     *   <li>How the container is started (CPU, memory, ports)</li>
     *   <li>How traffic is routed and health-checked</li>
     *   <li>How deployments are performed (rolling update behavior)</li>
     *   <li>How the application integrates with AWS (IAM permissions)</li>
     *   <li>How logs are retained and structured</li>
     * </ul>
     *
     * <p>
     * The default values are intentionally chosen so that a typical
     * Spring Boot application will run out of the box without additional
     * configuration.
     * </p>
     *
     * <p>
     * This class follows a fluent builder-style API:
     * </p>
     *
     * <pre>
     * ServiceInputParameters params = new ServiceInputParameters(image, env)
     *     .withCpu(512)
     *     .withMemory(1024)
     *     .withDesiredInstances(2)
     *     .withStickySessionsEnabled(true);
     * </pre>
     *
     * <p>
     * Changing values here will typically result in a new ECS Task Definition
     * revision and/or a rolling deployment of the ECS Service.
     * </p>
     */
    public static class ServiceInputParameters {

        /**
         * Defines where the Docker image for this service comes from.
         *
         * <p>
         * This may either point to:
         * </p>
         * <ul>
         *   <li>An Amazon ECR repository (private, IAM-controlled)</li>
         *   <li>An external Docker registry (e.g. Docker Hub)</li>
         * </ul>
         *
         * <p>
         * The image reference is resolved into a fully qualified image URI
         * and injected into the ECS Task Definition.
         * </p>
         */
        private final DockerImageSource dockerImageSource;

        /**
         * Environment variables passed into the container runtime.
         *
         * <p>
         * These are injected directly into the container and are typically
         * consumed by the application framework (e.g. Spring Boot).
         * </p>
         *
         * <p><strong>Important:</strong></p>
         * <ul>
         *   <li>Values are stored in plain text in the Task Definition</li>
         *   <li>Do not put secrets here</li>
         *   <li>Secrets should come from SSM or Secrets Manager</li>
         * </ul>
         */
        private final Map<String, String> environmentVariables;

        /**
         * List of security group IDs that should allow inbound traffic
         * originating from this ECS service.
         *
         * <p>
         * This is primarily used to allow this service to connect to
         * downstream resources such as:
         * </p>
         *
         * <ul>
         *   <li>Databases</li>
         *   <li>Caches (Redis, Memcached)</li>
         *   <li>Internal services</li>
         * </ul>
         *
         * <p>
         * For each security group ID provided here, an ingress rule is created
         * that allows traffic from the ECS service's security group.
         * </p>
         */
        private final List<String> securityGroupIdsToGrantIngressFromEcs;

        /**
         * IAM policy statements that define what AWS resources the application
         * running inside the container is allowed to access.
         *
         * <p>
         * These statements are attached to the <strong>Task Role</strong>,
         * not the Task Execution Role.
         * </p>
         *
         * <p>
         * Typical examples include:
         * </p>
         *
         * <ul>
         *   <li>Reading from SQS queues</li>
         *   <li>Publishing to SNS topics</li>
         *   <li>Accessing S3 buckets</li>
         * </ul>
         *
         * <p>
         * If empty, the application runs without any AWS permissions
         * beyond basic networking.
         * </p>
         */
        private List<PolicyStatement> taskRolePolicyStatements = new ArrayList<>();

        /**
         * Interval (in seconds) between consecutive health checks
         * performed by the Application Load Balancer.
         *
         * <p>
         * Shorter intervals detect failures faster but increase load.
         * </p>
         */
        private int healthCheckIntervalSeconds = 30;

        /**
         * HTTP path that the load balancer calls to determine
         * whether a container instance is healthy.
         *
         * <p>
         * This endpoint must return a successful HTTP status code
         * (2xx) when the application is healthy.
         * </p>
         */
        private String healthCheckPath = "/actuator/health";

        /**
         * Port on which the application listens inside the container.
         *
         * <p>
         * This value must match:
         * </p>
         * <ul>
         *   <li>The container port mapping</li>
         *   <li>The target group port</li>
         * </ul>
         */
        private int containerPort = 8080;

        /**
         * Protocol used by the load balancer to communicate
         * with the container.
         *
         * <p>
         * TLS termination happens at the ALB, not inside the container.
         * </p>
         */
        private String containerProtocol = "HTTP";

        /**
         * Port used by the load balancer when performing health checks.
         *
         * <p>
         * Defaults to the same port the application listens on.
         * </p>
         */
        private int healthCheckPort = containerPort;

        /**
         * Protocol used for health check requests.
         *
         * <p>
         * Defaults to the same protocol used to access the application.
         * </p>
         */
        private String healthCheckProtocol = containerProtocol;

        /**
         * Number of seconds to wait for a health check response
         * before marking it as failed.
         */
        private int healthCheckTimeoutSeconds = 5;

        /**
         * Number of consecutive successful health checks required
         * before a target is marked as healthy.
         */
        private int healthyThresholdCount = 2;

        /**
         * Number of consecutive failed health checks required
         * before a target is marked as unhealthy.
         */
        private int unhealthyThresholdCount = 3;

        /**
         * Retention period for application logs stored in CloudWatch Logs.
         *
         * <p>
         * Logs older than this duration are automatically deleted.
         * </p>
         */
        private RetentionDays logRetention = RetentionDays.THREE_DAYS;

        /**
         * Date-time format used by the awslogs log driver to extract
         * timestamps from log messages.
         *
         * <p>
         * This is particularly important for multi-line log entries
         * and structured JSON logging.
         * </p>
         */
        private int cpu = 256;

        /**
         * CPU units allocated to each running task.
         *
         * <p>
         * This controls both scheduling and billing.
         * </p>
         */
        private int memory = 512;

        /**
         * Memory (in MB) allocated to each running task.
         *
         * <p>
         * Exceeding this limit will cause the container to be terminated.
         * </p>
         */
        private int desiredInstancesCount = 2;

        /**
         * Number of task instances that should be running in parallel.
         */
        private int maximumInstancesPercent = 200;

        /**
         * Maximum percentage of desired instances that may run
         * during deployments.
         *
         * <p>
         * Used to control how many extra tasks may be started
         * during rolling updates.
         * </p>
         */
        private int minimumHealthyInstancesPercent = 50;

        /**
         * Minimum percentage of desired instances that must remain
         * healthy during deployments.
         *
         * <p>
         * Prevents all instances from being terminated at once.
         * </p>
         */
        private boolean stickySessionsEnabled = false;

        /**
         * Enables or disables sticky sessions at the load balancer level.
         *
         * <p>
         * When enabled, requests from the same client are routed
         * to the same container instance.
         * </p>
         */
        private String awslogsDateTimeFormat = "%Y-%m-%dT%H:%M:%S.%f%z";

        /**
         * Priority of the HTTP listener rule associated with this service.
         *
         * <p>
         * Listener rule priorities must be unique per listener.
         * Lower numbers are evaluated first.
         * </p>
         */
        private int httpListenerPriority = 2;

        /**
         * Knobs and dials you can configure to run a Docker image in an ECS service. The default values are set in a way
         * to work out of the box with a Spring Boot application.
         *
         * @param dockerImageSource                     the source from where to load the Docker image that we want to deploy.
         * @param securityGroupIdsToGrantIngressFromEcs Ids of the security groups that the ECS containers should be granted access to.
         * @param environmentVariables                  the environment variables provided to the Java runtime within the Docker containers.
         */
        public ServiceInputParameters(
                DockerImageSource dockerImageSource,
                List<String> securityGroupIdsToGrantIngressFromEcs,
                Map<String, String> environmentVariables) {
            this.dockerImageSource = dockerImageSource;
            this.environmentVariables = environmentVariables;
            this.securityGroupIdsToGrantIngressFromEcs = securityGroupIdsToGrantIngressFromEcs;
        }

        /**
         * Knobs and dials you can configure to run a Docker image in an ECS service. The default values are set in a way
         * to work out of the box with a Spring Boot application.
         *
         * @param dockerImageSource    the source from where to load the Docker image that we want to deploy.
         * @param environmentVariables the environment variables provided to the Java runtime within the Docker containers.
         */
        public ServiceInputParameters(
                DockerImageSource dockerImageSource,
                Map<String, String> environmentVariables) {
            this.dockerImageSource = dockerImageSource;
            this.environmentVariables = environmentVariables;
            this.securityGroupIdsToGrantIngressFromEcs = Collections.emptyList();
        }

        /**
         * The port that application uses for health checks
         * <p>
         * Default: "same as that of application"
         */
        public ServiceInputParameters withHealthCheckPort(int healthCheckPort){
            this.healthCheckPort = healthCheckPort;
            return this;
        }

        /**
         * The protocol that application uses for health checks
         * <p>
         * Default: "same as that of application"
         */
        public ServiceInputParameters withHealthCheckProtocol(String healthCheckProtocol){
            this.healthCheckProtocol = healthCheckProtocol;
            return this;
        }

        /**
         * The interval to wait between two health checks.
         * <p>
         * Default: 15.
         */
        public ServiceInputParameters withHealthCheckIntervalSeconds(int healthCheckIntervalSeconds) {
            this.healthCheckIntervalSeconds = healthCheckIntervalSeconds;
            return this;
        }

        /**
         * The path of the health check URL.
         * <p>
         * Default: "/actuator/health".
         */
        public ServiceInputParameters withHealthCheckPath(String healthCheckPath) {
            Objects.requireNonNull(healthCheckPath);
            this.healthCheckPath = healthCheckPath;
            return this;
        }

        /**
         * The port the application listens on within the container.
         * <p>
         * Default: 8080.
         */
        public ServiceInputParameters withContainerPort(int containerPort) {
            this.containerPort = containerPort;
            return this;
        }

        /**
         * The protocol to access the application within the container. Default: "HTTP".
         */
        public ServiceInputParameters withContainerProtocol(String containerProtocol) {
            Objects.requireNonNull(containerProtocol);
            this.containerProtocol = containerProtocol;
            return this;
        }

        /**
         * The number of seconds to wait for a response until a health check is deemed unsuccessful.
         * <p>
         * Default: 5.
         */
        public ServiceInputParameters withHealthCheckTimeoutSeconds(int healthCheckTimeoutSeconds) {
            this.healthCheckTimeoutSeconds = healthCheckTimeoutSeconds;
            return this;
        }

        /**
         * The number of consecutive successful health checks after which an instance is declared healthy.
         * <p>
         * Default: 2.
         */
        public ServiceInputParameters withHealthyThresholdCount(int healthyThresholdCount) {
            this.healthyThresholdCount = healthyThresholdCount;
            return this;
        }

        /**
         * The number of consecutive unsuccessful health checks after which an instance is declared unhealthy.
         * <p>
         * Default: 8.
         */
        public ServiceInputParameters withUnhealthyThresholdCount(int unhealthyThresholdCount) {
            this.unhealthyThresholdCount = unhealthyThresholdCount;
            return this;
        }

        /**
         * The number of CPU units allocated to each instance of the application. See
         * <a href="https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-cpu-memory-error.html">the docs</a>
         * for a table of valid values.
         * <p>
         * Default: 256 (0.25 CPUs).
         */
        public ServiceInputParameters withCpu(int cpu) {
            this.cpu = cpu;
            return this;
        }

        /**
         * The memory allocated to each instance of the application in megabytes. See
         * <a href="https://docs.aws.amazon.com/AmazonECS/latest/developerguide/task-cpu-memory-error.html">the docs</a>
         * for a table of valid values.
         * <p>
         * Default: 512.
         */
        public ServiceInputParameters withMemory(int memory) {
            this.memory = memory;
            return this;
        }

        /**
         * The duration the logs of the application should be retained.
         * <p>
         * Default: 1 week.
         */
        public ServiceInputParameters withLogRetention(RetentionDays logRetention) {
            Objects.requireNonNull(logRetention);
            this.logRetention = logRetention;
            return this;
        }

        /**
         * The number of instances that should run in parallel behind the load balancer.
         * <p>
         * Default: 2.
         */
        public ServiceInputParameters withDesiredInstances(int desiredInstances) {
            this.desiredInstancesCount = desiredInstances;
            return this;
        }

        /**
         * The maximum percentage in relation to the desired instances that may be running at the same time
         * (for example during deployments).
         * <p>
         * Default: 200.
         */
        public ServiceInputParameters withMaximumInstancesPercent(int maximumInstancesPercent) {
            this.maximumInstancesPercent = maximumInstancesPercent;
            return this;
        }

        /**
         * The minimum percentage in relation to the desired instances that must be running at the same time
         * (for example during deployments).
         * <p>
         * Default: 50.
         */
        public ServiceInputParameters withMinimumHealthyInstancesPercent(int minimumHealthyInstancesPercent) {
            this.minimumHealthyInstancesPercent = minimumHealthyInstancesPercent;
            return this;
        }

        /**
         * The list of PolicyStatement objects that define which operations this service can perform on other
         * AWS resources (for example ALLOW sqs:GetQueueUrl for all SQS queues).
         * <p>
         * Default: none (empty list).
         */
        public ServiceInputParameters withTaskRolePolicyStatements(List<PolicyStatement> taskRolePolicyStatements) {
            this.taskRolePolicyStatements = taskRolePolicyStatements;
            return this;
        }

        /**
         * Disable or enable sticky sessions for the the load balancer.
         * <p>
         * Default: false.
         */
        public ServiceInputParameters withStickySessionsEnabled(boolean stickySessionsEnabled) {
            this.stickySessionsEnabled = stickySessionsEnabled;
            return this;
        }

        /**
         * The format of the date time used in log entries. The awslogs driver will use this pattern to extract
         * the timestamp from a log event and also to distinguish between multiple multi-line log events.
         * <p>
         * Default: %Y-%m-%dT%H:%M:%S.%f%z (to work with JSON formatted logs created with <a href="https://github.com/osiegmar/logback-awslogs-json-encoder">awslogs JSON Encoder</a>).
         * <p>
         * See also: <a href="https://docs.docker.com/config/containers/logging/awslogs/#awslogs-datetime-format">awslogs driver</a>
         */
        public ServiceInputParameters withAwsLogsDateTimeFormat(String awsLogsDateTimeFormat) {
            this.awslogsDateTimeFormat = awsLogsDateTimeFormat;
            return this;
        }

        /**
         * The priority for the HTTP listener of the loadbalancer. The priority of two listeners must not be the same
         * so you need to choose different priorities for different services.
         */
        public ServiceInputParameters withHttpListenerPriority(int priority) {
            this.httpListenerPriority = priority;
            return this;
        }

    }

    /**
     * Value object that describes where the Docker image for an ECS service
     * should be loaded from.
     *
     * <p>
     * This class deliberately supports <strong>exactly one</strong> image source
     * at a time and models it as a mutually exclusive choice:
     * </p>
     *
     * <ul>
     *   <li><strong>External image</strong> (e.g. Docker Hub, GHCR, custom registry)</li>
     *   <li><strong>Amazon ECR image</strong> (private, IAM-protected)</li>
     * </ul>
     *
     * <p>
     * The purpose of this class is not to interact with AWS directly, but to
     * <em>describe intent</em>. The {@code ServiceConstruct} uses this description
     * to:
     * </p>
     *
     * <ul>
     *   <li>Resolve a fully-qualified Docker image URI</li>
     *   <li>Grant the ECS Task Execution Role permission to pull from ECR</li>
     *   <li>Remain agnostic about where the image physically lives</li>
     * </ul>
     *
     * <p>
     * This design allows the same ECS service construct to be used for:
     * </p>
     *
     * <ul>
     *   <li>Local development images</li>
     *   <li>Public images</li>
     *   <li>Private production images stored in ECR</li>
     * </ul>
     *
     * <p><strong>Invariant:</strong></p>
     * <ul>
     *   <li>If {@code dockerRepositoryName != null}, the image is assumed to be in ECR</li>
     *   <li>If {@code dockerImageUrl != null}, the image is assumed to be external</li>
     *   <li>Exactly one of these must be non-null</li>
     * </ul>
     */
    public static class DockerImageSource{

        /**
         * Name of the Amazon ECR repository that contains the Docker image.
         *
         * <p>
         * This value is only set when the image is sourced from ECR.
         * It is used to:
         * </p>
         *
         * <ul>
         *   <li>Look up an existing ECR repository by name</li>
         *   <li>Construct the fully-qualified ECR image URI</li>
         *   <li>Grant pull permissions to the ECS Task Execution Role</li>
         * </ul>
         *
         * <p>
         * When this field is non-null, {@code dockerImageUrl} is guaranteed
         * to be null.
         * </p>
         */
        private final String dockerRepositoryName;

        /**
         * Tag of the Docker image inside the ECR repository.
         *
         * <p>
         * Typical examples include:
         * </p>
         *
         * <ul>
         *   <li>{@code latest}</li>
         *   <li>{@code 1.0.0}</li>
         *   <li>{@code commit-sha}</li>
         * </ul>
         *
         * <p>
         * This value is only meaningful when {@code dockerRepositoryName}
         * is non-null.
         * </p>
         */
        private final String dockerImageTag;

        /**
         * Fully-qualified Docker image reference for images not stored in ECR.
         *
         * <p>
         * Examples:
         * </p>
         *
         * <ul>
         *   <li>{@code nginx:latest}</li>
         *   <li>{@code postgres:15}</li>
         *   <li>{@code ghcr.io/org/app:1.2.3}</li>
         * </ul>
         *
         * <p>
         * When this field is non-null, {@code dockerRepositoryName} and
         * {@code dockerImageTag} are guaranteed to be null.
         * </p>
         */
        private final String dockerImageUrl;

        /**
         * Creates a Docker image source that points to an external registry.
         *
         * <p>
         * Use this constructor when the image is:
         * </p>
         *
         * <ul>
         *   <li>Publicly accessible</li>
         *   <li>Hosted outside Amazon ECR</li>
         *   <li>Does not require IAM-based authentication</li>
         * </ul>
         *
         * <p>
         * In this case, no ECR permissions are granted and ECS will pull
         * the image directly from the registry.
         * </p>
         *
         * @param dockerImageUrl
         *   Fully-qualified Docker image reference
         *   (must include image name and optionally a tag).
         */
        public DockerImageSource(String dockerImageUrl){
            Objects.requireNonNull(dockerImageUrl);
            this.dockerImageUrl = dockerImageUrl;
            this.dockerImageTag = null;
            this.dockerRepositoryName = null;
        }

        /**
         * Creates a Docker image source that points to an Amazon ECR repository.
         *
         * <p>
         * Use this constructor when the image is:
         * </p>
         *
         * <ul>
         *   <li>Stored in a private Amazon ECR repository</li>
         *   <li>Protected by IAM permissions</li>
         *   <li>Part of a CI/CD pipeline that pushes images to ECR</li>
         * </ul>
         *
         * <p>
         * The {@code ServiceConstruct} will:
         * </p>
         *
         * <ul>
         *   <li>Import the ECR repository by name</li>
         *   <li>Grant the ECS Task Execution Role permission to pull the image</li>
         *   <li>Resolve the repository URI and tag into a full image reference</li>
         * </ul>
         *
         * @param dockerRepositoryName
         *   Name of the ECR repository (must already exist).
         *
         * @param dockerImageTag
         *   Tag of the Docker image inside the repository.
         */
        public DockerImageSource(String dockerRepositoryName, String dockerImageTag) {
            Objects.requireNonNull(dockerRepositoryName);
            Objects.requireNonNull(dockerImageTag);
            this.dockerRepositoryName = dockerRepositoryName;
            this.dockerImageTag = dockerImageTag;
            this.dockerImageUrl = null;
        }

        /**
         * Indicates whether this image source refers to an Amazon ECR repository.
         *
         * <p>
         * This method is used by the service construct to decide whether:
         * </p>
         *
         * <ul>
         *   <li>ECR-specific logic should be applied</li>
         *   <li>IAM permissions for image pulling must be granted</li>
         * </ul>
         *
         * @return {@code true} if the image is sourced from ECR,
         *         {@code false} if it is sourced from an external registry.
         */
        public boolean isEcrSource() {
            return this.dockerRepositoryName != null;
        }

        /**
         * Returns the name of the ECR repository containing the image.
         *
         * <p>
         * Only valid if {@link #isEcrSource()} returns {@code true}.
         * </p>
         */
        public String getDockerRepositoryName() {
            return dockerRepositoryName;
        }

        /**
         * Returns the tag of the Docker image inside the ECR repository.
         *
         * <p>
         * Only valid if {@link #isEcrSource()} returns {@code true}.
         * </p>
         */
        public String getDockerImageTag() {
            return dockerImageTag;
        }

        /**
         * Returns the fully-qualified Docker image reference for
         * externally hosted images.
         *
         * <p>
         * Only valid if {@link #isEcrSource()} returns {@code false}.
         * </p>
         */
        public String getDockerImageUrl() {
            return dockerImageUrl;
        }
    }
}
