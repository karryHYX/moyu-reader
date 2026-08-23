package com.moyu.reader.data.db

import com.moyu.reader.model.Book
import com.moyu.reader.model.BookFormat
import com.moyu.reader.model.Chapter

fun BookEntity.asExternalModel() = Book(
    id = id,
    title = title,
    author = author,
    format = BookFormat.valueOf(format),
    originalFileName = originalFileName,
    fileSize = fileSize,
    charset = charset,
    addedAt = addedAt,
    lastReadAt = lastReadAt,
    totalCharacters = totalCharacters,
    chapterCount = chapterCount,
    readingProgress = readingProgress,
    currentChapter = currentChapter,
    currentPosition = currentPosition,
    coverPath = coverPath,
    favorite = favorite,
    searchIndexed = searchIndexed,
)

fun ChapterEntity.asExternalModel() = Chapter(
    id = id,
    bookId = bookId,
    index = index,
    title = title,
    locator = locator,
    byteOffset = byteOffset,
    byteLength = byteLength,
    characterCount = characterCount,
    synthetic = synthetic,
)
