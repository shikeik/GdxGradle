# GitHook - Git 提交规范校验

自动安装 `commit-msg` 钩子，确保团队成员遵循统一的提交格式。

---

## 功能特性

- ✅ **自动安装** - Gradle 配置阶段自动写入钩子，无需手动操作
- ✅ **智能跳过** - Merge、Revert 等 Git 自动生成提交自动放行
- ✅ **灵活配置** - 支持简单列表或详细类型定义（含描述）
- ✅ **自定义规则** - 可添加自定义跳过模式和提交类型
- ✅ **安全兼容** - Composite Build 子构建自动跳过，避免越权

---

## 快速开始

### 1. 默认配置（零配置即可用）

```groovy
// build.gradle
gdxTasks {
    // 默认已启用，支持自动跳过 Merge/Revert
}
```

默认允许的提交类型：`feat`, `fix`, `perf`, `docs`, `refactor`, `chore`, `test`, `assets`

### 2. 验证安装

```bash
./gradlew --console=plain
```

看到输出 `✅ Git Commit Hook (commit-msg) 已更新。` 即安装成功。

---

## 配置方式

### 方式一：简单字符串列表（向后兼容）

适合只需要定义类型标识的场景：

```groovy
gdxTasks {
    commitTypes = ['feat', 'fix', 'docs', 'refactor', 'chore']
}
```

### 方式二：详细类型定义（推荐）

适合需要自定义类型描述的场景：

```groovy
gdxTasks {
    useDetailedCommitTypes = true
    
    commitType('feat', '新功能')
    commitType('fix', '修复问题')
    commitType('perf', '性能优化')
    commitType('docs', '文档更新')
    commitType('refactor', '代码重构')
    commitType('chore', '杂项/构建')
    commitType('test', '测试相关')
    commitType('assets', '资源文件')
}
```

或使用批量添加：

```groovy
gdxTasks {
    useDetailedCommitTypes = true
    commitTypes(['feat:新功能', 'fix:修复问题', 'perf:性能优化'])
}
```

---

## 提交格式要求

### 标准格式

```
<type>: <summary>

[optional body]

[optional footer]
```

### 示例

```bash
# ✅ 正确
feat: 增加Web浏览器组件
fix: 修复碰撞检测空指针异常
docs(api): 更新网络模块文档

# ❌ 错误（会被拦截）
增加了新功能
修复bug
更新文档
```

---

## 跳过模式配置

### 默认自动跳过的提交

| 模式 | 说明 |
|------|------|
| `^Merge ` | Merge branch 'xxx' into 'yyy' |
| `^Revert "` | Revert "xxx"（双引号） |
| `^Revert '` | Revert 'xxx'（单引号） |
| `^fixup!` | git commit --fixup |
| `^squash!` | git commit --squash |
| `^amend!` | git commit --amend |
| `^WIP[: ]` | Work In Progress |
| `^Auto-merged ` | IDE 自动合并 |
| `^Branch: ` | 某些 Git 工作流 |

### 添加自定义跳过模式

```groovy
gdxTasks {
    // 自定义额外的跳过模式（正则表达式）
    skipPatterns = ['^wip[: ]', '^temp[: ]', '^draft[: ]']
}
```

### 禁用默认跳过模式

```groovy
gdxTasks {
    // 警告：禁用后 Merge 提交会被拦截！
    enableDefaultSkipPatterns = false
}
```

---

## 完整配置示例

```groovy
gdxTasks {
    taskGroup = 'sandtank'
    
    // ============================================================
    // Git Hook 配置
    // ============================================================
    
    // 是否启用自动安装（默认 true）
    installGitHook = true
    
    // 方式1：简单字符串列表
    // commitTypes = ['feat', 'fix', 'docs']
    
    // 方式2：详细类型定义（推荐）
    useDetailedCommitTypes = true
    commitType('feat', '新功能')
    commitType('fix', '修复问题')
    commitType('docs', '文档更新')
    
    // 自定义跳过模式
    skipPatterns = ['^wip[: ]']
}
```

---

## 禁用 GitHook

如需完全禁用：

```groovy
gdxTasks {
    installGitHook = false
}
```

---

## 常见问题

### Q: Merge 提交被拦截怎么办？

**A:** 确保 `enableDefaultSkipPatterns = true`（默认已启用）。如果自定义了 `skipPatterns`，确保没有覆盖默认行为。

### Q: 如何临时绕过校验？

**A:** 使用 `--no-verify` 选项：

```bash
git commit -m "临时提交" --no-verify
```

### Q: 钩子没有自动更新？

**A:** 检查以下几点：
1. 确保是 Git 仓库（存在 `.git/hooks` 目录）
2. 确保 `installGitHook = true`
3. 对于 Composite Build 子构建，钩子不会自动安装（安全设计）

### Q: 如何手动更新钩子？

**A:** 运行任意 Gradle 任务触发配置阶段：

```bash
./gradlew help
```

---

## 技术细节

### 钩子文件位置

```
.git/hooks/commit-msg
```

### 更新时机

- Gradle 配置阶段自动更新
- 每次运行 Gradle 任务时检查

### 安全性

- Composite Build 子构建自动跳过（避免越权修改宿主项目）
- 非 Git 仓库环境静默忽略
