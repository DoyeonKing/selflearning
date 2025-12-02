#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
直接生成二维码文件到本地文件夹
"""

import qrcode
import os

NODES = [
    {"nodeId": 1, "name": "医院大门"},
    {"nodeId": 2, "name": "分诊台"},
    {"nodeId": 3, "name": "电梯口"},
    {"nodeId": 4, "name": "内科诊室"},
    {"nodeId": 5, "name": "外科诊室"},
]

def generate_qrcode_file(node_id, name):
    content = f"HOSPITAL_NODE_{node_id}"
    output_dir = "qrcode"
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)
    
    qr = qrcode.QRCode(version=1, error_correction=qrcode.constants.ERROR_CORRECT_M, box_size=10, border=4)
    qr.add_data(content)
    qr.make(fit=True)
    img = qr.make_image(fill_color="black", back_color="white")
    
    safe_name = name.replace('/', '_').replace('\\', '_').replace(':', '_')
    filename = f"{output_dir}/qrcode_node_{node_id}_{safe_name}.png"
    img.save(filename)
    
    print(f"✅ {filename}")
    return filename

if __name__ == "__main__":
    print("=" * 60)
    print("生成二维码文件中...")
    print("=" * 60)
    
    generated = []
    for node in NODES:
        try:
            f = generate_qrcode_file(node["nodeId"], node["name"])
            generated.append(f)
        except Exception as e:
            print(f"❌ 失败: {e}")
    
    print()
    print(f"✅ 完成！生成了 {len(generated)} 个文件")
    print(f"📁 位置: {os.path.abspath('qrcode')}")
    print()
    print("文件列表:")
    for f in generated:
        print(f"  {f}")





