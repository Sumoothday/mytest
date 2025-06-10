package com.example.demo1111111.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo1111111.entity.RoomEntity;

public interface RoomRepository extends JpaRepository<RoomEntity, Integer> {

  // 1. 核心方法：通过名称加载完整房间数据
  @Query("SELECT r FROM RoomEntity r WHERE r.name = :name")
  @EntityGraph(
      attributePaths = {
        "exitEast",
        "exitWest",
        "exitNorth",
        "exitSouth",
        "roomItems.item" // 确保物品关联也被加载
      })
  Optional<RoomEntity> findByNameWithFullData(@Param("name") String name);

  // 2. 通过ID加载完整房间数据
  @Query("SELECT r FROM RoomEntity r WHERE r.id = :id")
  @EntityGraph(
      attributePaths = {
        "exitEast",
        "exitWest",
        "exitNorth",
        "exitSouth",
        "roomItems.item" // 确保物品关联也被加载
      })
  Optional<RoomEntity> findByIdWithFullData(@Param("id") Integer id);

  // 3. 轻量级方法：仅加载出口信息
  @Query("SELECT r FROM RoomEntity r WHERE r.name = :name")
  @EntityGraph(attributePaths = {"exitEast", "exitWest", "exitNorth", "exitSouth"})
  Optional<RoomEntity> findByNameWithExits(@Param("name") String name);

  // 4. 基本方法
  Optional<RoomEntity> findByName(String name);
}
