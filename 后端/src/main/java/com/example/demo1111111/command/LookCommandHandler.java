package com.example.demo1111111.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo1111111.dto.GameResponse;
import com.example.demo1111111.service.GameService;

@Component
public class LookCommandHandler implements CommandHandler {

  private final GameService gameService;

  @Autowired
  public LookCommandHandler(GameService gameService) {
    this.gameService = gameService;
  }

  @Override
  public String getCommandName() {
    return "look";
  }

  @Override
  public GameResponse handleCommand(String sessionId, String[] commandParts) {
    // 参数校验
    if (commandParts.length > 1) {
      return GameResponse.failure("look 命令不需要额外参数");
    }

    // 调用服务层方法，保持原有功能
    return gameService.processLookCommand(sessionId);
  }
}
