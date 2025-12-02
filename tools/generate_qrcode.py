#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
医院导航系统 - 二维码生成工具
用于生成定位二维码

使用方法：
1. 安装依赖：pip install qrcode[pil]
2. 运行脚本：python generate_qrcode.py
3. 二维码会保存在当前目录的 qrcode 文件夹中
"""

import qrcode
import os

# 节点列表（可以从数据库或配置文件中读取）
NODES = [
    {"nodeId": 1, "name": "医院大门"},
    {"nodeId": 2, "name": "分诊台"},
    {"nodeId": 3, "name": "电梯口"},
    {"nodeId": 4, "name": "内科诊室"},
    {"nodeId": 5, "name": "外科诊室"},
]

def generate_qrcode(node_id, name):
    """生成单个二维码"""
    content = f"HOSPITAL_NODE_{node_id}"
    
    # 创建二维码
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_M,  # 中等纠错级别
        box_size=10,  # 每个小方块10像素
        border=4,  # 边框4个小方块
    )
    qr.add_data(content)
    qr.make(fit=True)
    
    # 生成图片
    img = qr.make_image(fill_color="black", back_color="white")
    
    # 创建输出目录
    output_dir = "qrcode"
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        print(f"✅ 创建输出目录: {output_dir}")
    
    # 保存图片
    filename = f"{output_dir}/qrcode_node_{node_id}_{name}.png"
    img.save(filename)
    
    print(f"✅ 已生成: {filename}")
    print(f"   内容: {content}")
    print(f"   位置: {name}\n")
    
    return filename

def main():
    """主函数"""
    print("=" * 50)
    print("医院导航系统 - 二维码生成工具")
    print("=" * 50)
    print()
    
    generated_files = []
    
    for node in NODES:
        filename = generate_qrcode(node["nodeId"], node["name"])
        generated_files.append(filename)
    
    print("=" * 50)
    print(f"✅ 所有二维码生成完成！共生成 {len(generated_files)} 个二维码")
    print(f"📁 输出目录: qrcode/")
    print()
    print("📋 使用说明：")
    print("1. 打印生成的二维码图片")
    print("2. 在医院对应位置张贴")
    print("3. 在导航页面使用'扫码定位'功能扫描")
    print("=" * 50)

if __name__ == "__main__":
    try:
        main()
    except ImportError:
        print("❌ 错误：缺少 qrcode 库")
        print()
        print("请先安装依赖：")
        print("  pip install qrcode[pil]")
        print()
        print("或者使用在线生成器：")
        print("  https://cli.im/")
    except Exception as e:
        print(f"❌ 生成失败: {e}")








