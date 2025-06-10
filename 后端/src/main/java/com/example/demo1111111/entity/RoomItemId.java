package com.example.demo1111111.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RoomItemId implements Serializable {
  @Column(name = "room_id")
  private Integer roomId; // ✅ 数据库字段名对应的基本类型

  @Column(name = "item_id")
  private Integer itemId; // ✅ 非实体对象
}
