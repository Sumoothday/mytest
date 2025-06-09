package com.example.demo1111111.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.demo1111111.entity.Item;
import com.example.demo1111111.entity.RoomEntity;

import lombok.Data;

@Data
public class GameState implements Serializable {
  private static final Logger logger = LoggerFactory.getLogger(GameState.class);

  private String sessionId;
  private RoomEntity currentRoom;
  private boolean gameOver = false;
  private List<Item> inventory = new ArrayList<>();

  // 物品名称到ID的映射
  private final ConcurrentMap<String, Integer> itemNameToIdMap = new ConcurrentHashMap<>();

  // 完整物品缓存
  private final ConcurrentMap<Integer, Item> itemCache = new ConcurrentHashMap<>();

  // 最后房间ID（用于状态恢复）
  private Integer lastRoomId;

  // 房间历史栈
  private Stack<RoomEntity> roomHistory = new Stack<>();

  // 全参数构造方法
  public GameState(
      String sessionId, RoomEntity currentRoom, List<Item> inventory, boolean gameOver) {
    this.sessionId = sessionId;
    this.currentRoom = currentRoom;
    this.lastRoomId = currentRoom != null ? currentRoom.getId() : null;
    this.inventory = inventory != null ? new ArrayList<>(inventory) : new ArrayList<>();
    this.gameOver = gameOver;

    // 初始化物品映射和缓存
    rebuildItemMappings();

    logger.debug(
        "创建GameState: sessionId={}, room={}",
        sessionId,
        currentRoom != null ? currentRoom.getName() : "null");
  }

  // 简化构造方法
  public GameState(String sessionId, RoomEntity currentRoom) {
    this(sessionId, currentRoom, null, false);
  }

  // ======================
  // 房间移动方法
  // ======================

  /**
   * 移动到新房间（记录历史）
   *
   * @param newRoom 要移动到的房间
   */
  public void moveToNewRoom(RoomEntity newRoom) {
    if (newRoom == null) {
      logger.warn("试图移动到null房间");
      return;
    }

    // 记录当前房间到历史（如果有）
    if (currentRoom != null) {
      RoomEntity currentSnapshot = createRoomSnapshot(currentRoom);
      // roomHistory.push(currentSnapshot);
      logger.debug("记录历史房间: {} -> {}", currentSnapshot.getName(), getHistoryStackAsString());
    }

    // 设置新房间
    this.currentRoom = newRoom;
    this.lastRoomId = newRoom.getId();
    logger.debug("移动到新房间: {}", newRoom.getName());
  }

  /**
   * 返回到历史房间（不记录历史）
   *
   * @param room 要返回到的历史房间
   */
  public void moveBackToRoom(RoomEntity room) {
    if (room == null) {
      logger.warn("试图返回到null房间");
      return;
    }

    this.currentRoom = room;
    this.lastRoomId = room.getId();
    logger.debug("返回到历史房间: {}", room.getName());
  }

  /**
   * 设置当前房间（不记录历史）
   *
   * @param room 要设置的房间
   */
  public void setCurrentRoomWithoutHistory(RoomEntity room) {
    if (room == null) {
      logger.warn("试图设置null房间");
      return;
    }

    this.currentRoom = room;
    this.lastRoomId = room.getId();
    logger.debug("设置当前房间: {} 不记录历史", room.getName());
  }

  // ======================
  // 历史管理方法
  // ======================

  /**
   * 创建房间的快照副本
   *
   * @param room 要创建快照的房间
   * @return 房间快照
   */
  private RoomEntity createRoomSnapshot(RoomEntity room) {
    if (room == null) return null;

    RoomEntity snapshot = new RoomEntity();
    snapshot.setId(room.getId());
    snapshot.setName(room.getName());
    snapshot.setDescription(room.getDescription());

    return snapshot;
  }

  /**
   * 获取历史栈状态字符串
   *
   * @return 历史栈字符串表示
   */
  public String getHistoryStackAsString() {
    if (roomHistory.isEmpty()) return "空";

    List<String> names = new ArrayList<>();
    for (int i = 0; i < roomHistory.size(); i++) {
      names.add(roomHistory.get(i).getName());
    }
    return "[" + String.join(", ", names) + "]";
  }

  /**
   * 从历史栈中弹出上一个房间
   *
   * @return 上一个房间
   */
  public RoomEntity popRoomFromHistory() {
    if (!roomHistory.isEmpty()) {
      RoomEntity room = roomHistory.pop();
      logger.debug("弹出历史房间: {}", room.getName());
      return room;
    }
    logger.debug("历史栈已空");
    return null;
  }

  /**
   * 添加房间到历史栈
   *
   * @param room 要添加到历史的房间
   */
  public void pushRoomToHistory(RoomEntity room) {
    if (room != null) {
      RoomEntity snapshot = createRoomSnapshot(room);
      roomHistory.push(snapshot);
      logger.debug("添加到历史栈: {}", room.getName());
    }
  }

  /**
   * 初始化房间关联
   *
   * @param room 要初始化的房间
   */
  public void initializeRoomAssociations(RoomEntity room) {
    if (room == null) return;

    try {
      // 初始化方向关联
      if (room.getExitEast() != null) Hibernate.initialize(room.getExitEast());
      if (room.getExitWest() != null) Hibernate.initialize(room.getExitWest());
      if (room.getExitNorth() != null) Hibernate.initialize(room.getExitNorth());
      if (room.getExitSouth() != null) Hibernate.initialize(room.getExitSouth());

      // 初始化房间物品
      if (room.getRoomItems() != null) {
        for (var roomItem : room.getRoomItems()) {
          if (roomItem.getItem() != null) {
            Hibernate.initialize(roomItem.getItem());
          }
        }
      }
    } catch (Exception e) {
      logger.error("初始化房间关联失败", e);
    }
  }

  /**
   * 检查是否有房间历史
   *
   * @return 是否有历史记录
   */
  public boolean hasRoomHistory() {
    return !roomHistory.isEmpty();
  }

  /**
   * 获取房间历史栈大小
   *
   * @return 历史栈大小
   */
  public int getRoomHistorySize() {
    return roomHistory.size();
  }

  // ======================
  // 物品管理方法
  // ======================

  /** 重建物品映射关系 */
  public void rebuildItemMappings() {
    itemNameToIdMap.clear();
    itemCache.clear();

    if (inventory != null) {
      for (Item item : inventory) {
        if (item.getName() != null) {
          itemNameToIdMap.put(item.getName().toLowerCase(), item.getId());
          itemCache.put(item.getId(), item);
        }
      }
    }

    logger.debug("重建物品映射: {} 件物品", inventory.size());
  }

  /**
   * 在库存中查找物品
   *
   * @param itemName 物品名称
   * @return 找到的物品
   */
  public Optional<Item> findItemInInventory(String itemName) {
    if (itemName == null || itemName.trim().isEmpty()) {
      return Optional.empty();
    }

    String normalized = itemName.toLowerCase().trim();

    // 首先检查名称->ID映射
    Integer itemId = itemNameToIdMap.get(normalized);
    if (itemId != null) {
      return Optional.ofNullable(itemCache.get(itemId));
    }

    // 尝试直接匹配
    return inventory.stream()
        .filter(item -> item.getName() != null)
        .filter(item -> item.getName().equalsIgnoreCase(normalized))
        .findFirst();
  }

  /**
   * 通过ID查找物品
   *
   * @param itemId 物品ID
   * @return 找到的物品
   */
  public Optional<Item> findItemById(Integer itemId) {
    return Optional.ofNullable(itemCache.get(itemId));
  }

  /**
   * 添加物品到库存
   *
   * @param item 要添加的物品
   */
  public void addToInventory(Item item) {
    if (item == null) return;

    // 确保物品不在库存中
    if (findItemById(item.getId()).isPresent()) {
      return;
    }

    inventory.add(item);

    // 更新映射和缓存
    if (item.getName() != null) {
      itemNameToIdMap.put(item.getName().toLowerCase(), item.getId());
    }
    itemCache.put(item.getId(), item);

    logger.debug("添加到库存: {}", item.getName());
  }

  /**
   * 从库存中移除物品
   *
   * @param item 要移除的物品
   */
  public void removeFromInventory(Item item) {
    if (item == null) return;

    inventory.removeIf(i -> i.getId().equals(item.getId()));

    // 更新映射和缓存
    if (item.getName() != null) {
      itemNameToIdMap.remove(item.getName().toLowerCase());
    }
    itemCache.remove(item.getId());

    logger.debug("从库存移除: {}", item.getName());
  }

  /**
   * 通过ID从库存中移除物品
   *
   * @param itemId 物品ID
   */
  public void removeFromInventoryById(Integer itemId) {
    Optional<Item> item = findItemById(itemId);
    item.ifPresent(this::removeFromInventory);
  }

  // ======================
  // Getter 和 Setter 方法
  // ======================

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public RoomEntity getCurrentRoom() {
    return currentRoom;
  }

  public void setCurrentRoom(RoomEntity currentRoom) {
    this.currentRoom = currentRoom;
    this.lastRoomId = currentRoom != null ? currentRoom.getId() : null;
  }

  public boolean isGameOver() {
    return gameOver;
  }

  public void setGameOver(boolean gameOver) {
    this.gameOver = gameOver;
  }

  public List<Item> getInventory() {
    return inventory;
  }

  public void setInventory(List<Item> inventory) {
    this.inventory = inventory != null ? new ArrayList<>(inventory) : new ArrayList<>();
    rebuildItemMappings();

    logger.debug("更新库存: {} 件物品", this.inventory.size());
  }

  public Integer getLastRoomId() {
    return lastRoomId;
  }

  public void setLastRoomId(Integer lastRoomId) {
    this.lastRoomId = lastRoomId;
  }
}
