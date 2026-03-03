package com.goldsprite.gdxgradle

import groovy.json.JsonOutput
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.security.MessageDigest
import java.text.SimpleDateFormat

/**
 * 模板发布任务: publishTemplate
 * 用法: gradlew publishTemplate -Ptarget=BigDemo
 */
class PublishTemplateTasks {

    static final int CHUNK_SIZE = 18 * 1024 * 1024 // 18 MB

    static void register(Project project, GdxTasksExtension ext) {
        def group = ext.taskGroup.get()
        def rootProject = project.rootProject

        project.tasks.register('publishTemplate') {
            it.group = "${group}-publish"
            it.description = '打包、分卷并生成指定模板的清单 (-Ptarget=TemplateName)'

            it.doLast {
                if (!project.hasProperty('target')) {
                    throw new GradleException("请指定目标模板! 用法: -Ptarget=TemplateName")
                }
                String targetName = project.property('target')
                def templatesRoot = rootProject.file(ext.templatesRoot.get())
                File sourceDir = new File(templatesRoot, targetName)

                if (!sourceDir.exists()) {
                    throw new GradleException("模板目录不存在: ${sourceDir.absolutePath}")
                }

                def dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss")
                dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"))
                def buildID = dateFormat.format(new Date())

                println "📦 正在处理模板: ${targetName} (ID: ${buildID})"

                def buildTmp = rootProject.file("build/tmp_tpl_publish")
                buildTmp.deleteDir()
                buildTmp.mkdirs()

                def distRoot = rootProject.file("dist/templates")
                File targetDistDir = new File(distRoot, targetName)
                if (!targetDistDir.exists()) targetDistDir.mkdirs()

                // 清理旧文件
                targetDistDir.listFiles().each { f ->
                    if (f.isFile() && (f.name == "manifest.json" || f.name.startsWith("${targetName}."))) {
                        f.delete()
                    }
                }

                // 打包 Zip
                println "   -> 正在压缩..."
                File zipFile = new File(buildTmp, "${targetName}.zip")
                project.ant.zip(destfile: zipFile, basedir: sourceDir)

                def sizeMb = zipFile.length() / (1024.0 * 1024.0)
                println "   -> 压缩完成: ${String.format('%.2f', sizeMb)} MB"

                // 切分
                println "   -> 正在切分 (每卷 ${CHUNK_SIZE / 1024 / 1024}MB)..."
                def partsList = []
                def buffer = new byte[CHUNK_SIZE]
                int partIndex = 0

                zipFile.withInputStream { ins ->
                    while (true) {
                        int bytesRead = ins.read(buffer)
                        if (bytesRead <= 0) break

                        String partName = String.format("${targetName}.%s.p%d.zip", buildID, partIndex)
                        File partFile = new File(targetDistDir, partName)

                        partFile.withOutputStream { outs -> outs.write(buffer, 0, bytesRead) }

                        String md5 = PublishDocsTasks.generateMD5(partFile)

                        partsList << [
                            index: partIndex,
                            file: partName,
                            md5: md5,
                            size: partFile.length()
                        ]

                        def partSize = partFile.length() / (1024.0 * 1024.0)
                        println "      Generated: ${partName} (${String.format('%.2f', partSize)} MB)"
                        partIndex++
                    }
                }

                // 生成 Manifest
                def manifest = [
                    id: targetName,
                    name: "${targetName}.zip",
                    totalSize: zipFile.length(),
                    version: "1.0",
                    buildId: buildID,
                    updatedAt: new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("Asia/Shanghai")),
                    parts: partsList
                ]

                File jsonFile = new File(targetDistDir, "manifest.json")
                jsonFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(manifest))

                println "✅ 发布完成！产物位于: ${targetDistDir}"
                println "🚀 请将 dist/ 目录提交到 GitHub"
            }
        }
    }
}
