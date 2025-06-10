package com.example.demo1111111.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo1111111.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
  // 添加通过sessionId查找用户的方法
  Optional<User> findBySessionId(String sessionId);

  Optional<User> findByUsername(String username);
}
