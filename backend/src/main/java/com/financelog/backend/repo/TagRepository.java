package com.financelog.backend.repo;

import com.financelog.backend.entity.Tag;
import com.financelog.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findByUser(User user);

    Optional<Tag> findByUserAndNameIgnoreCase(User user, String name);

    boolean existsByUserAndNameIgnoreCase(User user, String name);

}
