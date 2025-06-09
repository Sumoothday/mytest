package com.example.demo1111111.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo1111111.entity.Item;
import com.example.demo1111111.entity.RoomItem;

@Repository
public interface RoomItemRepository extends JpaRepository<RoomItem, RoomItem.RoomItemId> {
  List<RoomItem> findByItem(Item item); // 使用 RoomItemId 作为主键类型

  // ==================== 方法命名查询（复合主键路径） ====================
  boolean existsByIdRoomIdAndIdItemId(Integer roomId, Integer itemId); // ✅ 正确路径

  @Modifying
  @Query("DELETE FROM RoomItem ri WHERE ri.id.roomId = :roomId AND ri.id.itemId = :itemId")
  void deleteByIdRoomIdAndIdItemId( // ✅ 方法名与查询一致
      @Param("roomId") Integer roomId, @Param("itemId") Integer itemId);

  @Query("SELECT ri FROM RoomItem ri WHERE ri.room.id = :roomId")
  List<RoomItem> findByRoomId(@Param("roomId") Integer roomId);

  // ==================== 原有查询保持不变 ====================
  @Query("SELECT ri FROM RoomItem ri " + "JOIN FETCH ri.item " + "WHERE ri.room.id = :roomId")
  List<RoomItem> findByRoomIdWithItems(@Param("roomId") Integer roomId);

  @Query("SELECT ri FROM RoomItem ri WHERE ri.room.id = :roomId AND ri.item.id = :itemId")
  Optional<RoomItem> findByRoomIdAndItemId(
      @Param("roomId") Integer roomId, @Param("itemId") Integer itemId);
}
