package com.moyu.reader.util

import java.io.ByteArrayOutputStream
import java.io.InputStream

fun InputStream.readUpTo(limit: Int): ByteArray {
    require(limit >= 0)
    val output = ByteArrayOutputStream(limit.coerceAtMost(64 * 1024))
    val buffer = ByteArray(16 * 1024)
    var remaining = limit
    while (remaining > 0) {
        val count = read(buffer, 0, buffer.size.coerceAtMost(remaining))
        if (count < 0) break
        output.write(buffer, 0, count)
        remaining -= count
    }
    return output.toByteArray()
}

