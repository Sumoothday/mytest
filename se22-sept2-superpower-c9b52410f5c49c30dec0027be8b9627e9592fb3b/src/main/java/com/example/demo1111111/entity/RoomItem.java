package com.example.demo1111111.entity;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.*;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "room_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"room", "item"})
@Builder
public class RoomItem {

  // ==================== 主键 ====================
  @EmbeddedId private RoomItemId id = new RoomItemId();

  // ==================== 关联关系 ====================
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("roomId")
  @JoinColumn(name = "room_id")
  @JsonBackReference
  private RoomEntity room;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("itemId")
  @JoinColumn(name = "item_id")
  @JsonIgnoreProperties({"roomItems", "baseRoom"}) // 防止循环引用
  private Item item;

  // ==================== 业务字段 ====================
  @Column(nullable = false)
  private Integer quantity = 1;

  // ==================== 嵌入的复合主键类 ====================
  @Embeddable
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RoomItemId implements Serializable {
    @Column(name = "room_id")
    private Integer roomId;

    @Column(name = "item_id")
    private Integer itemId;

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      RoomItemId that = (RoomItemId) o;
      return Objects.equals(roomId, that.roomId) && Objects.equals(itemId, that.itemId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(roomId, itemId);
    }
  }

  // ==================== 便捷构造方法 ====================
  public RoomItem(RoomEntity room, Item item, int quantity) {
    this.room = room;
    this.item = item;
    this.quantity = quantity;
    this.id =
        new RoomItemId(room != null ? room.getId() : null, item != null ? item.getId() : null);
  }

  // ==================== 安全设置方法 ====================
  public void setRoom(RoomEntity room) {
    this.room = room;
    if (this.id == null) {
      this.id = new RoomItemId();
    }
    this.id.setRoomId(room != null ? room.getId() : null);
  }

  public void setItem(Item item) {
    this.item = item;
    if (this.id == null) {
      this.id = new RoomItemId();
    }
    this.id.setItemId(item != null ? item.getId() : null);
  }

  // ==================== 业务方法 ====================
  /** 增加物品数量 */
  public void addQuantity(int amount) {
    if (amount > 0) {
      this.quantity += amount;
    }
  }

  /** 减少物品数量（不删除记录） */
  public boolean removeQuantity(int amount) {
    if (amount > 0 && this.quantity >= amount) {
      this.quantity -= amount;
      return true;
    }
    return false;
  }

  /** 计算物品堆重量 */
  @JsonIgnore
  public double getTotalWeight() {
    return item != null ? item.getWeight() * quantity : 0;
  }
}
