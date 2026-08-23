package com.moyu.reader.util

import org.junit.Assert.assertEquals
import org.junit.Test

class HashingTest {
    @Test fun sha256IsStableForDeduplication() {
        val first = Hashing.sha256("同一本书".byteInputStream())
        val second = Hashing.sha256("同一本书".byteInputStream())
        assertEquals(first, second)
        assertEquals(64, first.length)
    }
}

