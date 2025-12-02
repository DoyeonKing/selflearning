package com.example.springboot.service;

import com.example.springboot.entity.MapNode;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.MapNodeRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 二维码服务
 * 处理二维码的生成、上传和管理
 */
@Service
public class QRCodeService {
    
    private final MapNodeRepository mapNodeRepository;
    
    @Value("${app.qrcode.upload-dir:uploads/qrcodes}")
    private String uploadDir;
    
    @Autowired
    public QRCodeService(MapNodeRepository mapNodeRepository) {
        this.mapNodeRepository = mapNodeRepository;
    }
    
    /**
     * 初始化上传目录（在Spring注入完所有字段后执行）
     */
    @PostConstruct
    private void initUploadDirectory() {
        try {
            if (uploadDir == null || uploadDir.isEmpty()) {
                uploadDir = "uploads/qrcodes"; // 默认值
            }
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("[QRCodeService] 创建二维码上传目录: " + uploadDir);
            }
        } catch (IOException e) {
            System.err.println("[QRCodeService] 创建上传目录失败: " + e.getMessage());
        }
    }
    
    /**
     * 为节点生成二维码内容
     */
    public String generateQRCodeContent(Integer nodeId) {
        return "HOSPITAL_NODE_" + nodeId;
    }
    
    /**
     * 为节点自动生成二维码内容并更新数据库
     */
    public MapNode generateQRCodeForNode(Integer nodeId) {
        MapNode node = mapNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResourceNotFoundException("节点不存在: " + nodeId));
        
        // 生成二维码内容
        String qrcodeContent = generateQRCodeContent(nodeId);
        node.setQrcodeContent(qrcodeContent);
        node.setQrcodeGeneratedAt(LocalDateTime.now());
        node.setQrcodeStatus(MapNode.QRCodeStatus.PENDING); // 待上传图片
        
        return mapNodeRepository.save(node);
    }
    
    /**
     * 上传二维码图片
     */
    public MapNode uploadQRCodeImage(Integer nodeId, MultipartFile file) throws IOException {
        MapNode node = mapNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResourceNotFoundException("节点不存在: " + nodeId));
        
        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("只能上传图片文件");
        }
        
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = "qrcode_node_" + nodeId + "_" + UUID.randomUUID().toString() + extension;
        
        // 保存文件
        Path uploadPath = Paths.get(uploadDir);
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // 更新节点信息
        String relativePath = uploadDir + "/" + filename;
        node.setQrcodeImagePath(relativePath);
        node.setQrcodeGeneratedAt(LocalDateTime.now());
        node.setQrcodeStatus(MapNode.QRCodeStatus.ACTIVE);
        
        return mapNodeRepository.save(node);
    }
    
    /**
     * 根据二维码内容查找节点（用于扫码定位）
     */
    public MapNode findByQRCodeContent(String qrcodeContent) {
        return mapNodeRepository.findByQrcodeContent(qrcodeContent)
                .orElseThrow(() -> new ResourceNotFoundException("未找到对应的二维码: " + qrcodeContent));
    }
    
    /**
     * 获取所有节点的二维码信息
     */
    public List<MapNode> getAllNodesWithQRCode() {
        return mapNodeRepository.findAll();
    }
    
    /**
     * 获取所有已激活二维码的节点
     */
    public List<MapNode> getActiveQRCodeNodes() {
        return mapNodeRepository.findAllActiveQRCodeNodes();
    }
    
    /**
     * 更新二维码状态
     */
    public MapNode updateQRCodeStatus(Integer nodeId, MapNode.QRCodeStatus status) {
        MapNode node = mapNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResourceNotFoundException("节点不存在: " + nodeId));
        
        node.setQrcodeStatus(status);
        return mapNodeRepository.save(node);
    }
    
    /**
     * 删除二维码（软删除，设置为停用状态）
     */
    public MapNode deleteQRCode(Integer nodeId) {
        MapNode node = mapNodeRepository.findById(nodeId)
                .orElseThrow(() -> new ResourceNotFoundException("节点不存在: " + nodeId));
        
        node.setQrcodeStatus(MapNode.QRCodeStatus.INACTIVE);
        return mapNodeRepository.save(node);
    }
    
    /**
     * 获取二维码图片文件
     */
    public File getQRCodeImageFile(String imagePath) {
        Path filePath = Paths.get(imagePath);
        File file = filePath.toFile();
        
        if (!file.exists() || !file.isFile()) {
            throw new ResourceNotFoundException("二维码图片不存在: " + imagePath);
        }
        
        return file;
    }
}



