package com.stockpro.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stockpro.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long>{

    Optional<User> findByEmail(String email);

    User findByUserId(Long userId);

    boolean existsByEmail(String email);

    List<User> findAllByRole(String role);

    List<User> findByDepartment(String department);

    List<User> findByIsActive(boolean isActive);

    void deleteByUserId(Long userId);
}
