package com.example.demo1111111.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.example.demo1111111.config.GameState;
import com.example.demo1111111.dto.GameResponse;
import com.example.demo1111111.entity.*;
import com.example.demo1111111.repository.*;

import jakarta.annotation.PostConstruct;

@Service
public class GameService {
  // 数据库仓库
  private final RoomRepository roomRepo;
  private final ItemRepository itemRepo;
  private final RoomItemRepository roomItemRepo;
  private final TeleportDestinationRepository teleportRepo;
  private final PlayerInventoryRepository playerInventoryRepo;
  private final UserRepository userRepo;
  private boolean magicCakePlaced = false;
  private static final String MAGIC_CAKE_NAME = "magic cake";
  private static final double WEIGHT_BOOST_AMOUNT = 10.0; // 提升10kg负重能力

  // 会话存储 (改为 userId -> GameState)
  private final Map<Long, GameState> sessionStore = new ConcurrentHashMap<>();
  private static final Logger logger = LoggerFactory.getLogger(GameService.class);

  // 会话超时时间 (30分钟)
  private static final long SESSION_TIMEOUT = 30 * 60 * 1000;

  @Autowired
  public GameService(
      RoomRepository roomRepo,
      ItemRepository itemRepo,
      RoomItemRepository roomItemRepo,
      TeleportDestinationRepository teleportRepo,
      PlayerInventoryRepository playerInventoryRepo,
      UserRepository userRepo) {
    this.roomRepo = roomRepo;
    this.itemRepo = itemRepo;
    this.roomItemRepo = roomItemRepo;
    this.teleportRepo = teleportRepo;
    this.playerInventoryRepo = playerInventoryRepo;
    this.userRepo = userRepo;
  }

  // ==================== 用户登录和会话管理 ====================
  @Transactional
  public GameResponse loginUser(String username, String password) {
    // 1. 验证用户凭据
    User user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("用户不存在"));
    if (!password.equals(user.getPassword())) {
      return GameResponse.failure("密码错误");
    }

    // 2. 生成并保存新的 sessionId
    String sessionId = UUID.randomUUID().toString();
    user.setSessionId(sessionId);
    user.setLastLogin(new Date());
    userRepo.save(user);

    // 3. 加载用户当前房间
    RoomEntity currentRoom = loadUserRoom(user);

    // 4. 加载用户库存：改为拿 List<PlayerInventory>，以获取 quantity
    List<PlayerInventory> inventoryEntries = loadPlayerInventory(user.getId());

    // 5. 从 PlayerInventory 中抽取纯 Item 列表，供 GameState 使用
    List<Item> inventoryForState =
        inventoryEntries.stream().map(PlayerInventory::getItem).collect(Collectors.toList());

    // 6. 创建游戏状态（GameState 中只存 Item 列表，不存 quantity）
    GameState state = new GameState(sessionId, currentRoom, inventoryForState, false);
    sessionStore.put(user.getId(), state);

    // 7. 计算当前负重
    double currentWeight = calculateCurrentWeight(inventoryForState);
    double maxWeight = user.getMaxCarryWeight();

    // 8. 返回给前端：把 List<PlayerInventory> 传给 GameResponse.loginSuccess，
    //    以便在 DTO 中用真实的 quantity 填充
    return GameResponse.loginSuccess(
        sessionId,
        "欢迎回来, " + username + "!",
        currentRoom,
        inventoryEntries, // 改成传 List<PlayerInventory>
        user,
        currentWeight,
        maxWeight);
  }

  // 计算当前总负重的方法
  private double calculateCurrentWeight(List<Item> inventory) {
    return inventory.stream().mapToDouble(Item::getWeight).sum();
  }

  // 恢复用户游戏会话
  @Transactional
  public GameResponse restoreSession(String sessionId) {
    // 1. 查找关联的用户
    User user =
        userRepo.findBySessionId(sessionId).orElseThrow(() -> new RuntimeException("会话不存在或已过期"));

    // 2. 检查会话是否超时
    if (isSessionExpired(user.getLastLogin())) {
      sessionStore.remove(user.getId());
      return GameResponse.failure("会话已过期，请重新登录");
    }

    // 3. 更新最后登录时间
    user.setLastLogin(new Date());
    userRepo.save(user);

    // 4. 加载或创建游戏状态：这里创建 GameState 时依然只给它 List<Item>
    GameState state =
        sessionStore.computeIfAbsent(
            user.getId(),
            key -> {
              RoomEntity currentRoom = loadUserRoom(user);
              // 依旧把 PlayerInventory 转为 Item 列表
              List<PlayerInventory> invEntries = loadPlayerInventory(user.getId());
              List<Item> invForState =
                  invEntries.stream().map(PlayerInventory::getItem).collect(Collectors.toList());
              return new GameState(sessionId, currentRoom, invForState, false);
            });

    // 5. 加载完整的 PlayerInventory 列表，用于填充真实数量
    List<PlayerInventory> inventoryEntries = loadPlayerInventory(user.getId());

    // 6. 返回响应：把 List<PlayerInventory> 交给 GameResponse
    return GameResponse.restoreSuccess(
        sessionId,
        "已恢复游戏",
        state.getCurrentRoom(),
        inventoryEntries, // 传 List<PlayerInventory>
        user);
  }

  // ==================== 核心游戏逻辑 ====================
  // (原createNewGame方法不再使用，由登录方法替代)

  // ==================== 命令处理器 ====================

  // 通用命令前置验证
  private GameState validateUserSession(String sessionId) {
    // 1. 查找关联的用户
    User user = userRepo.findBySessionId(sessionId).orElseThrow(() -> new RuntimeException("无效会话"));

    // 2. 检查会话是否超时
    if (isSessionExpired(user.getLastLogin())) {
      sessionStore.remove(user.getId());
      throw new RuntimeException("会话已过期");
    }

    // 3. 更新最后登录时间
    user.setLastLogin(new Date());
    userRepo.save(user);

    // 4. 获取游戏状态
    GameState state = sessionStore.get(user.getId());
    if (state == null) {
      throw new RuntimeException("会话未初始化");
    }

    return state;
  }

  // GO 命令处理器 (修改为基于用户ID)
  @Transactional
  public GameResponse processGoCommand(String sessionId, String direction) {
    GameState state = validateUserSession(sessionId);
    if (direction == null || direction.isEmpty()) return GameResponse.failure("必须指定方向");

    // 获取用户信息
    User user = userRepo.findBySessionId(sessionId).orElse(null);
    if (user == null) return GameResponse.failure("用户不存在");

    // 获取当前房间并刷新数据
    RoomEntity currentRoom = refreshRoom(state.getCurrentRoom().getId());
    String normalizedDir = direction.toLowerCase().trim();

    // 验证出口是否存在
    RoomEntity nextRoom = currentRoom.getExitForDirection(normalizedDir);
    if (nextRoom == null) return GameResponse.failure("无法向 " + direction + " 移动");

    // 加载完整房间数据
    RoomEntity fullNextRoom = refreshRoom(nextRoom.getId());

    // 更新用户当前位置
    user.setCurrentRoomId(fullNextRoom.getId());
    userRepo.save(user);

    // 处理传送门房间逻辑
    if (fullNextRoom.isTeleportRoom()) {
      return handleTeleportation(state, currentRoom, fullNextRoom, user);
    }

    // 正常移动逻辑
    return handleRegularMove(state, currentRoom, fullNextRoom, user);
  }

  // LOOK 命令处理器
  @Transactional
  public GameResponse processLookCommand(String sessionId) {
    logger.debug("处理 look 命令: sessionId={}", sessionId);

    try {
      GameState state = validateUserSession(sessionId);
      User user = userRepo.findBySessionId(sessionId).orElse(null);

      // 获取并刷新当前房间数据
      RoomEntity currentRoom = refreshRoom(state.getCurrentRoom().getId());

      // 更新游戏状态中的房间引用
      state.setCurrentRoomWithoutHistory(currentRoom);

      // 构建响应
      return buildLookResponse(state, currentRoom, user);

    } catch (Exception e) {
      logger.error("查看房间失败: sessionId={}", sessionId, e);
      return GameResponse.failure("查看房间失败: " + e.getMessage());
    }
  }

  // BACK 命令处理器
  @Transactional
  public GameResponse processBackCommand(String sessionId) {
    GameState state = validateUserSession(sessionId);
    User user = userRepo.findBySessionId(sessionId).orElse(null);

    if (user == null) {
      return GameResponse.failure("用户不存在");
    }

    if (!state.hasRoomHistory()) {
      return GameResponse.failure("没有可返回的路径");
    }

    try {
      // 1. 从历史栈中弹出上一个房间（这是要返回的房间）
      RoomEntity prevRoom = state.popRoomFromHistory();

      // 2. 重新加载完整房间数据
      RoomEntity fullPrevRoom = refreshRoom(prevRoom.getId());

      // 3. 使用特殊方法移动到返回的房间（不记录历史）
      // >>> 这是关键修改点 <<<
      state.moveBackToRoom(fullPrevRoom);

      // 4. 更新用户位置
      user.setCurrentRoomId(fullPrevRoom.getId());
      userRepo.save(user);

      logger.debug("Back命令后，历史栈大小: {}", state.getRoomHistorySize());

      return GameResponse.success(
          sessionId,
          "返回成功：" + fullPrevRoom.getDescription(),
          fullPrevRoom,
          state.getInventory(),
          GameResponse.UserInfo.fromUser(user));
    } catch (Exception e) {
      logger.error("返回过程中发生错误", e);
      return GameResponse.failure("无法返回: " + e.getMessage());
    }
  }

  // TAKE 命令处理器 (改为基于用户ID)
  @Transactional(propagation = Propagation.REQUIRED)
  public GameResponse processTakeCommand(String sessionId, String itemName) {
    logger.debug("处理 take 命令: sessionId={}, itemName={}", sessionId, itemName);

    GameState state = validateUserSession(sessionId);
    User user = userRepo.findBySessionId(sessionId).orElse(null);

    try {
      // 获取当前房间
      RoomEntity currentRoom = state.getCurrentRoom();

      // 查找目标物品
      // 查找目标物品（使用新的匹配方法）
      Optional<RoomItem> targetRoomItemOpt =
          currentRoom.getRoomItems().stream()
              .filter(ri -> matchesItemName(ri.getItem(), itemName))
              .findFirst();

      if (targetRoomItemOpt.isEmpty()) {
        return GameResponse.failure("房间中没有 " + itemName);
      }

      RoomItem targetRoomItem = targetRoomItemOpt.get();
      Item item = targetRoomItem.getItem();

      // 检查是否超重
      double currentWeight = calculateCurrentWeight(state.getInventory());
      double itemWeight = item.getWeight();
      double maxWeight = user.getMaxCarryWeight();

      if (currentWeight + itemWeight > maxWeight) {
        return GameResponse.failure(
            "太重了！无法拾取 " + itemName + "\n当前负重: " + currentWeight + "kg/" + maxWeight + "kg");
      }

      // 从房间移除物品
      removeItemFromRoom(currentRoom, item);

      // 添加到玩家库存
      addItemToPlayerInventory(user.getId(), item);
      state.addToInventory(item);

      // 重新计算拾取后的负重
      double newWeight = currentWeight + itemWeight;
      List<PlayerInventory> inventoryEntries = playerInventoryRepo.findByUserId(user.getId());
      return GameResponse.inventoryUpdate(
          sessionId,
          currentRoom,
          inventoryEntries,
          "获得物品: " + item.getName(),
          newWeight,
          maxWeight);
    } catch (Exception e) {
      logger.error("取物品失败: sessionId={}, item={}", sessionId, itemName, e);

      if (TransactionAspectSupport.currentTransactionStatus() != null) {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      }

      return GameResponse.failure("操作失败: " + e.getMessage());
    }
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public GameResponse processDropCommand(String sessionId, String itemName) {
    logger.debug("处理 drop 命令: sessionId={}, itemName={}", sessionId, itemName);

    // (1) 验证会话并拿到 state、user
    GameState state = validateUserSession(sessionId);
    User user = userRepo.findBySessionId(sessionId).orElse(null);
    if (user == null) {
      return GameResponse.failure("用户不存在");
    }

    // (2) 在内存背包中查找该物品（只判断是否存在）
    Optional<Item> itemOpt =
        state.getInventory().stream().filter(it -> matchesItemName(it, itemName)).findFirst();
    if (!itemOpt.isPresent()) {
      return GameResponse.failure("您的背包中没有该物品: " + itemName);
    }
    Item item = itemOpt.get();

    try {
      // (3) 先在数据库中减少/删除该物品的 PlayerInventory 记录
      removeItemFromPlayerInventory(user.getId(), item);

      // (4) 检查数据库里这一条记录是否还存在
      boolean stillInDb =
          playerInventoryRepo
              .findByUserIdAndItemId(user.getId(), Long.valueOf(item.getId()))
              .isPresent();

      // (5) 如果 DB 里已没有该物品（quantity 最终从 1→0 被删除），才从 state.inventory 移除
      if (!stillInDb) {
        state.removeFromInventory(item);
      }

      // (6) 把物品放回房间
      RoomEntity currentRoom = refreshRoom(state.getCurrentRoom().getId());
      addItemToRoom(currentRoom, item);
      state.setCurrentRoomWithoutHistory(currentRoom);

      // (7) 重新加载数据库中这个用户的 PlayerInventory 列表，以获取最新 quantity
      List<PlayerInventory> inventoryEntries = loadPlayerInventory(user.getId());

      // (8) 计算玩家当前负重
      double newWeight =
          inventoryEntries.stream()
              .mapToDouble(pi -> pi.getItem().getWeight() * pi.getQuantity())
              .sum();
      double maxWeight = user.getMaxCarryWeight();

      // (9) 用接收 List<PlayerInventory> 的 inventoryUpdate(...) 返回给前端
      return GameResponse.inventoryUpdate(
          sessionId,
          currentRoom,
          inventoryEntries, // 传 List<PlayerInventory>
          "您丢弃了物品: " + itemName,
          newWeight,
          maxWeight);
    } catch (Exception e) {
      logger.error("丢弃物品失败", e);
      return GameResponse.failure("丢弃物品失败: " + e.getMessage());
    }
  }

  // ITEMS 命令处理器
  @Transactional
  public GameResponse processItemsCommand(String sessionId) {
    GameState state = validateUserSession(sessionId);
    User user = userRepo.findBySessionId(sessionId).orElse(null);
    if (user == null) {
      return GameResponse.failure("用户不存在");
    }

    // 1. 刷新并拿到当前房间
    RoomEntity currentRoom = refreshRoom(state.getCurrentRoom().getId());

    // 2. 计算房间中所有物品的总重量（保持不变）
    double roomTotalWeight =
        currentRoom.getRoomItems().stream()
            .mapToDouble(ri -> ri.getItem().getWeight() * ri.getQuantity())
            .sum();

    // 3. 从数据库拿到玩家库存及数量
    List<PlayerInventory> inventoryEntries = loadPlayerInventory(user.getId());

    // 4. 计算玩家物品总重量时，要把重量乘以每个条目的 quantity
    double playerTotalWeight =
        inventoryEntries.stream()
            .mapToDouble(pi -> pi.getItem().getWeight() * pi.getQuantity())
            .sum();

    // 5. 把这些数据一并交给新的 itemsResponse
    return GameResponse.itemsResponse(
        sessionId,
        "物品清单",
        currentRoom,
        inventoryEntries, // 传 List<PlayerInventory>
        roomTotalWeight,
        playerTotalWeight);
  }

  // ==================== 辅助方法 ====================

  // 加载用户房间
  private RoomEntity loadUserRoom(User user) {
    if (user.getCurrentRoomId() != null) {
      return roomRepo.findByIdWithFullData(user.getCurrentRoomId()).orElseGet(() -> fallbackRoom());
    }
    return fallbackRoom();
  }

  // 备用起始房间
  private RoomEntity fallbackRoom() {
    return roomRepo
        .findByNameWithFullData("outside")
        .orElseThrow(() -> new RuntimeException("起始房间未配置"));
  }

  // 加载玩家库存
  // 原版：只返回 Item 列表，丢掉了数量
  // GameService.java 中将原方法替换为：
  // 位置：GameService 类里所有 @Transactional 方法之后（或和原来位置对应），把签名和返回类型都改掉
  private List<PlayerInventory> loadPlayerInventory(Long userId) {
    // 直接调用 Repository 拿到 PlayerInventory 对象列表
    return playerInventoryRepo.findByUserId(userId);
  }

  // 检查会话是否过期
  private boolean isSessionExpired(Date lastLogin) {
    if (lastLogin == null) return true;
    long currentTime = System.currentTimeMillis();
    return (currentTime - lastLogin.getTime()) > SESSION_TIMEOUT;
  }

  // 刷新房间数据
  private RoomEntity refreshRoom(Integer roomId) {
    // 保持原有实现不变
    logger.debug("深度刷新房间数据: roomId={}", roomId);
    return roomRepo
        .findByIdWithFullData(roomId)
        .orElseThrow(() -> new RuntimeException("房间不存在: " + roomId));
  }

  // 传送门处理逻辑 (添加用户参数)
  private GameResponse handleTeleportation(
      GameState state, RoomEntity originRoom, RoomEntity teleportRoom, User user) {
    // 安全获取传送目的地
    RoomEntity destination = teleportRoom.getRandomDestination();

    // 检查目的地的有效性
    if (destination == null) {
      return GameResponse.failure("传送门没有配置目的地！");
    }

    // 加载目的地房间数据
    RoomEntity fullDestination = refreshRoom(destination.getId());

    // 更新状态（不记录传送门房间到历史）
    state.pushRoomToHistory(originRoom); // 记录传送前的原始房间
    state.setCurrentRoomWithoutHistory(fullDestination); // 不触发自动历史记录

    // 更新用户位置
    user.setCurrentRoomId(fullDestination.getId());
    userRepo.save(user);

    return buildSuccessResponse(state, fullDestination, "嗡！你被传送到：", user);
  }

  // 普通移动处理逻辑 (添加用户参数)
  private GameResponse handleRegularMove(
      GameState state, RoomEntity currentRoom, RoomEntity nextRoom, User user) {
    // 记录历史并更新房间
    state.pushRoomToHistory(currentRoom);
    state.moveToNewRoom(nextRoom);

    // 更新用户位置
    user.setCurrentRoomId(nextRoom.getId());
    userRepo.save(user);

    return buildSuccessResponse(state, nextRoom, "你移动到了：", user);
  }

  // 构建成功响应 (添加用户参数)
  private GameResponse buildSuccessResponse(
      GameState state, RoomEntity room, String prefix, User user) {
    // ① 先重新从数据库里取这位用户的完整库存记录，拿到真正的 quantity
    List<PlayerInventory> inventoryEntries = loadPlayerInventory(user.getId());

    // ② 计算当前负重 (可选，如果你只想返回列表而不更新负重，也可以用一个专门不带重量的方法)
    double currentWeight =
        inventoryEntries.stream()
            .mapToDouble(pi -> pi.getItem().getWeight() * pi.getQuantity())
            .sum();
    double maxWeight = user.getMaxCarryWeight();

    // ③ 调用重载版 inventoryUpdate(...)，它会把 item + quantity 全部写入响应
    return GameResponse.inventoryUpdate(
        state.getSessionId(),
        room,
        inventoryEntries, // 传 List<PlayerInventory>，保留真实数量
        prefix + room.getDescription(),
        currentWeight,
        maxWeight);
  }

  // 构建 LOOK 响应 (添加用户参数)
  private GameResponse buildLookResponse(GameState state, RoomEntity room, User user) {
    // ① 重新从数据库里加载这位用户真实的背包记录
    List<PlayerInventory> inventoryEntries = loadPlayerInventory(user.getId());

    // ② 计算当前总负重，后面用来返回
    double currentWeight =
        inventoryEntries.stream()
            .mapToDouble(pi -> pi.getItem().getWeight() * pi.getQuantity())
            .sum();
    double maxWeight = user.getMaxCarryWeight();

    // ③ 用新的工厂方法专门返回：要把房间信息 + 背包（含真实 quantity）一起打包
    return GameResponse.inventoryUpdate(
        state.getSessionId(),
        room,
        inventoryEntries, // 真实 quantity
        buildRoomDescription(room), // 作为 message
        currentWeight,
        maxWeight);
  }

  private String buildRoomDescription(RoomEntity room) {
    // 构建房间描述
    StringBuilder sb = new StringBuilder();
    sb.append("=== ").append(room.getName()).append(" ===\n");
    sb.append(room.getDescription()).append("\n\n");

    // 添加出口信息
    List<String> exits = room.getValidExitDirections();
    if (!exits.isEmpty()) {
      sb.append("出口: ").append(String.join(", ", exits)).append("\n");
    }

    // 添加物品信息
    if (room.getRoomItems().isEmpty()) {
      sb.append("\n这里没有可见的物品");
    } else {
      sb.append("\n房间物品:\n");
      room.getRoomItems()
          .forEach(
              ri -> {
                Item item = ri.getItem();
                sb.append("- ")
                    .append(item.getName())
                    .append(" (数量: ")
                    .append(ri.getQuantity())
                    .append(", 重量: ")
                    .append(item.getWeight())
                    .append("kg")
                    .append(")\n   ")
                    .append(item.getDescription())
                    .append("\n");
              });
    }

    // 添加传送门信息
    if (room.isTeleportRoom() && room.getTeleportDestinations() != null) {
      sb.append("\n传送门能量波动中，可能通往未知领域...");
    }

    return sb.toString();
  }

  // 转换物品栏格式
  private static List<GameResponse.ItemDetail> convertInventory(List<Item> inventory) {
    if (inventory == null || inventory.isEmpty()) {
      return Collections.emptyList();
    }

    return inventory.stream()
        .map(
            item -> {
              String name = item.getName() != null ? item.getName() : "未命名物品";
              String desc = item.getDescription() != null ? item.getDescription() : "无描述";

              return GameResponse.ItemDetail.builder()
                  .name(name)
                  .description(desc)
                  .weight(item.getWeight())
                  .build();
            })
        .collect(Collectors.toList());
  }

  // ==================== 数据库操作方法 (修改为基于用户ID) ====================

  @Transactional
  public void removeItemFromRoom(RoomEntity room, Item item) {
    // 找到要移除的房间物品
    RoomItem roomItem =
        room.getRoomItems().stream()
            .filter(ri -> ri.getItem().equals(item))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("物品不存在于房间"));

    // 获取当前数量（空值安全处理）
    Integer quantity = roomItem.getQuantity();
    int currentQuantity = (quantity != null) ? quantity : 1; // 默认为1

    if (currentQuantity > 1) {
      // 减少数量
      roomItem.setQuantity(currentQuantity - 1);
      roomItemRepo.save(roomItem); // 显式保存更改
    } else {
      // 完全移除物品
      room.getRoomItems().remove(roomItem);
      roomItemRepo.delete(roomItem); // 显式删除记录
    }

    // 保存父实体
    roomRepo.save(room);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void addItemToPlayerInventory(Long userId, Item item) {
    // 使用用户ID查找记录
    Optional<PlayerInventory> existingOpt =
        playerInventoryRepo.findByUserIdAndItemId(userId, Long.valueOf(item.getId()));

    if (existingOpt.isPresent()) {
      PlayerInventory existing = existingOpt.get();
      existing.setQuantity(existing.getQuantity() + 1);
      playerInventoryRepo.save(existing);
    } else {
      PlayerInventory newEntry = new PlayerInventory();
      newEntry.setUserId(userId); // 使用用户ID而非sessionId
      newEntry.setItem(item);
      newEntry.setQuantity(1);
      playerInventoryRepo.save(newEntry);
    }
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void removeItemFromPlayerInventory(Long userId, Item item) {
    // 根据用户ID查找记录
    Optional<PlayerInventory> existingOpt =
        playerInventoryRepo.findByUserIdAndItemId(userId, Long.valueOf(item.getId()));

    if (existingOpt.isPresent()) {
      PlayerInventory existing = existingOpt.get();
      if (existing.getQuantity() > 1) {
        existing.setQuantity(existing.getQuantity() - 1);
        playerInventoryRepo.save(existing);
      } else {
        playerInventoryRepo.deleteById(existing.getId());
      }
    }
  }

  // 添加物品到房间
  private void addItemToRoom(RoomEntity room, Item item) {
    // 按 Item ID 比较，而不是用 .equals(item)
    RoomItem existingItem =
        room.getRoomItems().stream()
            .filter(
                ri -> {
                  // 先防空，再按 id 比较
                  Item roomItem = ri.getItem();
                  return roomItem != null
                      && roomItem.getId() != null
                      && roomItem.getId().equals(item.getId());
                })
            .findFirst()
            .orElse(null);

    if (existingItem != null) {
      // 房间已有该物品，只更新数量
      Integer quantity = existingItem.getQuantity();
      int currentQuantity = (quantity != null) ? quantity : 0;
      existingItem.setQuantity(currentQuantity + 1);
      roomItemRepo.save(existingItem);
    } else {
      // 房间暂时没有该物品，新建一条
      RoomItem newRoomItem = new RoomItem();
      newRoomItem.setRoom(room);
      newRoomItem.setItem(item);
      newRoomItem.setQuantity(1);
      roomItemRepo.save(newRoomItem);
      room.getRoomItems().add(newRoomItem);
    }
    roomRepo.save(room);
  }

  // ==================== 命令分发器 ====================
  @Transactional
  public GameResponse processCommand(String sessionId, String command, String parameter) {
    // 恢复会话
    GameResponse response = restoreSession(sessionId);
    if (!response.isSuccess()) {
      return response;
    }

    // 分发命令
    String normalizedCommand = command.toLowerCase();

    switch (normalizedCommand) {
      case "go":
        return processGoCommand(sessionId, parameter);
      case "look":
        return processLookCommand(sessionId);
      case "back":
        return processBackCommand(sessionId);
      case "take":
        return processTakeCommand(sessionId, parameter);
      case "drop":
        return processDropCommand(sessionId, parameter);
      case "eat": // 添加 eat 命令
        return processEatCommand(sessionId, parameter);
      case "items":
        return processItemsCommand(sessionId);
      default:
        return GameResponse.failure("未知命令: " + command);
    }
  }

  @PostConstruct
  @Transactional
  public void placeMagicCake() {
    if (!magicCakePlaced) {
      // 检查物品表中是否存在 Magic Cake
      Optional<Item> cakeOpt =
          itemRepo.findAll().stream()
              .filter(item -> matchesItemName(item, MAGIC_CAKE_NAME))
              .findFirst();
      if (!cakeOpt.isPresent()) {
        logger.warn("物品表中找不到魔法蛋糕，请确保已添加该物品");
        return;
      }

      Item cake = cakeOpt.get();

      // 检查蛋糕是否已经存在于某个房间中
      List<RoomItem> existingCakes = roomItemRepo.findByItem(cake);
      if (!existingCakes.isEmpty()) {
        logger.info("魔法蛋糕已经存在于房间ID: {}", existingCakes.get(0).getRoom().getId());
        magicCakePlaced = true;
        return;
      }

      // 获取所有房间
      List<RoomEntity> allRooms = roomRepo.findAll();
      if (!allRooms.isEmpty()) {
        // 随机选择一个房间
        Random random = new Random();
        RoomEntity randomRoom = allRooms.get(random.nextInt(allRooms.size()));

        // 将蛋糕添加到房间
        addItemToRoom(randomRoom, cake);
        logger.info("魔法蛋糕已放置在房间: {} (房间ID: {})", randomRoom.getName(), randomRoom.getId());
        magicCakePlaced = true;
      } else {
        logger.warn("找不到任何房间，无法放置魔法蛋糕");
      }
    }
  }

  @Transactional
  public GameResponse processEatCommand(String sessionId, String itemName) {
    logger.debug("处理 eat 命令: sessionId={}, itemName={}", sessionId, itemName);

    GameState state = validateUserSession(sessionId);
    User user = userRepo.findBySessionId(sessionId).orElse(null);
    if (user == null) return GameResponse.failure("用户不存在");

    // 在玩家物品栏中查找物品
    Optional<Item> itemOpt =
        state.getInventory().stream().filter(item -> matchesItemName(item, itemName)).findFirst();
    if (!itemOpt.isPresent()) {
      return GameResponse.failure("您没有该物品: " + itemName);
    }

    Item item = itemOpt.get();

    // 检查物品是否可食用
    if (!item.isEdible()) {
      return GameResponse.failure(item.getName() + "不可食用");
    }

    try {
      // 从内存状态移除
      state.removeFromInventory(item);

      // 从数据库移除
      removeItemFromPlayerInventory(user.getId(), item);

      // 处理特殊物品效果
      String effectMessage;
      if (MAGIC_CAKE_NAME.equalsIgnoreCase(item.getName())) {
        // 提升最大负重能力
        double newMaxWeight = user.getMaxCarryWeight() + WEIGHT_BOOST_AMOUNT;
        user.setMaxCarryWeight(newMaxWeight);
        userRepo.save(user);

        effectMessage = "你感到一股神奇的力量涌入体内！最大负重能力提升了 " + WEIGHT_BOOST_AMOUNT + "kg";

        // 记录蛋糕已被消耗
        magicCakePlaced = false;
      } else {
        // 普通可食用物品
        effectMessage = "你吃掉了 " + item.getName();
      }

      // 重新计算当前负重
      double currentWeight = calculateCurrentWeight(state.getInventory());
      double maxWeight = user.getMaxCarryWeight();

      return GameResponse.success(
          sessionId,
          effectMessage,
          state.getCurrentRoom(),
          state.getInventory(),
          GameResponse.UserInfo.fromUser(user),
          currentWeight,
          maxWeight);
    } catch (Exception e) {
      logger.error("食用物品失败", e);
      return GameResponse.failure("食用物品失败: " + e.getMessage());
    }
  }

  private boolean matchesItemName(Item item, String targetName) {
    return item.getName().trim().equalsIgnoreCase(targetName.trim());
  }
}
