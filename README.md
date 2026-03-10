# GdxGradle 插件

LibGDX 项目统一构建任务插件，提供版本管理、资源清单、Git 提交规范、Changelog、JPackage 打包等功能。

---

## 快速开始

```groovy
// build.gradle
plugins {
    id 'com.goldsprite.gdx-tasks' version '0.9.0'
}

gdxTasks {
    taskGroup = 'mygame'
    docsPreviewMainClass = 'com.example.MyDocServer'
    templatesRoot = 'MyTemplates'
}
```

运行 `./gradlew tasks --group=mygame` 查看所有任务。

---

## 功能列表

| 功能 | 说明 | 文档 |
|------|------|------|
| **Versioning** | 版本号同步（BuildConfig、README、Android） | - |
| **AssetTasks** | 资源清单自动生成 | - |
| **GitHook** | Git 提交规范校验 | [GitHook.md](docs/GitHook.md) |
| **Changelog** | 自动生成版本变更日志 | - |
| **JPackage** | 桌面端打包 | - |
| **PublishDocs** | 文档发布 | - |
| **DocsPreview** | 文档预览服务器 | - |
| **PublishTemplate** | 模板发布 | - |

---

## 配置示例

### 基础配置

```groovy
gdxTasks {
    // 任务分组名
    taskGroup = 'sandtank'
    
    // 模块绑定
    hasLwjgl3 = true
    hasAndroid = true
    hasExamples = true
    
    // 版本管理
    maxVersions = 100
    maxCommitsPerVersion = 500
}
```

### Git 提交规范（详细见 [GitHook.md](docs/GitHook.md)）

```groovy
gdxTasks {
    // 启用详细类型定义
    useDetailedCommitTypes = true
    commitType('feat', '新功能')
    commitType('fix', '修复问题')
    
    // 或简单列表
    // commitTypes = ['feat', 'fix', 'docs']
    
    // 自定义跳过模式
    skipPatterns = ['^wip[: ]']
}
```

---

## 许可证

MIT License
