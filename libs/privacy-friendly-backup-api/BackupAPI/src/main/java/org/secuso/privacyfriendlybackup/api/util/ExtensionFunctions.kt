package org.secuso.privacyfriendlybackup.api.util

import android.content.pm.Signature
import android.util.Base64
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import java.io.File
import java.io.InputStream
import java.security.MessageDigest


fun InputStream.readString() : String {
    return this.use {
        val buffer = it.readBytes()
        return@use String(buffer, Charsets.UTF_8)
    }
}

fun Signature.toBase64() : String {
    val md = MessageDigest.getInstance("SHA")
    md.update(this.toByteArray())
    return Base64.encodeToString(md.digest(), Base64.DEFAULT)
}

fun ByteArray.toBase64() : String {
    return Base64.encodeToString(this, Base64.DEFAULT)
}

fun String.fromBase64() : ByteArray {
    return Base64.decode(this, Base64.DEFAULT)
}

fun Signature.toHex() : String {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(this.toByteArray())
    return md.digest().toHex()
}

fun ByteArray.toHex() : String {
    val HEX_CHARS = "0123456789ABCDEF"
    val result = StringBuilder(this.size * 2)
    this.forEach {
        val i = it.toInt()
        result.append(HEX_CHARS[i shr 4 and 0x0f])
        result.append(HEX_CHARS[i and 0x0f])
        result.append(':')
    }
    if(result.isNotEmpty()) {
        result.deleteCharAt(result.lastIndex)
    }
    return result.toString()
}

fun ByteArray.hash(algorithm: String) : ByteArray {
    val md = MessageDigest.getInstance(algorithm)
    md.update(this)
    return md.digest()
}

fun InputStream.toFile(path: String) {
    File(path).outputStream().use { this.copyTo(it) }
}

fun File.copyInputStreamToFile(inputStream: InputStream) {
    this.outputStream().use { fileOut ->
        inputStream.copyTo(fileOut)
    }
}

fun <T, K, R> MediatorLiveData<R>.addSources(
    liveData1: LiveData<T>,
    liveData2: LiveData<K>,
    block: (T?, K?) -> R
): MediatorLiveData<R> {
    this.addSource(liveData1) {
        this.value = block.invoke(liveData1.value, liveData2.value)
    }
    this.addSource(liveData2) {
        this.value = block.invoke(liveData1.value, liveData2.value)
    }
    return this
}