package com.example.demo1111111.entity;

import java.util.Date;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "username")
  private String username;

  @Column(name = "password")
  private String password;

  @Column(name = "current_roomid")
  private Integer currentRoomId;

  @Column(name = "session_id")
  private String sessionId;

  @Column(name = "last_login")
  @Temporal(TemporalType.TIMESTAMP)
  private Date lastLogin;

  @Column(name = "max_carry_weight", nullable = false, columnDefinition = "DOUBLE DEFAULT 50.0")
  private Double maxCarryWeight = 50.0;

  // Getters and setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Integer getCurrentRoomId() {
    return currentRoomId;
  }

  public void setCurrentRoomId(Integer currentRoomId) {
    this.currentRoomId = currentRoomId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public Date getLastLogin() {
    return lastLogin;
  }

  public void setLastLogin(Date lastLogin) {
    this.lastLogin = lastLogin;
  }

  public Double getMaxCarryWeight() {
    return maxCarryWeight;
  }

  public void setMaxCarryWeight(Double maxCarryWeight) {
    this.maxCarryWeight = maxCarryWeight;
  }

  // toString method for debugging
  @Override
  public String toString() {
    return "User{"
        + "id="
        + id
        + ", username='"
        + username
        + '\''
        + ", password='"
        + password
        + '\''
        + ", currentRoomId="
        + currentRoomId
        + ", sessionId='"
        + sessionId
        + '\''
        + ", lastLogin="
        + lastLogin
        + '}';
  }

  // equals and hashCode for proper comparison
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User)) return false;

    User user = (User) o;
    return id != null && id.equals(user.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }

  // Builder pattern for convenient object creation
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private Long id;
    private String username;
    private String password;
    private Integer currentRoomId;
    private String sessionId;
    private Date lastLogin;

    public Builder id(Long id) {
      this.id = id;
      return this;
    }

    public Builder username(String username) {
      this.username = username;
      return this;
    }

    public Builder password(String password) {
      this.password = password;
      return this;
    }

    public Builder currentRoomId(Integer currentRoomId) {
      this.currentRoomId = currentRoomId;
      return this;
    }

    public Builder sessionId(String sessionId) {
      this.sessionId = sessionId;
      return this;
    }

    public Builder lastLogin(Date lastLogin) {
      this.lastLogin = lastLogin;
      return this;
    }

    public User build() {
      User user = new User();
      user.setId(id);
      user.setUsername(username);
      user.setPassword(password);
      user.setCurrentRoomId(currentRoomId);
      user.setSessionId(sessionId);
      user.setLastLogin(lastLogin);
      return user;
    }
  }
}
