package com.example.springboot.repository;

import com.example.springboot.entity.MapEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * 地图路径Repository
 */
public interface MapEdgeRepository extends JpaRepository<MapEdge, Integer> {
    
    /**
     * 根据起始节点ID查找所有连接的路径
     */
    List<MapEdge> findByStartNodeId(Integer startNodeId);
    
    /**
     * 根据结束节点ID查找所有连接的路径
     */
    List<MapEdge> findByEndNodeId(Integer endNodeId);
    
    /**
     * 查找两个节点之间的路径（双向）
     */
    @Query("SELECT e FROM MapEdge e WHERE " +
           "(e.startNodeId = :nodeId1 AND e.endNodeId = :nodeId2) OR " +
           "(e.startNodeId = :nodeId2 AND e.endNodeId = :nodeId1 AND e.isBidirectional = true)")
    List<MapEdge> findPathBetweenNodes(@Param("nodeId1") Integer nodeId1, @Param("nodeId2") Integer nodeId2);
    
    /**
     * 查找从指定节点出发的所有可达节点（考虑双向性）
     */
    @Query("SELECT e FROM MapEdge e WHERE e.startNodeId = :nodeId OR " +
           "(e.endNodeId = :nodeId AND e.isBidirectional = true)")
    List<MapEdge> findReachableNodes(@Param("nodeId") Integer nodeId);
    
    /**
     * 删除与指定节点相关的所有路径
     */
    void deleteByStartNodeId(Integer startNodeId);
    void deleteByEndNodeId(Integer endNodeId);
}



