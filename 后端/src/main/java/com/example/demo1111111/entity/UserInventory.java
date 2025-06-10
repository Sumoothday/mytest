package com.example.demo1111111.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_inventory")
@Data
public class UserInventory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne
  @JoinColumn(name = "item_id")
  private GameItem item;

  private Integer quantity = 1;
}
