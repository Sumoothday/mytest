package com.example.demo1111111.entity;

import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "room")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RoomEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  private Integer id;

  @Column(nullable = false, length = 50)
  @ToString.Include
  private String name;

  @Column(length = 255)
  private String description;

  // 出口方向映射到数据库字段 - 使用LAZY加载
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exit_east")
  @ToString.Exclude
  @JsonIgnoreProperties({"exitEast", "exitWest", "exitNorth", "exitSouth", "roomItems"})
  private RoomEntity exitEast;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exit_west")
  @ToString.Exclude
  @JsonIgnoreProperties({"exitEast", "exitWest", "exitNorth", "exitSouth", "roomItems"})
  private RoomEntity exitWest;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exit_north")
  @ToString.Exclude
  @JsonIgnoreProperties({"exitEast", "exitWest", "exitNorth", "exitSouth", "roomItems"})
  private RoomEntity exitNorth;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "exit_south")
  @ToString.Exclude
  @JsonIgnoreProperties({"exitEast", "exitWest", "exitNorth", "exitSouth", "roomItems"})
  private RoomEntity exitSouth;

  @Column(name = "is_teleport")
  private boolean isTeleportRoom;

  // ===== 传送目的地集合 =====
  @OneToMany(
      mappedBy = "teleportRoom",
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL,
      orphanRemoval = true)
  @JsonIgnore
  @ToString.Exclude
  private List<TeleportDestination> teleportDestinations = new ArrayList<>();

  // 物品关联 - 使用JsonManagedReference避免循环引用
  @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @JsonManagedReference
  private List<RoomItem> roomItems = new ArrayList<>();

  // ========================
  // 传送目的地方法（修复版）
  // ========================

  /** 获取传送目的地列表 */
  @JsonIgnore
  public List<RoomEntity> getTeleportDestinations() {
    if (teleportDestinations == null || teleportDestinations.isEmpty()) {
      return Collections.emptyList();
    }

    return teleportDestinations.stream()
        .map(TeleportDestination::getDestination)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  /** 设置传送目的地列表 */
  public void setTeleportDestinations(List<RoomEntity> destinations) {
    if (destinations == null) {
      this.teleportDestinations.clear();
      return;
    }

    // 先清除现有目的地
    this.teleportDestinations.clear();

    // 添加新目的地
    for (RoomEntity destination : destinations) {
      if (destination != null) {
        addTeleportDestination(destination);
      }
    }
  }

  /** 添加单个传送目的地 */
  public void addTeleportDestination(RoomEntity destination) {
    if (destination != null && destination.getId() != null) {
      // 防止重复添加
      boolean exists =
          teleportDestinations.stream()
              .anyMatch(td -> destination.getId().equals(td.getDestination().getId()));

      if (!exists) {
        TeleportDestination td = new TeleportDestination();
        td.setTeleportRoom(this);
        td.setDestination(destination);
        teleportDestinations.add(td);
      }
    }
  }

  /** 移除传送目的地 */
  public void removeTeleportDestination(RoomEntity destination) {
    if (destination != null) {
      teleportDestinations.removeIf(
          td ->
              td.getDestination() != null
                  && destination.getId().equals(td.getDestination().getId()));
    }
  }

  /** 获取随机传送目的地（安全版） */
  @JsonIgnore
  public RoomEntity getRandomDestination() {
    List<RoomEntity> destinations = getTeleportDestinations();
    if (destinations.isEmpty()) {
      return null;
    }

    return destinations.get(new Random().nextInt(destinations.size()));
  }

  // ========================
  // 业务逻辑方法（优化版）
  // ========================

  /** 安全访问出口房间（带空值检查） */
  @JsonIgnore
  public Map<String, RoomEntity> getSafeExits() {
    Map<String, RoomEntity> exits = new LinkedHashMap<>();
    if (getExitEast() != null) exits.put("east", getExitEast());
    if (getExitWest() != null) exits.put("west", getExitWest());
    if (getExitNorth() != null) exits.put("north", getExitNorth());
    if (getExitSouth() != null) exits.put("south", getExitSouth());
    return exits;
  }

  /** 获取所有有效的出口方向（优化版） */
  @JsonIgnore
  public List<String> getValidExitDirections() {
    return new ArrayList<>(getSafeExits().keySet());
  }

  /** 根据方向获取出口（增强版） */
  public RoomEntity getExitForDirection(String direction) {
    if (direction == null) return null;

    return getSafeExits().get(direction.toLowerCase());
  }

  /** 获取房间内物品（重写为只读视图） */
  @JsonIgnore
  public Map<String, Item> getRoomItemsMap() {
    Map<String, Item> itemsMap = new LinkedHashMap<>();
    for (RoomItem roomItem : roomItems) {
      if (roomItem.getItem() != null) {
        itemsMap.put(roomItem.getItem().getName(), roomItem.getItem());
      }
    }
    return Collections.unmodifiableMap(itemsMap);
  }

  /** 从房间中移除物品（增强版） */
  public Item removeItem(String itemName) {
    if (itemName == null || itemName.isEmpty()) return null;

    for (Iterator<RoomItem> iterator = roomItems.iterator(); iterator.hasNext(); ) {
      RoomItem roomItem = iterator.next();
      if (roomItem.getItem() != null && itemName.equalsIgnoreCase(roomItem.getItem().getName())) {
        iterator.remove();
        roomItem.setRoom(null);
        return roomItem.getItem();
      }
    }
    return null;
  }

  /** 添加物品到房间（新方法） */
  public void addItem(Item item, int quantity) {
    if (item == null) return;

    // 检查物品是否已存在
    for (RoomItem roomItem : roomItems) {
      if (roomItem.getItem().getId().equals(item.getId())) {
        roomItem.setQuantity(roomItem.getQuantity() + quantity);
        return;
      }
    }

    // 创建新关联
    RoomItem newRoomItem = new RoomItem();
    newRoomItem.setRoom(this);
    newRoomItem.setItem(item);
    newRoomItem.setQuantity(quantity);
    roomItems.add(newRoomItem);
  }

  /** 获取房间描述信息（优化版） */
  @JsonIgnore
  public String getFullDescription() {
    StringBuilder sb = new StringBuilder();
    sb.append("你位于: ").append(description != null ? description : "").append("\n");

    List<String> exits = getValidExitDirections();
    if (!exits.isEmpty()) {
      sb.append("出口方向: ").append(String.join(", ", exits)).append("\n");
    }

    if (!roomItems.isEmpty()) {
      sb.append("房间物品:\n");
      for (RoomItem roomItem : roomItems) {
        if (roomItem.getItem() != null) {
          sb.append("- ")
              .append(roomItem.getItem().getName())
              .append(" (数量: ")
              .append(roomItem.getQuantity())
              .append(", 重量: ")
              .append(roomItem.getItem().getWeight())
              .append(" kg)\n");
        }
      }
    }

    if (isTeleportRoom) {
      List<RoomEntity> destinations = getTeleportDestinations();
      sb.append("这里有神秘的传送能量! 可用目的地: ")
          .append(
              destinations.stream()
                  .filter(Objects::nonNull)
                  .map(RoomEntity::getName)
                  .filter(name -> name != null && !name.isEmpty())
                  .collect(Collectors.joining(", ", "[", "]")))
          .append("\n");
    }

    return sb.toString();
  }

  /** 计算房间物品总重量（优化版） */
  @JsonIgnore
  public double calculateTotalItemWeight() {
    return roomItems.stream()
        .filter(roomItem -> roomItem.getItem() != null)
        .mapToDouble(roomItem -> roomItem.getItem().getWeight() * roomItem.getQuantity())
        .sum();
  }

  // ========================
  // JPA 生命周期方法
  // ========================

  @PrePersist
  @PreUpdate
  private void prePersist() {
    // 确保子实体引用正确
    for (TeleportDestination td : teleportDestinations) {
      if (td.getTeleportRoom() == null) {
        td.setTeleportRoom(this);
      }
    }

    for (RoomItem ri : roomItems) {
      if (ri.getRoom() == null) {
        ri.setRoom(this);
      }
    }
  }
}
