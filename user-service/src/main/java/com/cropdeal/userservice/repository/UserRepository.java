package com.cropdeal.userservice.repository;

import com.cropdeal.userservice.entity.OAuthProvider;
import com.cropdeal.userservice.entity.User;
import com.cropdeal.userservice.entity.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    Page<User> findAll(Pageable pageable);

    Page<User> findByRole(UserRole role, Pageable pageable);

    List<User> findByActiveTrue();

    Optional<User> findByOauthProviderAndOauthProviderId(OAuthProvider provider, String providerId);

    @Query("SELECT u FROM User u WHERE u.active = true AND u.role = :role")
    List<User> findActiveUsersByRole(@Param("role") UserRole role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") UserRole role);
}
