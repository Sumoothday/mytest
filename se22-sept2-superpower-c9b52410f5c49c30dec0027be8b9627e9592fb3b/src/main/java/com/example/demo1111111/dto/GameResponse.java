package com.example.demo1111111.dto;

import java.util.*;
import java.util.stream.Collectors;

import com.example.demo1111111.entity.Item;
import com.example.demo1111111.entity.PlayerInventory;
import com.example.demo1111111.entity.RoomEntity;
import com.example.demo1111111.entity.User;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Builder
public class GameResponse {
  // 基础字段
  private String sessionId;
  private String message;
  private boolean gameOver;
  private boolean success;
  private int status;
  private Double currentWeight; // 玩家当前负重
  private Double maxWeight; // 玩家最大负重能力

  public boolean isSuccess() {
    return success;
  }

  // 用户信息字段（新增）
  private UserInfo userInfo;

  // 房间详情（二选一）
  private RoomDetail currentRoom; // 完整版（用于 go、back 等命令）
  private LookRoomDetail lookRoom; // 精简版（用于 look 命令）

  // 玩家库存
  private List<ItemDetail> inventory;

  // 新增字段用于 items 命令
  private List<LookItemDetail> roomItems; // 房间物品列表
  private Double roomTotalWeight; // 房间物品总重量
  private List<LookItemDetail> playerItems; // 玩家物品列表
  private Double playerTotalWeight; // 玩家物品总重量

  // ==================================================
  // 用户信息内部类（新增）
  // ==================================================
  // 添加 UserInfo 内部类
  @Getter
  @Setter
  @Builder
  public static class UserInfo {
    private Long id;
    private String username;
    private Integer currentRoomId;

    // 添加 fromUser 静态工厂方法
    public static UserInfo fromUser(User user) {
      return UserInfo.builder()
          .id(user.getId())
          .username(user.getUsername())
          .currentRoomId(user.getCurrentRoomId())
          .build();
    }
  }

  // ==================================================
  // 新增登录相关工厂方法
  // ==================================================

  /** 登录成功响应（包含用户信息） */
  /** 登录成功响应（包含用户信息），直接传入 PlayerInventory 列表，以保证 quantity 正确 */
  public static GameResponse loginSuccess(
      String sessionId,
      String message,
      RoomEntity room,
      List<PlayerInventory> inventoryEntries,
      User user,
      Double currentWeight,
      Double maxWeight) {
    // 把 PlayerInventory 转成 ItemDetail，并用真实 quantity
    List<ItemDetail> details =
        inventoryEntries.stream()
            .map(
                pi -> {
                  Item item = pi.getItem();
                  return ItemDetail.builder()
                      .name(item.getName())
                      .description(item.getDescription())
                      .weight(item.getWeight())
                      .quantity(pi.getQuantity()) // ★ 真实数量
                      .build();
                })
            .collect(Collectors.toList());

    return GameResponse.builder()
        .currentWeight(currentWeight)
        .maxWeight(maxWeight)
        .success(true)
        .message(message)
        .sessionId(sessionId)
        .gameOver(false)
        .currentRoom(RoomDetail.fromRoom(room))
        .inventory(details) // ★ 用真实数量的列表
        .userInfo(UserInfo.fromUser(user))
        .build();
  }

  /** 游戏恢复响应（包含用户信息） */
  /** 恢复会话成功响应（包含用户信息），从 PlayerInventory 列表构造 ItemDetail */
  public static GameResponse restoreSuccess(
      String sessionId,
      String message,
      RoomEntity room,
      List<PlayerInventory> inventoryEntries,
      User user) {
    List<ItemDetail> details =
        inventoryEntries.stream()
            .map(
                pi -> {
                  Item item = pi.getItem();
                  return ItemDetail.builder()
                      .name(item.getName())
                      .description(item.getDescription())
                      .weight(item.getWeight())
                      .quantity(pi.getQuantity()) // ★ 用真实数量
                      .build();
                })
            .collect(Collectors.toList());

    return GameResponse.builder()
        .success(true)
        .message(message)
        .sessionId(sessionId)
        .gameOver(false)
        .currentRoom(RoomDetail.fromRoom(room))
        .inventory(details) // ★ 用真实数量的列表
        .userInfo(UserInfo.fromUser(user))
        .build();
  }

  // ==================================================
  // 静态工厂方法（按需选择）
  // ==================================================

  /** 完整版响应（移动命令、返回命令等） */
  public static GameResponse classicSuccess(
      String sessionId, RoomEntity room, List<Item> inventory, String message, UserInfo userInfo) {
    return GameResponse.builder()
        .sessionId(sessionId)
        .success(true)
        .message(message)
        .currentRoom(RoomDetail.fromRoom(room))
        .inventory(convertInventory(inventory))
        .gameOver(false)
        .build();
  }

  /** 精简版响应（查看命令） */
  public static GameResponse lookSuccess(
      String sessionId, RoomEntity room, List<Item> inventory, String message, UserInfo userInfo) {
    return GameResponse.builder()
        .sessionId(sessionId)
        .success(true)
        .message(message)
        .lookRoom(LookRoomDetail.fromRoom(room))
        .inventory(convertInventory(inventory))
        .gameOver(false)
        .build();
  }

  /** 错误响应 */
  public static GameResponse failure(String message) {
    return GameResponse.builder()
        .success(false)
        .message(message)
        .gameOver(false)
        .currentRoom(null)
        .lookRoom(null)
        .inventory(Collections.emptyList())
        .build();
  }

  public static GameResponse inventoryUpdate(
      String sessionId,
      RoomEntity room,
      List<PlayerInventory> inventoryEntries,
      String message,
      double currentWeight,
      double maxWeight) {
    List<ItemDetail> details =
        inventoryEntries.stream()
            .map(
                pi -> {
                  Item item = pi.getItem();
                  return ItemDetail.builder()
                      .name(item.getName())
                      .description(item.getDescription())
                      .weight(item.getWeight())
                      .quantity(pi.getQuantity()) // 用真正的数量
                      .build();
                })
            .collect(Collectors.toList());

    return GameResponse.builder()
        .success(true)
        .message(message)
        .sessionId(sessionId)
        .currentRoom(RoomDetail.fromRoom(room))
        .inventory(details)
        .currentWeight(currentWeight)
        .maxWeight(maxWeight)
        .gameOver(false)
        .build();
  }

  // 新增辅助方法：转换房间物品
  private static List<LookItemDetail> convertRoomItems(RoomEntity room) {
    if (room == null) return Collections.emptyList();

    return room.getRoomItems().stream()
        .map(
            ri -> {
              LookItemDetail detail = LookItemDetail.fromItem(ri.getItem());
              int quantity = (ri.getQuantity() != null) ? ri.getQuantity() : 1;
              detail.setQuantity(quantity);
              return detail;
            })
        .collect(Collectors.toList());
  }

  // 新增辅助方法：转换玩家物品
  private static List<LookItemDetail> convertPlayerItems(List<Item> inventory) {
    if (inventory == null) return Collections.emptyList();

    return inventory.stream()
        .map(
            item -> {
              LookItemDetail detail = LookItemDetail.fromItem(item);
              detail.setQuantity(1); // 玩家物品默认数量为1
              return detail;
            })
        .collect(Collectors.toList());
  }

  /** 物品清单响应 */
  /** 物品清单响应（接收 PlayerInventory 列表，以保留真实 quantity） */
  public static GameResponse itemsResponse(
      String sessionId,
      String message,
      RoomEntity room,
      List<PlayerInventory> inventoryEntries,
      double roomTotalWeight,
      double playerTotalWeight) {
    // 把房间中的 RoomItem 转成 LookItemDetail（房间物品里已经有 quantity）
    List<LookItemDetail> roomDetails = convertRoomItems(room);

    // 把玩家库存里的 PlayerInventory 转成 LookItemDetail，保留 pi.getQuantity()
    List<LookItemDetail> playerDetails =
        inventoryEntries.stream()
            .map(
                pi -> {
                  Item item = pi.getItem();
                  return LookItemDetail.builder()
                      .name(item.getName())
                      .weight(item.getWeight())
                      .quantity(pi.getQuantity()) // ★ 用真实的 quantity
                      .build();
                })
            .collect(Collectors.toList());

    return GameResponse.builder()
        .sessionId(sessionId)
        .success(true)
        .message(message)
        .roomItems(roomDetails)
        .roomTotalWeight(roomTotalWeight)
        .playerItems(playerDetails) // ★ 用真实 quantity 的列表
        .playerTotalWeight(playerTotalWeight)
        .gameOver(false)
        .build();
  }

  // ==================================================
  // 数据转换方法
  // ==================================================
  public static List<ItemDetail> convertInventory(List<Item> inventory) {
    if (inventory == null) return Collections.emptyList();

    return inventory.stream()
        .map(
            item -> {
              ItemDetail detail = ItemDetail.fromItem(item);
              detail.setQuantity(1); // 玩家物品默认数量为1
              return detail;
            })
        .collect(Collectors.toList());
  }

  // ==================================================
  // 内部类定义
  // ==================================================

  /** 完整版房间详情（包含房间描述、出口名称映射等） */
  @Data
  @Builder
  public static class RoomDetail {
    private String description;
    private Map<String, String> exits; // 方向 -> 房间名称
    private List<ItemDetail> items;
    private String teleportHint; // 可选：传送门提示信息

    public static RoomDetail fromRoom(RoomEntity room) {
      if (room == null) return null;

      Map<String, String> exits = new LinkedHashMap<>();
      // 使用基础getter方法
      if (room.getExitEast() != null) exits.put("east", room.getExitEast().getName());
      if (room.getExitWest() != null) exits.put("west", room.getExitWest().getName());
      if (room.getExitNorth() != null) exits.put("north", room.getExitNorth().getName());
      if (room.getExitSouth() != null) exits.put("south", room.getExitSouth().getName());

      List<ItemDetail> items =
          Optional.ofNullable(room.getRoomItems()).orElse(Collections.emptyList()).stream()
              .map(
                  ri -> {
                    ItemDetail detail = ItemDetail.fromItem(ri.getItem());
                    int quantity = (ri.getQuantity() != null) ? ri.getQuantity() : 1;
                    detail.setQuantity(quantity);
                    return detail;
                  })
              .collect(Collectors.toList());

      String teleportHint = "";
      if (room.isTeleportRoom()) {
        List<RoomEntity> destinations = room.getTeleportDestinations();
        teleportHint =
            destinations.isEmpty()
                ? "这里有神秘的传送能量"
                : "传送门可通往: "
                    + destinations.stream()
                        .map(RoomEntity::getName)
                        .collect(Collectors.joining(", "));
      }

      return RoomDetail.builder()
          .description(room.getDescription())
          .exits(exits)
          .items(items)
          .teleportHint(teleportHint) // 添加传送提示
          .build();
    }
  }

  /** 精简版房间详情（仅出口方向、物品重量） */
  @Data
  @Builder
  public static class LookRoomDetail {
    private List<String> exits; // 出口方向列表（如 ["east", "north"]）
    private List<LookItemDetail> items; // 物品名称 + 重量 + 数量
    private double totalWeight; // 物品总重量

    public static LookRoomDetail fromRoom(RoomEntity room) {
      if (room == null) return null;

      // 修复这里 - 使用 getValidExitDirections() 代替 getExitDirections()
      List<String> exits = room.getValidExitDirections();

      List<LookItemDetail> items =
          room.getRoomItems().stream()
              .map(
                  ri -> {
                    LookItemDetail detail = LookItemDetail.fromItem(ri.getItem());
                    int quantity = (ri.getQuantity() != null) ? ri.getQuantity() : 1;
                    detail.setQuantity(quantity);
                    return detail;
                  })
              .collect(Collectors.toList());

      double totalWeight =
          items.stream().mapToDouble(item -> item.getWeight() * item.getQuantity()).sum();

      return LookRoomDetail.builder().exits(exits).items(items).totalWeight(totalWeight).build();
    }
  }

  /** 精简版物品详情（名称、重量和数量） */
  @Data
  @Builder
  public static class LookItemDetail {
    private String name;
    private double weight;
    private int quantity; // 数量

    public static LookItemDetail fromItem(Item item) {
      if (item == null) return null;

      return LookItemDetail.builder()
          .name(item.getName())
          .weight(item.getWeight())
          .quantity(1) // 默认数量为1
          .build();
    }
  }

  /** 完整版物品详情（名称、描述、重量） */
  @Data
  @Builder
  public static class ItemDetail {
    private String name;
    private String description;
    private double weight;
    private int quantity; // 新增字段：物品数量

    public static ItemDetail fromItem(Item item) {
      if (item == null) return null;

      return ItemDetail.builder()
          .name(item.getName())
          .description(item.getDescription())
          .weight(item.getWeight())
          .quantity(1) // 默认数量为1
          .build();
    }
  }

  // 在 GameResponse 类中
  public static GameResponse success(
      String sessionId, String message, RoomEntity currentRoom, List<Item> inventory) {
    return GameResponse.builder()
        .sessionId(sessionId)
        .success(true)
        .message(message)
        .currentRoom(RoomDetail.fromRoom(currentRoom))
        .inventory(convertInventory(inventory))
        .gameOver(false)
        .build();
  }

  // 添加用户信息的重载方法
  public static GameResponse success(
      String sessionId,
      String message,
      RoomEntity currentRoom,
      List<Item> inventory,
      UserInfo userInfo) {
    return GameResponse.builder()
        .sessionId(sessionId)
        .success(true)
        .message(message)
        .userInfo(userInfo)
        .currentRoom(RoomDetail.fromRoom(currentRoom))
        .inventory(convertInventory(inventory))
        .gameOver(false)
        .build();
  }

  public static UserInfo convertUser(User user) {
    if (user == null) return null;
    return UserInfo.builder()
        .id(user.getId())
        .username(user.getUsername())
        .currentRoomId(user.getCurrentRoomId())
        .build();
  }

  public static GameResponse success(
      String sessionId,
      String message,
      RoomEntity currentRoom,
      List<Item> inventory,
      UserInfo userInfo,
      double currentWeight,
      double maxWeight) {
    return builder()
        .sessionId(sessionId)
        .message(message)
        .currentRoom(RoomDetail.fromRoom(currentRoom))
        .inventory(convertInventory(inventory))
        .userInfo(userInfo)
        .currentWeight(currentWeight)
        .maxWeight(maxWeight)
        .build();
  }
}
