package com.example.demo1111111.command;

// 添加此行

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo1111111.dto.GameResponse;
import com.example.demo1111111.service.GameService;

@Component("take")
public class TakeCommandHandler implements CommandHandler {

  private final GameService gameService;

  @Autowired
  public TakeCommandHandler(GameService gameService) {
    this.gameService = gameService;
  }

  @Override
  public String getCommandName() {
    return "take";
  }

  @Override
  public GameResponse handleCommand(String sessionId, String[] commandParts) {
    if (commandParts.length < 2) {
      return GameResponse.failure("请指定要拾取的物品名称");
    }
    String itemName = commandParts[1];
    return gameService.processTakeCommand(sessionId, itemName);
  }
}
