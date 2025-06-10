package com.example.demo1111111.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo1111111.dto.GameResponse;
import com.example.demo1111111.service.GameService;

@Component
public class GoCommandHandler implements CommandHandler {

  private final GameService gameService;

  @Autowired
  public GoCommandHandler(GameService gameService) {
    this.gameService = gameService;
  }

  @Override
  public String getCommandName() {
    return "go"; // 返回命令名称
  }

  @Override
  public GameResponse handleCommand(String sessionId, String[] commandParts) {
    // 参数校验 - 保持原有逻辑
    if (commandParts.length < 2) {
      return GameResponse.failure("必须指定移动方向");
    }

    // 获取方向参数 - 保持原有逻辑
    String direction = commandParts[1];

    // 调用服务层方法，保持原有功能
    return gameService.processGoCommand(sessionId, direction);
  }
}
