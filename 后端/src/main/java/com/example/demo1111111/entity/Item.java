// entity/Item.java
package com.example.demo1111111.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Item {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private String name;

  @Column(columnDefinition = "TEXT") // 明确指定长文本类型
  private String description;

  @Column(nullable = false)
  private double weight;

  @Column(name = "edible", columnDefinition = "TINYINT(1) default 0", nullable = false)
  private Boolean edible = false; // 使用包装类而不是基本类型

  public Boolean isEdible() { // 改名为 isEdible 更符合布尔属性的命名约定
    return edible;
  }

  public void setEdible(Boolean edible) {
    this.edible = edible;
  }
}
