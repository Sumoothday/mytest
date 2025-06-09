package com.example.demo1111111.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "game_items")
@Data
public class GameItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private String name;
  private String description;
  private double weight;

  @Column(name = "base_room")
  private String baseRoom;
}
