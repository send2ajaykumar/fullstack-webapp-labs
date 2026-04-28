package com.ajay.epicquest.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ajay.epicquest.model.User;
import com.ajay.epicquest.model.enums.Role;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByRole(Role role);
}
