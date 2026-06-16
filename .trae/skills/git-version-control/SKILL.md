---
name: "git-version-control"
description: "Git 版本控制、分支管理、提交规范、合并冲突解决。当用户需要执行 git 操作、创建分支、合并代码、解决冲突、查看历史、标签管理时使用此技能。"
---

# Git Version Control

Git 版本控制与分支管理专家技能。

## 核心功能

- 代码版本管理
- 分支创建与切换
- 代码合并与冲突解决
- 提交历史查看
- 标签管理
- 远程仓库同步

## 基础配置

### 初始化配置

```bash
# 配置用户信息
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# 配置默认分支
git config --global init.defaultBranch main

# 配置编辑器
git config --global core.editor "code --wait"

# 配置换行符（Windows）
git config --global core.autocrlf true
```

## 日常操作

### 基本工作流

```bash
# 查看状态
git status

# 添加文件到暂存区
git add <file>
git add .  # 添加所有文件

# 提交更改
git commit -m "feat: 添加新功能"

# 查看提交历史
git log --oneline --graph --all

# 查看差异
git diff
git diff --staged
```

### 分支管理

```bash
# 查看所有分支
git branch -a

# 创建新分支
git branch feature/new-feature

# 切换分支
git checkout feature/new-feature
# 或
git switch feature/new-feature

# 创建并切换分支
git checkout -b feature/new-feature
# 或
git switch -c feature/new-feature

# 删除分支
git branch -d feature/new-feature

# 重命名当前分支
git branch -m new-name
```

### 合并与变基

```bash
# 合并分支
git checkout main
git merge feature/new-feature

# 变基（保持线性历史）
git checkout feature/new-feature
git rebase main

# 交互式变基
git rebase -i HEAD~3

# 中止合并
git merge --abort

# 中止变基
git rebase --abort
```

## 高级操作

### 暂存工作

```bash
# 暂存当前工作
git stash

# 暂存并添加消息
git stash save "working on feature"

# 查看暂存列表
git stash list

# 恢复暂存
git stash pop
git stash apply stash@{0}

# 删除暂存
git stash drop stash@{0}
```

### 撤销更改

```bash
# 撤销工作区更改
git checkout -- <file>
# 或
git restore <file>

# 撤销暂存区
git restore --staged <file>

# 撤销提交（保留更改）
git reset --soft HEAD~1

# 撤销提交（保留更改到工作区）
git reset --mixed HEAD~1

# 完全撤销（危险操作）
git reset --hard HEAD~1
```

### 标签管理

```bash
# 查看标签
git tag

# 创建轻量标签
git tag v1.0.0

# 创建附注标签
git tag -a v1.0.0 -m "Release version 1.0.0"

# 删除标签
git tag -d v1.0.0

# 推送标签到远程
git push origin v1.0.0
git push origin --tags

# 检出标签
git checkout v1.0.0
```

## 远程仓库

### 远程操作

```bash
# 查看远程仓库
git remote -v

# 添加远程仓库
git remote add origin https://github.com/user/repo.git

# 拉取远程更改
git fetch origin
git pull origin main

# 推送更改
git push origin main
git push -u origin main  # 首次推送并设置上游

# 删除远程分支
git push origin --delete feature/old-feature
```

### 同步 Fork

```bash
# 添加上游仓库
git remote add upstream https://github.com/original/repo.git

# 同步上游更改
git fetch upstream
git checkout main
git merge upstream/main
```

## 冲突解决

### 合并冲突

1. 识别冲突文件：`git status`
2. 打开文件，查找冲突标记：`<<<<<<<`, `=======`, `>>>>>>>`
3. 手动编辑解决冲突
4. 添加解决后的文件：`git add <file>`
5. 完成合并：`git commit`

### 使用工具解决冲突

```bash
# 使用 VS Code 解决冲突
git mergetool --tool=vscode

# 使用其他工具
git mergetool --tool=intellij
```

## 提交规范

### Conventional Commits

```
<type>(<scope>): <subject>

<body>

<footer>
```

**类型（type）：**
- `feat`: 新功能
- `fix`: 修复 bug
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具变动

**示例：**
```
feat(auth): 添加 JWT 认证支持

- 实现 JWT token 生成和验证
- 添加认证拦截器
- 更新安全配置

Closes #123
```

## 最佳实践

1. **频繁提交**：每个逻辑更改单独提交
2. **清晰的提交信息**：描述"为什么"而不是"什么"
3. **小步提交**：避免大型提交
4. **使用分支**：功能开发、bug 修复使用独立分支
5. **定期同步**：经常从主分支拉取最新代码
6. **代码审查**：使用 Pull Request 进行代码审查
7. **保护主分支**：main/master 分支应该受到保护

## 常见问题

### 提交错误

```bash
# 修改最后一次提交信息
git commit --amend -m "New commit message"

# 添加遗漏的文件到上次提交
git add forgotten-file
git commit --amend --no-edit
```

### 恢复误删分支

```bash
# 找回删除的分支
git reflog
git checkout -b recovered-branch HEAD@{n}
```

### 清理仓库

```bash
# 清理未跟踪的文件
git clean -fd

# 清理未跟踪的文件和目录
git clean -fd

# 预览将清理的文件（干运行）
git clean -fdn
```
