package com.example.demo1111111.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo1111111.entity.GameItem;

@Repository
public interface GameItemRepository extends JpaRepository<GameItem, Integer> {}
