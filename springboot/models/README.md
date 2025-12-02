# 模型文件目录

## 📁 目录说明

此目录用于存放训练生成的 Word2Vec 模型文件。

## 📝 模型文件

训练完成后，模型文件会保存在此目录下：
- `medical_word2vec.bin` - Word2Vec 模型文件（二进制格式，推荐）

## ⚙️ 配置文件

在 `application.yml` 中配置模型路径：

```yaml
ai:
  model:
    path: models/medical_word2vec.bin
```

## 📋 训练模型

### Java全自动方案（推荐）

调用训练接口即可自动生成模型：
```bash
POST /api/ai-model/train
```

详见：`Java全自动模型训练指南.md`

## ✅ 验证

启动项目后，查看控制台日志：
- 成功：`✅ AI 模型加载成功！词汇量: XXXXXX`
- 失败：`⚠️ 未找到模型文件...`（会降级为关键词匹配模式）
