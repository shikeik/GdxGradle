# GdxGradle 插件

LibGDX 项目统一构建任务插件，提供版本管理、BuildConfig 生成、资源清单、Git 提交规范、Changelog、JPackage 打包等功能。

---

## 快速开始

```groovy
// build.gradle
plugins {
    id 'com.goldsprite.gdx-tasks' version '1.0.0'
}

gdxTasks {
    taskGroup = 'mygame'
}
```

运行 `./gradlew tasks --group=mygame` 查看所有任务。

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

## 版本管理（Versioning）

### 版本任务

| 任务 | 命令 | 说明 | 示例 |
|------|------|------|------|
| `syncVersions` | `./gradlew syncVersions` | 同步版本号到所有文件 | - |
| `bumpBuild` | `./gradlew bumpBuild` | 添加/递增 build 号 | `0.10.10` → `0.10.10.0_build` |
| `bumpPatch` | `./gradlew bumpPatch` | 升级修订号 | `0.10.10.x_build` → `0.10.11` |
| `bumpMinor` | `./gradlew bumpMinor` | 升级次版本号 | `0.10.10` → `0.11.0` |
| `bumpMajor` | `./gradlew bumpMajor` | 升级主版本号 | `0.10.10` → `1.0.0` |
| `printVersion` | `./gradlew printVersion` | 打印当前版本号 | - |

### BuildConfig 生成

自动从 `gradle.properties` 读取版本信息，生成 `BuildConfig.java`：

```java
public class BuildConfig {
    public static final String PROJECT_NAME = "MyGame";
    public static final String DEV_VERSION = "0.10.10.1_build";
    public static final String JDK_VERSION = "17";
    
    // 构建流水号（项目生命周期总构建次数）
    public static final int BUILD_COUNT = 9;
    public static final String DISPLAY_VERSION = "0.10.10.1_build#9";
    
    // Android 版本号（与 BUILD_COUNT 同步）
    public static final int VERSION_CODE = 9;
    
    // 构建时间
    public static final long BUILD_TIMESTAMP = 1773176433508L;
    public static final String BUILD_TIME = "2026-03-11 05:00:33";
}
```

**存储位置**：`core/src/main/java/{package}/BuildConfig.java`

**模板自定义**：

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
    public static final String DEV_VERSION = "${projectVersion}";
    public static final int BUILD_COUNT = ${buildCount};
    public static final String BUILD_TIME = "${new Date().format('yyyy-MM-dd HH:mm:ss')}";
}
```

### 构建流水号（buildCount）

- 存储在 `build.properties`，单调递增
- **触发时机**：
  - 执行 `bumpXx` 任务时（版本号变化）
  - 项目打包任务挂钩 `markBuildRelease` 时

**项目端挂钩示例**：

```groovy
// android/build.gradle
android.applicationVariants.all { variant ->
    variant.outputs.all {
        finalizedBy rootProject.tasks.markBuildRelease
    }
}

// lwjgl3/build.gradle
tasks.named('jpackage') {
    finalizedBy rootProject.tasks.markBuildRelease
}
```

---

## 配置项

### 基础配置

```groovy
gdxTasks {
    // 任务分组名
    taskGroup = 'sandtank'
    
    // 模块绑定（用于自动挂载依赖）
    hasLwjgl3 = true
    hasAndroid = true
    hasExamples = true
    
    // assets 目录（相对 rootProject）
    assetsDir = 'assets'
    
    // 是否自动执行 syncVersions
    autoSyncVersions = true
}
```

### Changelog 配置

```groovy
gdxTasks {
    // 回溯最多多少个 Tag 版本
    maxVersions = 100
    
    // 每个版本最多抓多少条 Commit
    maxCommitsPerVersion = 500
}
```

### Git 提交规范（GitHook）

```groovy
gdxTasks {
    // 是否自动安装 commit-msg 钩子
    installGitHook = true
    
    // 使用详细类型定义
    useDetailedCommitTypes = true
    commitType('feat', '新功能')
    commitType('fix', '修复问题')
    commitType('docs', '文档更新')
    
    // 或简单列表
    // commitTypes = ['feat', 'fix', 'docs', 'style', 'refactor', 'test', 'chore']
    
    // 自定义跳过模式（正则）
    skipPatterns = ['^wip[: ]', '^tmp[: ]']
    
    // 启用默认跳过模式（Merge、Revert 等）
    enableDefaultSkipPatterns = true
}
```

详细说明见 [GitHook.md](docs/GitHook.md)

### DocsPreview 配置

```groovy
gdxTasks {
    // 文档预览服务器的主类全限定名
    docsPreviewMainClass = 'com.example.docs.DocServer'
}
```

### PublishTemplate 配置

```groovy
gdxTasks {
    // 模板根目录（相对 rootProject）
    templatesRoot = 'Templates'
}
```

---

## 任务说明

### Versioning 任务组

| 任务 | 说明 |
|------|------|
| `syncVersions` | 从 `gradle.properties` 同步版本号到 BuildConfig.java、README.md、android/build.gradle |
| `bumpBuild` | 添加或递增 build 版本号（如 `0.10.9` → `0.10.9.0_build`）|
| `bumpPatch` | 升级修订号，自动去除 `_build` 后缀 |
| `bumpMinor` | 升级次版本号，自动去除 `_build` 后缀 |
| `bumpMajor` | 升级主版本号，自动去除 `_build` 后缀 |
| `printVersion` | 打印当前版本号 |
| `markBuildRelease` | 标记构建产物生成，递增 buildCount |

### AssetTasks 任务组

| 任务 | 说明 |
|------|------|
| `generateAssetList` | 扫描 assets 目录，生成 `assets.txt` 资源清单 |

### Changelog 任务组

| 任务 | 说明 |
|------|------|
| `generateChangelog` | 从 Git 历史生成 `changelog.json` |

### Docs 任务组

| 任务 | 说明 |
|------|------|
| `previewDocs` | 启动文档预览服务器 |
| `buildDocsDist` | 构建文档发布包 |
| `packageDocs` | 打包文档 |

### JPackage 任务组

| 任务 | 说明 |
|------|------|
| `jpackageExe` | 使用 jpackage 打包为 EXE（仅 Windows）|
| `packageSingleExe` | 使用 Enigma Virtual Box 打包为单文件 EXE |
| `oneClickRelease` | 一键构建 Jar → jpackage → 单文件 EXE |

### Template 任务组

| 任务 | 说明 |
|------|------|
| `publishTemplate` | 发布模板到指定模板列表（`-Ptarget=TemplateName`）|

---

## 依赖配置

### gradle.properties

```properties
projectPackage=com.example.mygame
projectVersion=0.1.0
jdkVersion=17
```

### JitPack 使用

```groovy
// settings.gradle
pluginManagement {
    repositories {
        maven { url "https://jitpack.io" }
    }
}

// build.gradle
plugins {
    id 'com.goldsprite.gdx-tasks' version '1.0.0'
}
```

### Composite Build（本地开发）

```groovy
// settings.gradle
pluginManagement {
    includeBuild('../GdxGradle')
}
```

---

## GdxGradle 自举

GdxGradle 插件本身也使用自己的功能管理版本：

- **版本文件**：`gradle.properties`（`projectVersion=1.0.0`）
- **构建流水号**：独立的 `build.properties`（与使用方项目隔离）
- **BuildConfig**：`src/main/java/com/goldsprite/gdxgradle/BuildConfig.java`

---

## 许可证

MIT License
