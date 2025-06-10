package com.example.demo1111111.command;

import com.example.demo1111111.dto.GameResponse;

public interface CommandHandler {
  // 获取命令名称（如 "go", "take", "drop"）
  String getCommandName();

  // 处理命令的核心方法
  GameResponse handleCommand(String sessionId, String[] commandParts);
}
