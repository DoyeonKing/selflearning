#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
直接生成二维码文件到本地文件夹
不需要数据库，直接生成所有节点的二维码
"""

import qrcode
import os

# 节点列表（从数据库获取或手动配置）
NODES = [
    {"nodeId": 1, "name": "医院大门"},
    {"nodeId": 2, "name": "分诊台"},
    {"nodeId": 3, "name": "电梯口"},
    {"nodeId": 4, "name": "内科诊室"},
    {"nodeId": 5, "name": "外科诊室"},
]

def generate_qrcode_file(node_id, name):
    """生成二维码文件"""
    content = f"HOSPITAL_NODE_{node_id}"
    
    # 创建输出目录
    output_dir = "qrcode"
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
        print(f"✅ 创建文件夹: {output_dir}/")
    
    # 创建二维码
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_M,
        box_size=10,
        border=4,
    )
    qr.add_data(content)
    qr.make(fit=True)
    
    # 生成图片
    img = qr.make_image(fill_color="black", back_color="white")
    
    # 清理文件名
    safe_name = name.replace('/', '_').replace('\\', '_').replace(':', '_')
    filename = f"{output_dir}/qrcode_node_{node_id}_{safe_name}.png"
    
    # 保存文件
    img.save(filename)
    
    print(f"✅ 已生成: {filename}")
    print(f"   内容: {content}")
    print(f"   位置: {name}\n")
    
    return filename

def main():
    print("=" * 60)
    print("医院导航系统 - 二维码文件生成工具")
    print("=" * 60)
    print()
    print(f"📋 准备生成 {len(NODES)} 个二维码...")
    print()
    
    generated_files = []
    
    for node in NODES:
        try:
            filename = generate_qrcode_file(node["nodeId"], node["name"])
            generated_files.append(filename)
        except Exception as e:
            print(f"❌ 生成失败 (节点{node['nodeId']}): {e}")
    
    print()
    print("=" * 60)
    print(f"✅ 生成完成！共生成 {len(generated_files)} 个二维码文件")
    print(f"📁 保存位置: {os.path.abspath('qrcode')}")
    print()
    print("📋 文件列表:")
    for f in generated_files:
        print(f"   - {f}")
    print()
    print("💡 提示: 二维码文件保存在 tools/qrcode/ 文件夹中")
    print("=" * 60)

if __name__ == "__main__":
    try:
        main()
    except ImportError:
        print("❌ 错误：缺少 qrcode 库")
        print()
        print("请先安装依赖：")
        print("  pip install qrcode[pil]")
    except Exception as e:
        print(f"❌ 生成失败: {e}")
        import traceback
        traceback.print_exc()





