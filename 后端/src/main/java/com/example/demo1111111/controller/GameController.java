package com.example.demo1111111.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo1111111.dto.GameResponse;
import com.example.demo1111111.service.GameService;

@RestController
@RequestMapping("/api")
public class GameController {

  private final GameService gameService;

  @Autowired
  public GameController(GameService gameService) {
    this.gameService = gameService;
  }

  // 1. 用户登录接口
  @PostMapping("/login")
  public GameResponse loginUser(
      @RequestParam("username") String username, @RequestParam("password") String password) {
    return gameService.loginUser(username, password);
  }

  // 2. 会话恢复接口
  @PostMapping("/restore")
  public GameResponse restoreSession(@RequestParam("sessionId") String sessionId) {
    return gameService.restoreSession(sessionId);
  }

  // 3. 命令处理接口
  @PostMapping("/command")
  public GameResponse handleCommand(
      @RequestHeader("X-Session-Id") String sessionId,
      @RequestParam("command") String command,
      @RequestParam(value = "parameter", required = false) String parameter) {
    return gameService.processCommand(sessionId, command, parameter);
  }
}
