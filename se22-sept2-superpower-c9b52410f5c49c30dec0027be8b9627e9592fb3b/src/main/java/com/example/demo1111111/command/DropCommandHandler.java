package com.example.demo1111111.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.demo1111111.dto.GameResponse;
import com.example.demo1111111.service.GameService;

@Component
public class DropCommandHandler implements CommandHandler {

  private final GameService gameService;

  @Autowired
  public DropCommandHandler(GameService gameService) {
    this.gameService = gameService;
  }

  @Override
  public String getCommandName() {
    return "drop";
  }

  @Override
  public GameResponse handleCommand(String sessionId, String[] commandParts) {
    if (commandParts.length < 2) {
      return GameResponse.failure("请指定要丢弃的物品名称");
    }
    String itemName = commandParts[1];
    return gameService.processDropCommand(sessionId, itemName);
  }
}
