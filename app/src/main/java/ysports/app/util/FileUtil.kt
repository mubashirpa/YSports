package ysports.app.util

import java.io.File

class FileUtil {

    fun deleteDir(dir: File?): Boolean {
        return dir?.deleteRecursively() ?: false
    }

    fun getDirSize(dir: File?): Long {
        var size: Long = 0
        for (file in dir?.listFiles()!!) {
            if (file != null) {
                if (file.isDirectory) {
                    size += getDirSize(file)
                } else if (file.isFile) {
                    size += file.length()
                }
            }
        }
        return size
    }
}