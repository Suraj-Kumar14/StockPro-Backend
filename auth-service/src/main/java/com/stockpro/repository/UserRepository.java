package com.stockpro.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.stockpro.entity.User;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findAllByRole(String role);
    List<User> findByDepartment(String department);
    List<User> findByIsActive(boolean isActive);
    boolean existsByEmail(String email);
    User findByUserId(Long userId);
    void deleteByUserId(Long userId);
}