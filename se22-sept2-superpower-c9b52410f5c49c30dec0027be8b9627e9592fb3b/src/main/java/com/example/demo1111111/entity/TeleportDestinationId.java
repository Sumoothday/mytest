package com.example.demo1111111.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;

// 复合主键类
@Embeddable
@Data // Lombok 自动生成 equals 和 hashCode
public class TeleportDestinationId implements Serializable {
  @Column(name = "teleport_room_id")
  private Integer teleportRoomId;

  @Column(name = "destination_room_id")
  private Integer destinationRoomId;
}
