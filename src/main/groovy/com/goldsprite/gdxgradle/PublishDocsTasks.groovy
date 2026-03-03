package com.goldsprite.gdxgradle

import groovy.json.JsonOutput
import org.gradle.api.GradleException
import org.gradle.api.Project
import java.security.MessageDigest
import java.text.SimpleDateFormat

/**
 * 文档发布任务: packageDocs, buildDocsDist
 * 将 docs/engine_docs 打包、分卷、生成清单 JSON
 */
class PublishDocsTasks {

    static final int CHUNK_SIZE = 18 * 1024 * 1024 // 18 MB

    static void register(Project project, GdxTasksExtension ext) {
        def group = ext.taskGroup.get()
        def rootProject = project.rootProject

        def docsRoot = rootProject.file("docs")
        def buildTmp = rootProject.file("build/tmp_publish")
        def distDir = rootProject.file("dist")

        project.tasks.register('packageDocs', org.gradle.api.tasks.bundling.Zip) {
            it.group = "${group}-publish"
            it.from docsRoot
            it.include 'engine_docs/**'
            it.archiveFileName = 'docs_full.zip'
            it.destinationDirectory = buildTmp
            it.doFirst { buildTmp.mkdirs() }
        }

        project.tasks.register('buildDocsDist') {
            it.group = "${group}-publish"
            it.dependsOn 'packageDocs'
            it.outputs.upToDateWhen { false }

            it.doLast {
                def sourceZip = rootProject.file("${buildTmp}/docs_full.zip")
                if (!sourceZip.exists()) throw new GradleException("Source zip missing!")

                def dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss")
                dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"))
                def buildID = dateFormat.format(new Date())

                println "🚀 Starting Docs Build. ID: ${buildID}"

                if (!distDir.exists()) distDir.mkdirs()
                distDir.listFiles().each { f ->
                    if (f.isFile() && (f.name == "docs_manifest.json" || f.name.startsWith("docs."))) {
                        f.delete()
                    }
                }

                def partsList = []
                def buffer = new byte[CHUNK_SIZE]
                int partIndex = 0

                sourceZip.withInputStream { ins ->
                    while (true) {
                        int bytesRead = ins.read(buffer)
                        if (bytesRead <= 0) break

                        File tempPart = new File(distDir, "temp_part_${partIndex}")
                        tempPart.withOutputStream { it.write(buffer, 0, bytesRead) }
                        String md5 = generateMD5(tempPart)

                        String finalName = String.format("docs.%s.p%d.zip", buildID, partIndex)
                        File finalFile = new File(distDir, finalName)
                        tempPart.renameTo(finalFile)

                        partsList << [
                            index: partIndex,
                            file: finalName,
                            md5: md5,
                            size: finalFile.length()
                        ]

                        println "   -> Generated: ${finalName} (Size: ${finalFile.length()})"
                        partIndex++
                    }
                }

                def projectVersion = rootProject.findProperty('projectVersion') ?: '0.0.0'
                def manifest = [
                    name: "engine_docs.zip",
                    totalSize: sourceZip.length(),
                    version: projectVersion,
                    buildId: buildID,
                    updatedAt: new Date().format("yyyy-MM-dd HH:mm:ss", TimeZone.getTimeZone("Asia/Shanghai")),
                    parts: partsList
                ]

                File jsonFile = new File(distDir, "docs_manifest.json")
                jsonFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(manifest))

                println "✅ Docs Build Complete. Manifest generated at dist/docs_manifest.json"
            }
        }
    }

    static String generateMD5(File file) {
        MessageDigest digest = MessageDigest.getInstance("MD5")
        file.withInputStream { is ->
            byte[] buffer = new byte[8192]
            int read
            while ((read = is.read(buffer)) > 0) digest.update(buffer, 0, read)
        }
        return digest.digest().encodeHex().toString()
    }
}
