package com.example.demo1111111.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "player_inventory")
@Data
public class PlayerInventory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id") // 从session_id改为user_id
  private Long userId;

  @ManyToOne
  @JoinColumn(name = "item_id")
  private Item item;

  private int quantity;

  // Getters and setters
}
