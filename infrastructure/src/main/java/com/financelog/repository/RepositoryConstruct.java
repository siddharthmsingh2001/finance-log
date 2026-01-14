package com.financelog.repository;

import java.util.Collections;
import java.util.Objects;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.LifecycleRule;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.iam.AccountPrincipal;
import software.constructs.Construct;

/**
 * A CDK Construct responsible for provisioning and configuring
 * an Amazon Elastic Container Registry (ECR) repository.
 *
 * <p>
 * This construct encapsulates all ECR-related infrastructure concerns
 * for the application, including:
 * </p>
 *
 * <ul>
 *   <li>Creation of an ECR repository</li>
 *   <li>Lifecycle policies to control image retention</li>
 *   <li>IAM permissions for pushing and pulling images</li>
 * </ul>
 *
 * <p>
 * Architectural intent:
 * <br>
 * The repository is modeled as a dedicated construct so that:
 * </p>
 *
 * <ul>
 *   <li>ECR can be deployed independently from compute resources</li>
 *   <li>Image storage lifecycle can be managed separately</li>
 *   <li>The repository can exist before any ECS services are deployed</li>
 * </ul>
 *
 * <p>
 * This aligns with the "one app per stack" approach where foundational
 * infrastructure (like container registries) is decoupled from runtime
 * infrastructure (like ECS services).
 * </p>
 */
public class RepositoryConstruct extends Construct{

    /**
     * The ECR repository instance created by this construct.
     *
     * <p>
     * Exposed as an {@link IRepository} interface rather than a concrete
     * {@link Repository} implementation to:
     * </p>
     *
     * <ul>
     *   <li>Reduce coupling to a specific implementation</li>
     *   <li>Allow the repository to be referenced by other constructs or stacks</li>
     *   <li>Support importing existing repositories if needed later</li>
     * </ul>
     */
    private final IRepository repository;

    /**
     * Creates an ECR repository with lifecycle and access policies applied.
     *
     * <p>
     * This constructor performs the following actions:
     * </p>
     *
     * <ol>
     *   <li>Creates an ECR repository with a deterministic name</li>
     *   <li>Applies a lifecycle rule to limit stored images</li>
     *   <li>Configures deletion behavior based on environment needs</li>
     *   <li>Grants push/pull permissions to the owning AWS account</li>
     * </ol>
     *
     * @param scope
     *   The parent construct in the CDK construct tree.
     *   Typically a {@link software.amazon.awscdk.Stack}.
     *
     * @param constructId
     *   Logical identifier for this construct within its scope.
     *
     * @param inputParameters
     *   Immutable configuration object defining repository behavior
     *   such as naming, retention, and access control.
     */
    public RepositoryConstruct(
            final Construct scope,
            final String constructId,
            final RepositoryInputParameters inputParameters
    ){
        super(scope, constructId);

        // Create the ECR repository resource
        this.repository = Repository.Builder.create(this, "EcrRepository")

                // Repository name is derived from the application name to ensure
                // consistency across environments and stacks
                .repositoryName(inputParameters.repositoryName)

                // Removal policy controls what happens to the repository
                // when the CloudFormation stack is deleted:
                // - RETAIN is useful for production to prevent accidental data loss
                // - DESTROY is suitable for ephemeral or non-critical environments
                .removalPolicy(inputParameters.retainRegistryOnDelete ? RemovalPolicy.RETAIN:RemovalPolicy.DESTROY)

                // Lifecycle rules prevent unbounded image growth,
                // which could otherwise lead to unnecessary storage costs
                // - rulePriority dictates which image to delete first
                // - description describes the lifecycle rule
                // - maxImageCount dictates the maximum number of images to be stored in the repo before the old ones get deleted
                .lifecycleRules(Collections.singletonList(LifecycleRule.builder()
                        .rulePriority(1)
                        .description(String.format("Limit to %d images", inputParameters.maxImageCount))
                        .maxImageCount(inputParameters.maxImageCount)
                        .build())
                )
                .build();

        // Grant push and pull permissions to the specified AWS account.
        //
        // This is required so that:
        // - CI/CD pipelines can push images
        // - ECS tasks can pull images at runtime
        //
        // Permissions are granted explicitly instead of relying on defaults
        // to keep IAM intent clear and auditable.
        this.repository.grantPullPush(
                new AccountPrincipal(inputParameters.accountId
                ));
    }

    /**
     * Immutable configuration holder for {@link RepositoryConstruct}.
     *
     * <p>
     * This class exists to:
     * </p>
     *
     * <ul>
     *   <li>Avoid long constructor argument lists</li>
     *   <li>Make configuration intent explicit</li>
     *   <li>Encourage validation at construction time</li>
     * </ul>
     *
     * <p>
     * Once created, instances of this class cannot be modified.
     * </p>
     */
    public static class RepositoryInputParameters{

        /**
         * AWS Account ID that will be granted permission
         * to push and pull images from the repository.
         */
        private final String accountId;

        /**
         * Logical application name used as the base
         * for the repository name.
         */
        private final String repositoryName;

        /**
         * Maximum number of container images
         * retained in the repository.
         */
        private final int maxImageCount;

        /**
         * Determines whether the repository should be retained
         * or destroyed when the stack is deleted.
         */
        private final boolean retainRegistryOnDelete;

        /**
         * Creates a validated configuration object for the repository.
         *
         * @param repositoryName
         *   Base name used for constructing the ECR repository name.
         *
         * @param accountId
         *   AWS Account ID that receives push/pull permissions.
         *
         * @param maxImageCount
         *   Maximum number of images retained by lifecycle rules.
         *
         * @param retainRegistryOnDelete
         *   Whether the repository should survive stack deletion.
         */
        public  RepositoryInputParameters(String repositoryName, String accountId, int maxImageCount, boolean retainRegistryOnDelete){
            Objects.requireNonNull(accountId, "accountId must not be null");
            Objects.requireNonNull(repositoryName, "repositoryName must not be null");
            this.accountId = accountId;
            this.repositoryName = repositoryName;
            this.maxImageCount = maxImageCount;
            this.retainRegistryOnDelete = retainRegistryOnDelete;
        }
    }

    /**
     * Exposes the created ECR repository.
     *
     * <p>
     * This allows other constructs or stacks to:
     * </p>
     *
     * <ul>
     *   <li>Reference the repository</li>
     *   <li>Grant additional permissions</li>
     *   <li>Use it in ECS task definitions</li>
     * </ul>
     *
     * @return the ECR repository created by this construct
     */
    public IRepository getEcrRepository() {
        return repository;
    }
}
