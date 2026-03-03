package com.goldsprite.gdxgradle

import groovy.json.JsonOutput
import org.gradle.api.Project

/**
 * Changelog 生成任务: generateChangelog
 * 从 Git Tag/Commit 生成 changelog.json
 */
class ChangelogTasks {

    static void register(Project project, GdxTasksExtension ext) {
        def group = ext.taskGroup.get()

        project.tasks.register('generateChangelog') {
            it.group = group
            it.description = '从 Git 历史生成 changelog.json'

            it.doLast {
                def MAX_VERSIONS = ext.maxVersions.get()
                def MAX_COMMITS_PER_VER = ext.maxCommitsPerVersion.get()
                def rootDir = project.rootProject.rootDir

                def jsonFile = project.rootProject.file("docs/engine_docs/changelog/changelog.json")
                jsonFile.parentFile.mkdirs()

                println "⏳ [Changelog] Reading Git Tags..."
                def tags = getGitOutput("git tag --sort=-version:refname", rootDir).readLines()

                if (tags.size() > MAX_VERSIONS) {
                    tags = tags.subList(0, MAX_VERSIONS)
                    println "⚠️ [Changelog] History limited to latest ${MAX_VERSIONS} versions."
                }

                def groupedData = [:] as LinkedHashMap

                // Unreleased
                if (!tags.isEmpty()) {
                    def unreleased = getCommitsBetween(tags[0], "HEAD", MAX_COMMITS_PER_VER, rootDir)
                    if (!unreleased.isEmpty()) {
                        def devKey = "In Development"
                        groupedData[devKey] = []
                        groupedData[devKey] << [
                            tag: "HEAD",
                            fullVersion: "HEAD",
                            date: new Date().format("yyyy-MM-dd"),
                            tagSummary: "正在开发中...",
                            tagDetails: "包含自上个版本以来的最新改动。",
                            isSnapshot: true,
                            commits: unreleased
                        ]
                    }
                }

                // Tag 列表
                for (int i = 0; i < tags.size(); i++) {
                    def currentTag = tags[i]
                    def prevTag = (i == tags.size() - 1) ? "" : tags[i + 1]

                    def rawTagMsg = getGitOutput("git for-each-ref refs/tags/${currentTag} --format='%(contents)'", rootDir)
                    def tagSummary = ""
                    def tagDetails = ""

                    if (rawTagMsg && !rawTagMsg.trim().isEmpty()) {
                        def lines = rawTagMsg.readLines()
                        if (!lines.isEmpty()) {
                            tagSummary = lines[0].trim().replaceAll(/^['"]|['"]$/, "")
                            if (lines.size() > 1) {
                                tagDetails = lines.subList(1, lines.size()).join("\n").trim().replaceAll(/^['"]|['"]$/, "")
                            }
                        }
                    } else {
                        tagSummary = "Release ${currentTag}"
                    }

                    def cleanTag = currentTag.replace("v", "")
                    def mainVersion = cleanTag
                    def matcher = (cleanTag =~ /^(\d+\.\d+\.\d+)/)
                    if (matcher.find()) {
                        mainVersion = matcher.group(1)
                    }

                    if (!groupedData.containsKey(mainVersion)) {
                        groupedData[mainVersion] = []
                    }

                    println "⏳ Processing ${currentTag} -> Group: ${mainVersion}"

                    def tagDate = getGitOutput("git log -1 --format=%ad --date=short ${currentTag}", rootDir)
                    def commits = getCommitsBetween(prevTag, currentTag, MAX_COMMITS_PER_VER, rootDir)

                    groupedData[mainVersion] << [
                        tag: currentTag,
                        fullVersion: cleanTag,
                        date: tagDate,
                        tagSummary: tagSummary,
                        tagDetails: tagDetails,
                        isSnapshot: false,
                        commits: commits
                    ]
                }

                // 构建 JSON
                def finalGroups = []
                groupedData.each { key, patches ->
                    finalGroups << [id: key, patches: patches]
                }

                def finalJson = [
                    lastUpdated: new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("Asia/Shanghai")),
                    groups: finalGroups
                ]

                def jsonString = JsonOutput.prettyPrint(JsonOutput.toJson(finalJson))
                jsonFile.write(jsonString, "UTF-8")

                println "✅ Changelog Generated: ${finalGroups.size()} groups."
            }
        }
    }

    // --- 辅助方法 ---

    private static List getCommitsBetween(String fromRef, String toRef, int maxCount, File workDir) {
        def range = fromRef.isEmpty() ? toRef : "${fromRef}..${toRef}"
        def DELIMITER = "__GD_SEP__"

        def rawLog = getGitOutput("git log ${range} -n ${maxCount} --pretty=format:%H${DELIMITER}%ad${DELIMITER}%an${DELIMITER}%B[END_C] --date=short", workDir)

        def commits = []
        if (rawLog == null || rawLog.trim().isEmpty()) return commits

        rawLog.split("\\[END_C\\]").each { rawEntry ->
            if (rawEntry.trim().isEmpty()) return

            def parts = rawEntry.split(DELIMITER, 4)
            if (parts.length < 4) return

            def hash = parts[0].trim()
            def date = parts[1].trim()
            def fullMessage = parts[3].trim()

            def matcher = (fullMessage =~ /^(feat|fix|perf|docs|refactor|chore|test)(\((.+)\))?:\s*(.+)/)

            if (matcher.find()) {
                def type = matcher.group(1)
                def summary = matcher.group(4)
                def lines = fullMessage.readLines()
                def details = (lines.size() > 1) ? lines.subList(1, lines.size()).join("\n").trim() : ""

                commits << [hash: hash, date: date, type: type, summary: summary, details: details]
            } else {
                def lines = fullMessage.readLines()
                def summary = lines.isEmpty() ? "No Description" : lines[0].trim()
                if (!summary.startsWith("Merge branch") && !summary.startsWith("Merge remote-tracking")) {
                    def details = (lines.size() > 1) ? lines.subList(1, lines.size()).join("\n").trim() : ""
                    commits << [hash: hash, date: date, type: "legacy", summary: summary, details: details]
                }
            }
        }
        return commits
    }

    private static String getGitOutput(String command, File workDir) {
        try {
            def proc = command.execute(null, workDir)
            def out = new StringBuilder()
            def err = new StringBuilder()
            proc.consumeProcessOutput(out, err)
            proc.waitFor()
            if (out.length() > 0) return out.toString().trim()
            return ""
        } catch (Exception ignored) {
            return ""
        }
    }
}
