package org.secuso.privacyfriendlybackup.api.backup

import android.util.JsonReader
import android.util.JsonWriter
import org.secuso.privacyfriendlybackup.api.util.copyInputStreamToFile
import org.secuso.privacyfriendlybackup.api.util.fromBase64
import org.secuso.privacyfriendlybackup.api.util.toBase64
import java.io.File
import java.lang.RuntimeException

/**
 * @author Christopher Beckmann (Kamuno)
 */
object FileUtil {

    @JvmStatic
    fun writePath(writer: JsonWriter, path: File, recursive : Boolean = false) {
        writer.beginArray()
        val files = path.listFiles() ?: emptyArray()

        for(file in files) {
            if(!file.isDirectory || recursive) {
                writeFile(writer, file, recursive)
            }
        }

        writer.endArray()
    }

    @JvmStatic
    fun writeFile(writer: JsonWriter, file: File, recursive : Boolean = false) {
        writer.beginObject()
        writer.name("name").value(file.name)
        writer.name("path").value(file.path)
        writer.name("type")

        if(file.isFile) {
            writer.value("file")
            writer.name("content")
            file.inputStream().use {
                writer.value(it.readBytes().toBase64())
            }
        } else if(file.isDirectory) {
            writer.value("directory")
            writer.name("children")
            writePath(writer, file, recursive)
        }

        writer.endObject()
    }

    @JvmStatic
    fun readFile(reader: JsonReader, path: File) {
        reader.beginObject()

        var file : File = path
        var type : String? = null
        var content : String? = null

        while(reader.hasNext()) {
            val name = reader.nextName()

            // "name" must come before "children" for this to work
            when(name) {
                "name" -> file = File(path, reader.nextString())
                "path" -> reader.skipValue()
                "type" -> type = reader.nextString()
                "children" -> readPath(reader, file)
                "content" -> content = reader.nextString()
                else -> throw RuntimeException("Can not parse name $name")
            }
        }

        if(type == "file") {
            content?.fromBase64()?.inputStream()?.let {
                if(file != path) {
                    path.mkdirs()
                    file.copyInputStreamToFile(it)
                }
            }
        }

        reader.endObject()
    }

    @JvmStatic
    fun readPath(reader: JsonReader, path: File) {
        reader.beginArray()

        while(reader.hasNext()) {
            readFile(reader, path)
        }

        reader.endArray()
    }

    @JvmStatic
    fun copyFile(inputFile: File, outputFile: File) {
        inputFile.inputStream().use {
            outputFile.copyInputStreamToFile(it)
        }
    }

}