package com.cropdeal.adminservice.command.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddOnRepository extends JpaRepository<AddOn, Long> {
    boolean existsByName(String name);
    Optional<AddOn> findByName(String name);
}
