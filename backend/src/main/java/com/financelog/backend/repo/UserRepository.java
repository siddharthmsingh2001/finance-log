package com.financelog.backend.repo;

import com.financelog.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByCognitoSub(UUID cognitoSub);

    boolean existsByEmail(String email);

    boolean existsByCognitoSub(UUID cognitoSub);

}
