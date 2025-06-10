package com.example.demo1111111.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo1111111.entity.TeleportDestination;
import com.example.demo1111111.entity.TeleportDestinationId;

public interface TeleportDestinationRepository
    extends JpaRepository<TeleportDestination, TeleportDestinationId> {

  List<TeleportDestination> findByTeleportRoomId(Integer teleportRoomId);
}
