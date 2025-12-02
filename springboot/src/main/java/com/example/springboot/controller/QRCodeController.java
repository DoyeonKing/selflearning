package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.MapNode;
import com.example.springboot.service.QRCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 二维码管理控制器
 * 提供二维码的生成、上传、查询等接口
 */
@RestController
@RequestMapping("/api/qrcode")
@CrossOrigin(origins = "*")
public class QRCodeController {
    
    private final QRCodeService qrCodeService;
    
    @Autowired
    public QRCodeController(QRCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }
    
    /**
     * 为节点生成二维码内容
     * POST /api/qrcode/generate/{nodeId}
     */
    @PostMapping("/generate/{nodeId}")
    public ResponseEntity<Result> generateQRCode(@PathVariable Integer nodeId) {
        try {
            MapNode node = qrCodeService.generateQRCodeForNode(nodeId);
            return ResponseEntity.ok(Result.success(node));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "生成二维码失败: " + e.getMessage()));
        }
    }
    
    /**
     * 上传二维码图片
     * POST /api/qrcode/upload/{nodeId}
     */
    @PostMapping("/upload/{nodeId}")
    public ResponseEntity<Result> uploadQRCodeImage(
            @PathVariable Integer nodeId,
            @RequestParam("file") MultipartFile file) {
        try {
            MapNode node = qrCodeService.uploadQRCodeImage(nodeId, file);
            return ResponseEntity.ok(Result.success(node));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error("400", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "上传二维码失败: " + e.getMessage()));
        }
    }
    
    /**
     * 根据二维码内容查找节点（用于扫码定位）
     * GET /api/qrcode/scan/{qrcodeContent}
     */
    @GetMapping("/scan/{qrcodeContent}")
    public ResponseEntity<Result> scanQRCode(@PathVariable String qrcodeContent) {
        try {
            MapNode node = qrCodeService.findByQRCodeContent(qrcodeContent);
            return ResponseEntity.ok(Result.success(node));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("404", "未找到对应的节点: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有节点的二维码信息
     * GET /api/qrcode/nodes
     */
    @GetMapping("/nodes")
    public ResponseEntity<Result> getAllNodes() {
        try {
            List<MapNode> nodes = qrCodeService.getAllNodesWithQRCode();
            return ResponseEntity.ok(Result.success(nodes));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "获取节点列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有已激活二维码的节点
     * GET /api/qrcode/nodes/active
     */
    @GetMapping("/nodes/active")
    public ResponseEntity<Result> getActiveQRCodeNodes() {
        try {
            List<MapNode> nodes = qrCodeService.getActiveQRCodeNodes();
            return ResponseEntity.ok(Result.success(nodes));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "获取激活节点列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新二维码状态
     * PUT /api/qrcode/status/{nodeId}
     */
    @PutMapping("/status/{nodeId}")
    public ResponseEntity<Result> updateQRCodeStatus(
            @PathVariable Integer nodeId,
            @RequestParam("status") String status) {
        try {
            MapNode.QRCodeStatus qrCodeStatus = MapNode.QRCodeStatus.valueOf(status.toUpperCase());
            MapNode node = qrCodeService.updateQRCodeStatus(nodeId, qrCodeStatus);
            return ResponseEntity.ok(Result.success(node));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Result.error("400", "无效的状态值: " + status));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "更新状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除二维码（软删除）
     * DELETE /api/qrcode/{nodeId}
     */
    @DeleteMapping("/{nodeId}")
    public ResponseEntity<Result> deleteQRCode(@PathVariable Integer nodeId) {
        try {
            MapNode node = qrCodeService.deleteQRCode(nodeId);
            return ResponseEntity.ok(Result.success(node));
        } catch (Exception e) {
            return ResponseEntity.ok(Result.error("500", "删除二维码失败: " + e.getMessage()));
        }
    }
    
    /**
     * 下载二维码图片
     * GET /api/qrcode/image/{nodeId}
     */
    @GetMapping("/image/{nodeId}")
    public ResponseEntity<Resource> downloadQRCodeImage(@PathVariable Integer nodeId) {
        try {
            // 先获取节点信息
            MapNode node = qrCodeService.getAllNodesWithQRCode().stream()
                    .filter(n -> n.getNodeId().equals(nodeId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("节点不存在"));
            
            if (node.getQrcodeImagePath() == null) {
                return ResponseEntity.notFound().build();
            }
            
            File file = qrCodeService.getQRCodeImageFile(node.getQrcodeImagePath());
            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + file.getName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}



