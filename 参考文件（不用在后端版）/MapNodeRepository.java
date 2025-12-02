package com.example.springboot.repository;

import com.example.springboot.entity.MapNode;
import com.example.springboot.entity.enums.NodeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MapNodeRepository extends JpaRepository<MapNode, Integer> {
    List<MapNode> findByFloorLevel(Integer floorLevel);
    List<MapNode> findByNodeNameContainingIgnoreCase(String keyword);
    List<MapNode> findByNodeType(NodeType nodeType);
}