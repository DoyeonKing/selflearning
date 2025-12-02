package com.example.springboot.repository;

import com.example.springboot.entity.MapNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 地图节点Repository
 */
@Repository
public interface MapNodeRepository extends JpaRepository<MapNode, Integer> {
    
    /**
     * 根据二维码内容查找节点
     */
    Optional<MapNode> findByQrcodeContent(String qrcodeContent);
    
    /**
     * 根据二维码状态查找节点列表（使用字符串字段）
     */
    @Query("SELECT n FROM MapNode n WHERE n.qrcodeStatusStr = :status")
    List<MapNode> findByQrcodeStatus(@Param("status") String status);
    
    /**
     * 查找所有已激活二维码的节点（使用字符串字段）
     */
    @Query("SELECT n FROM MapNode n WHERE n.qrcodeStatusStr = 'active' AND n.qrcodeContent IS NOT NULL")
    List<MapNode> findAllActiveQRCodeNodes();
    
    /**
     * 根据节点名称查找
     */
    Optional<MapNode> findByNodeName(String nodeName);
    
    /**
     * 根据楼层查找节点
     */
    List<MapNode> findByFloorLevel(Integer floorLevel);
}



