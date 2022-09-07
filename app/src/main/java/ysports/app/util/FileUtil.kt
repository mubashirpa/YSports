package ysports.app.util

import java.io.File

class FileUtil {

    fun deleteDir(dir: File?): Boolean {
        return dir?.deleteRecursively() ?: false
        /*
        if (dir != null) {
            return if (dir.isDirectory) {
                val children = dir.list()
                if (children != null) {
                    for (child in children) {
                        deleteDir(File(dir, child))
                    }
                }
                dir.delete()
            } else if (dir.isFile)
                dir.delete()
            else
                false
        }
        */
    }

    fun getDirSize(dir: File?) : Long {
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