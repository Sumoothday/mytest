package com.example.demo1111111.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo1111111.entity.PlayerInventory;

@Repository
public interface PlayerInventoryRepository extends JpaRepository<PlayerInventory, Long> {
  // 修改查询方法为基于用户ID
  Optional<PlayerInventory> findByUserIdAndItemId(Long userId, Long itemId);

  List<PlayerInventory> findByUserId(Long userId);
}
