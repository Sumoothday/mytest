package com.example.demo1111111.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo1111111.entity.Item;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

  // 直接使用查询方法推导（推荐）
  Optional<Item> findByName(String name);

  // 或者使用自定义JPQL（如果确实需要）
  @Query("SELECT i FROM Item i WHERE i.name = :name")
  Optional<Item> findByNameCustom(@Param("name") String name);
}
