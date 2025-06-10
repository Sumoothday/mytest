package com.example.demo1111111.entity;

import java.io.Serializable;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teleport_destinations")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class TeleportDestination {

  // ===== 主键部分 =====
  @EmbeddedId private TeleportDestinationId id;

  // ===== 关联实体部分 =====
  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("teleportRoomId")
  @JoinColumn(name = "teleport_room_id", referencedColumnName = "id")
  @ToString.Exclude
  private RoomEntity teleportRoom;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("destinationRoomId")
  @JoinColumn(name = "destination_room_id", referencedColumnName = "id")
  @ToString.Exclude
  private RoomEntity destination;

  // ===== 业务方法部分 =====

  /** 便捷构造函数 - 直接创建关系 */
  public TeleportDestination(RoomEntity teleportRoom, RoomEntity destination) {
    this.teleportRoom = teleportRoom;
    this.destination = destination;

    // 安全创建复合ID
    Integer teleportId = teleportRoom != null ? teleportRoom.getId() : null;
    Integer destinationId = destination != null ? destination.getId() : null;
    this.id = new TeleportDestinationId(teleportId, destinationId);
  }

  /** 用于初始化的便捷方法 */
  @PrePersist
  protected void onCreate() {
    if (id == null && teleportRoom != null && destination != null) {
      this.id = new TeleportDestinationId(teleportRoom.getId(), destination.getId());
    }
  }

  // ===== 完整toString方法 =====
  @Override
  public String toString() {
    return "TeleportDestination{"
        + "id="
        + id
        + ", teleportRoomId="
        + (teleportRoom != null ? teleportRoom.getId() : "null")
        + ", destinationRoomId="
        + (destination != null ? destination.getId() : "null")
        + '}';
  }

  // ===== 复合主键类 =====
  @Embeddable
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class TeleportDestinationId implements Serializable {
    @Column(name = "teleport_room_id")
    private Integer teleportRoomId;

    @Column(name = "destination_room_id")
    private Integer destinationRoomId;

    // 完整toString方法
    @Override
    public String toString() {
      return "(" + teleportRoomId + "->" + destinationRoomId + ")";
    }
  }
}
