# GdxGradle 插件

LibGDX 项目统一构建任务插件，提供版本管理、BuildConfig 生成、资源清单、Git 提交规范、Changelog、JPackage 打包等功能。

---

## 目录

- [快速开始](#快速开始)
- [功能概览](#功能概览)
- [版本管理](#版本管理)
  - [版本任务](#版本任务)
  - [BuildConfig 生成](#buildconfig-生成)
  - [构建流水号](#构建流水号)
- [资源清单](#资源清单)
- [Git 提交规范](#git-提交规范)
- [Changelog](#changelog)
- [JPackage 打包](#jpackage-打包)
- [文档系统](#文档系统)
- [模板发布](#模板发布)
- [配置参考](#配置参考)
- [JitPack 使用](#jitpack-使用)

---

## 快速开始

### 1. 添加插件

```groovy
// build.gradle
plugins {
    id 'com.goldsprite.gdx-tasks' version '1.0.0'
}
```

### 2. 基础配置

```groovy
// build.gradle
gdxTasks {
    taskGroup = 'mygame'
    hasLwjgl3 = true
    hasAndroid = true
}
```

### 3. 配置版本

```properties
# gradle.properties
projectPackage=com.example.mygame
projectVersion=0.1.0
jdkVersion=17
```

### 4. 运行任务

```bash
./gradlew tasks --group=mygame
```

---

## 功能概览

| 功能 | 任务组 | 说明 |
|------|--------|------|
| **Versioning** | `versioning` | 版本号管理与同步 |
| **BuildConfig** | `versioning` | 自动生成 BuildConfig.java |
| **AssetTasks** | `assets` | 资源清单自动生成 |
| **GitHook** | - | Git 提交规范校验 |
| **Changelog** | `changelog` | 自动生成版本变更日志 |
| **JPackage** | `jpackage` | 桌面端打包（Windows EXE）|
| **DocsPreview** | `docs` | 文档预览服务器 |
| **PublishDocs** | `docs` | 文档发布 |
| **PublishTemplate** | `template` | 模板发布 |

---

## 版本管理

### 版本任务

| 任务 | 命令 | 说明 | 示例 |
|------|------|------|------|
| `syncVersions` | `./gradlew syncVersions` | 同步版本号到所有文件 | - |
| `bumpBuild` | `./gradlew bumpBuild` | 添加/递增 build 号 | `0.10.10` → `0.10.10.0_build` |
| `bumpPatch` | `./gradlew bumpPatch` | 升级修订号 | `0.10.10.x_build` → `0.10.11` |
| `bumpMinor` | `./gradlew bumpMinor` | 升级次版本号 | `0.10.10` → `0.11.0` |
| `bumpMajor` | `./gradlew bumpMajor` | 升级主版本号 | `0.10.10` → `1.0.0` |
| `printVersion` | `./gradlew printVersion` | 打印当前版本号 | - |
| `markBuildRelease` | `./gradlew markBuildRelease` | 标记构建产物，递增 buildCount | - |

### 版本号规则

遵循语义化版本：`v主.次.修.build`

| 变更类型 | 版本位 | 示例 |
|----------|--------|------|
| Bug 修复/小调整 | 修订版本 +1 | `0.10.10` → `0.10.11` |
| 功能增加/改进 | 次版本 +1 | `0.10.10` → `0.11.0` |
| 大功能/架构重构 | 主版本 +1 | `0.10.10` → `1.0.0` |
| 开发期迭代 | 添加 build 后缀 | `0.10.10` → `0.10.10.0_build` |

### BuildConfig 生成

自动从 `gradle.properties` 读取版本信息，生成 `BuildConfig.java`。

#### 生成内容示例

```java
package com.example.mygame;

/**
 * 项目构建配置信息
 * 此文件由 Gradle 插件自动生成，请勿手动修改
 */
public class BuildConfig {
    public static final String PROJECT_NAME = "MyGame";
    public static final String DEV_VERSION = "0.10.10.1_build";
    public static final String JDK_VERSION = "17";
    
    // 构建流水号（项目生命周期总构建次数）
    public static final int BUILD_COUNT = 9;
    public static final String DISPLAY_VERSION = "0.10.10.1_build#9";
    
    // Android 版本号（与 BUILD_COUNT 同步）
    public static final int VERSION_CODE = 9;
    
    // 构建信息
    public static final long BUILD_TIMESTAMP = 1773176433508L;
    public static final String BUILD_TIME = "2026-03-11 05:00:33";
}
```

#### 生成位置

```
core/src/main/java/{projectPackage}/BuildConfig.java
```

#### 自定义模板

```groovy
gdxTasks {
    buildConfig {
        templateFile = file('templates/MyBuildConfig.groovy')
    }
}
```

模板使用 Groovy 语法，支持 `${}` 表达式：

```groovy
package ${projectPackage};

public class BuildConfig {
    public static final String PROJECT_NAME = "${projectName}";
    public static final String DEV_VERSION = "${projectVersion}";
    public static final String JDK_VERSION = "${jdkVersion}";
    
    public static final int BUILD_COUNT = ${buildCount};
    public static final String DISPLAY_VERSION = "${projectVersion}#${buildCount}";
    
    public static final long BUILD_TIMESTAMP = ${System.currentTimeMillis()}L;
    public static final String BUILD_TIME = "${new java.util.Date().format('yyyy-MM-dd HH:mm:ss')}";
}
```

#### 可用模板变量

| 变量 | 类型 | 说明 |
|------|------|------|
| `projectName` | String | 项目名称（rootProject.name）|
| `projectVersion` | String | 版本号（projectVersion）|
| `projectPackage` | String | 包名（projectPackage）|
| `jdkVersion` | String | JDK 版本 |
| `buildCount` | int | 构建流水号 |
| `rootProject` | Project | Gradle 根项目对象 |

### 构建流水号（buildCount）

- 存储在 `build.properties`，单调递增
- 项目生命周期内总构建次数，不随版本重置

#### 触发时机

1. **版本变更时**：执行 `bumpXx` 任务（版本号变化触发 syncVersions）
2. **构建产物时**：项目打包任务挂钩 `markBuildRelease`

#### 项目端挂钩配置

**Android**（`android/build.gradle`）：

```groovy
android.applicationVariants.all { variant ->
    variant.outputs.all {
        finalizedBy rootProject.tasks.markBuildRelease
    }
}
```

**Desktop**（`lwjgl3/build.gradle`）：

```groovy
tasks.named('jpackage') {
    finalizedBy rootProject.tasks.markBuildRelease
}

// 或所有打包任务
tasks.withType(org.gradle.api.tasks.Exec) { task ->
    if (task.name.contains('jpackage')) {
        task.finalizedBy rootProject.tasks.markBuildRelease
    }
}
```

#### build.properties 格式

```properties
# 项目总构建流水号
# 每次构建产物或版本变更时自动递增，用于追踪项目生命周期内的总构建次数
#Mon Mar 11 05:00:33 CST 2026
lastVersion=0.10.10.1_build
buildCount=9
```

---

## 资源清单

自动扫描 assets 目录，生成 `assets.txt` 资源清单文件。

### 任务

| 任务 | 命令 | 说明 |
|------|------|------|
| `generateAssetList` | `./gradlew generateAssetList` | 生成资源清单 |

### 生成文件

```
assets/assets.txt
```

### 配置

```groovy
gdxTasks {
    // assets 目录（相对 rootProject，默认 'assets'）
    assetsDir = 'assets'
}
```

---

## Git 提交规范

自动安装 `commit-msg` 钩子，校验提交信息格式。

### 功能

- 提交前自动校验格式
- 支持自定义提交类型
- 支持跳过模式（如 WIP、TMP 等）

### 配置

```groovy
gdxTasks {
    // 是否自动安装 commit-msg 钩子（默认 true）
    installGitHook = true
    
    // 使用详细类型定义（默认 false）
    useDetailedCommitTypes = true
    
    // 添加提交类型
    commitType('feat', '新功能')
    commitType('fix', '修复问题')
    commitType('docs', '文档更新')
    commitType('style', '代码格式')
    commitType('refactor', '重构')
    commitType('test', '测试')
    commitType('chore', '构建/工具')
    
    // 或批量添加
    commitTypes(['feat:新功能', 'fix:修复', 'docs:文档'])
    
    // 自定义跳过模式（正则）
    skipPatterns = ['^wip[: ]', '^tmp[: ]', '^draft[: ]']
    
    // 启用默认跳过模式（Merge、Revert 等，默认 true）
    enableDefaultSkipPatterns = true
}
```

### 默认提交类型

| 类型 | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | 修复问题 |
| `docs` | 文档更新 |
| `style` | 代码格式（不影响功能）|
| `refactor` | 代码重构 |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `chore` | 构建/工具/配置 |
| `revert` | 回滚提交 |

### 提交格式

```
<type>: <subject>

<body>

<footer>
```

示例：
```
feat: 添加用户登录功能

实现 JWT Token 认证，支持记住登录状态。

Closes #123
```

详细说明见 [GitHook.md](docs/GitHook.md)

---

## Changelog

从 Git 历史自动生成版本变更日志。

### 任务

| 任务 | 命令 | 说明 |
|------|------|------|
| `generateChangelog` | `./gradlew generateChangelog` | 生成 changelog.json |

### 配置

```groovy
gdxTasks {
    // 回溯最多多少个 Tag 版本（默认 100）
    maxVersions = 100
    
    // 每个版本最多抓多少条 Commit（默认 500）
    maxCommitsPerVersion = 500
}
```

### 输出文件

```
changelog.json
```

格式：
```json
{
  "versions": [
    {
      "version": "v0.10.10",
      "date": "2026-03-11",
      "commits": [
        {
          "type": "feat",
          "message": "添加新功能",
          "hash": "abc1234"
        }
      ]
    }
  ]
}
```

---

## JPackage 打包

使用 JDK 14+ 的 jpackage 工具，将 Java 应用打包为原生安装包（Windows EXE、macOS DMG、Linux DEB/RPM）。

### 任务

| 任务 | 命令 | 说明 |
|------|------|------|
| `jpackageExe` | `./gradlew jpackageExe` | 打包为 EXE（Windows）|
| `packageSingleExe` | `./gradlew packageSingleExe` | 打包为单文件 EXE（需 Enigma Virtual Box）|
| `oneClickRelease` | `./gradlew oneClickRelease` | 一键构建 Jar → jpackage → 单文件 EXE |

### 环境要求

- JDK 14+（含 jpackage 工具）
- Windows：WiX Toolset 3.0+（用于生成 MSI）
- 单文件 EXE：Enigma Virtual Box

### 输出目录

```
outputs/
├── jpackage/          # jpackage 输出
└── single-exe/        # 单文件 EXE
```

---

## 文档系统

包含文档预览服务器和文档发布功能。

### 文档预览

| 任务 | 命令 | 说明 |
|------|------|------|
| `previewDocs` | `./gradlew previewDocs` | 启动文档预览服务器 |

#### 配置

```groovy
gdxTasks {
    // 文档预览服务器的主类全限定名
    docsPreviewMainClass = 'com.example.docs.DocServer'
}
```

### 文档发布

| 任务 | 命令 | 说明 |
|------|------|------|
| `buildDocsDist` | `./gradlew buildDocsDist` | 构建文档发布包 |
| `packageDocs` | `./gradlew packageDocs` | 打包文档 |

---

## 模板发布

发布模板到指定模板列表。

### 任务

| 任务 | 命令 | 说明 |
|------|------|------|
| `publishTemplate` | `./gradlew publishTemplate -Ptarget=TemplateName` | 发布模板 |

### 配置

```groovy
gdxTasks {
    // 模板根目录（相对 rootProject）
    templatesRoot = 'Templates'
}
```

---

## 配置参考

### 完整配置示例

```groovy
gdxTasks {
    // ═══════════════════════════════════════════
    // 基础配置
    // ═══════════════════════════════════════════
    
    // 任务分组名（默认 'gdxTasks'）
    taskGroup = 'mygame'
    
    // 模块绑定（用于自动挂载依赖）
    hasLwjgl3 = true
    hasAndroid = true
    hasExamples = false
    
    // assets 目录（相对 rootProject，默认 'assets'）
    assetsDir = 'assets'
    
    // 是否自动执行 syncVersions（默认 true）
    autoSyncVersions = true
    
    // ═══════════════════════════════════════════
    // Changelog 配置
    // ═══════════════════════════════════════════
    
    maxVersions = 100
    maxCommitsPerVersion = 500
    
    // ═══════════════════════════════════════════
    // DocsPreview 配置
    // ═══════════════════════════════════════════
    
    docsPreviewMainClass = 'com.example.docs.DocServer'
    
    // ═══════════════════════════════════════════
    // PublishTemplate 配置
    // ═══════════════════════════════════════════
    
    templatesRoot = 'Templates'
    
    // ═══════════════════════════════════════════
    // GitHook 配置
    // ═══════════════════════════════════════════
    
    installGitHook = true
    useDetailedCommitTypes = true
    enableDefaultSkipPatterns = true
    
    commitType('feat', '新功能')
    commitType('fix', '修复问题')
    commitType('docs', '文档更新')
    
    skipPatterns = ['^wip[: ]', '^tmp[: ]']
    
    // ═══════════════════════════════════════════
    // BuildConfig 配置
    // ═══════════════════════════════════════════
    
    buildConfig {
        templateFile = file('templates/MyBuildConfig.groovy')
    }
}
```

### gradle.properties 配置

```properties
# ═══════════════════════════════════════════
# 项目基本信息（必需）
# ═══════════════════════════════════════════

# 项目包名
projectPackage=com.example.mygame

# 项目版本号
projectVersion=0.1.0

# JDK 版本
jdkVersion=17

# ═══════════════════════════════════════════
# GdxGradle 插件版本（如使用 buildscript 方式）
# ═══════════════════════════════════════════
# gdxGradleVersion=1.0.0
```

---

## JitPack 使用

### 1. 添加 JitPack 仓库

```groovy
// settings.gradle
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url "https://jitpack.io" }
    }
}
```

### 2. 添加插件

```groovy
// build.gradle
plugins {
    id 'com.goldsprite.gdx-tasks' version '1.0.0'
}
```

### 3. 或使用 buildscript

```groovy
// build.gradle
buildscript {
    repositories {
        maven { url "https://jitpack.io" }
    }
    dependencies {
        classpath 'com.github.shikeik:GdxGradle:1.0.0'
    }
}

apply plugin: 'com.goldsprite.gdx-tasks'
```

---

## GdxGradle 自举

GdxGradle 插件本身也使用自己的功能管理版本：

- **版本文件**：`gradle.properties`（`projectVersion=1.0.0`）
- **构建流水号**：独立的 `build.properties`（与使用方项目隔离）
- **BuildConfig**：`src/main/java/com/goldsprite/gdxgradle/BuildConfig.java`

当作为 included build 使用时，SandTank 的 `syncVersions` 会同时更新 GdxGradle 的 BuildConfig。

---

## 许可证

MIT License
