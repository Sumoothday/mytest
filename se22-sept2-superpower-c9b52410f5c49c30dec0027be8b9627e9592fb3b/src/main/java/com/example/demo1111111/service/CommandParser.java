package com.example.demo1111111.service;

public class CommandParser {
  public static ParsedCommand parse(String input) {
    String[] parts = input.trim().split("\\s+", 2);
    return new ParsedCommand(parts[0].toLowerCase(), parts.length > 1 ? parts[1] : null);
  }

  public record ParsedCommand(String command, String parameter) {}
}
