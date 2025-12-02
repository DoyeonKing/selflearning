package com.example.springboot.repository;

import com.example.springboot.entity.MapEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MapEdgeRepository extends JpaRepository<MapEdge, Integer> {
    List<MapEdge> findByStartNodeIdOrEndNodeIdAndIsBidirectionalTrue(
            Integer startNodeId, Integer endNodeId);
    List<MapEdge> findByStartNodeId(Integer startNodeId);
    List<MapEdge> findByEndNodeIdAndIsBidirectionalTrue(Integer endNodeId);
}