#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
批量生成所有节点的二维码
根据数据库中的节点数据生成二维码图片
"""

import qrcode
import os
import mysql.connector
from mysql.connector import Error

# 数据库配置（从application.yml获取）
DB_CONFIG = {
    'host': 'localhost',
    'database': 'hospital_05',
    'user': 'root',
    'password': '123456',
    'charset': 'utf8mb4'
}

def get_nodes_from_database():
    """从数据库获取所有节点信息"""
    nodes = []
    try:
        connection = mysql.connector.connect(**DB_CONFIG)
        cursor = connection.cursor(dictionary=True)
        
        query = """
            SELECT node_id, node_name, qrcode_content, qrcode_status
            FROM map_nodes
            ORDER BY node_id
        """
        cursor.execute(query)
        nodes = cursor.fetchall()
        
        cursor.close()
        connection.close()
        
        print(f"✅ 从数据库获取了 {len(nodes)} 个节点")
        return nodes
    except Error as e:
        print(f"❌ 数据库连接失败: {e}")
        print("⚠️  使用默认节点列表")
        # 如果数据库连接失败，使用默认节点
        return [
            {"node_id": i, "node_name": f"节点{i}", "qrcode_content": f"HOSPITAL_NODE_{i}", "qrcode_status": "PENDING"}
            for i in range(1, 15)
        ]

def generate_qrcode(node_id, node_name, qrcode_content):
    """生成单个二维码"""
    output_dir = "qrcode"
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        print(f"✅ 创建输出目录: {output_dir}")
    
    # 创建二维码
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_M,
        box_size=10,
        border=4,
    )
    qr.add_data(qrcode_content)
    qr.make(fit=True)
    
    # 生成图片
    img = qr.make_image(fill_color="black", back_color="white")
    
    # 保存图片
    filename = f"{output_dir}/qrcode_node_{node_id}_{node_name}.png"
    # 清理文件名中的特殊字符
    filename = filename.replace('/', '_').replace('\\', '_').replace(':', '_')
    img.save(filename)
    
    return filename

def main():
    """主函数"""
    print("=" * 60)
    print("医院导航系统 - 批量二维码生成工具")
    print("=" * 60)
    print()
    
    # 从数据库获取节点信息
    nodes = get_nodes_from_database()
    
    if not nodes:
        print("❌ 没有找到节点数据")
        return
    
    print(f"\n📋 节点列表 ({len(nodes)} 个):")
    print("-" * 60)
    for node in nodes:
        print(f"  节点{node['node_id']}: {node['node_name']} -> {node['qrcode_content']}")
    print("-" * 60)
    print()
    
    # 生成二维码
    generated_files = []
    for node in nodes:
        try:
            filename = generate_qrcode(
                node['node_id'],
                node['node_name'],
                node['qrcode_content'] or f"HOSPITAL_NODE_{node['node_id']}"
            )
            generated_files.append(filename)
            print(f"✅ 已生成: {filename}")
        except Exception as e:
            print(f"❌ 生成失败 (节点{node['node_id']}): {e}")
    
    print()
    print("=" * 60)
    print(f"✅ 所有二维码生成完成！共生成 {len(generated_files)} 个二维码")
    print(f"📁 输出目录: qrcode/")
    print()
    print("📋 下一步操作：")
    print("1. 检查生成的二维码图片")
    print("2. 打印二维码（建议尺寸：5cm × 5cm 或更大）")
    print("3. 在医院对应位置张贴")
    print("4. 使用API上传二维码图片到系统")
    print("=" * 60)

if __name__ == "__main__":
    try:
        main()
    except ImportError as e:
        print("❌ 缺少必要的库")
        print()
        print("请先安装依赖：")
        print("  pip install qrcode[pil] mysql-connector-python")
        print()
        print("或者使用在线生成器：")
        print("  打开 tools/qrcode_generator.html")
    except Exception as e:
        print(f"❌ 生成失败: {e}")
        import traceback
        traceback.print_exc()








