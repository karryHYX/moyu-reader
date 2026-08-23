package com.moyu.reader

import android.app.Application
import com.moyu.reader.data.BookImportRepository
import com.moyu.reader.data.LibraryRepository
import com.moyu.reader.data.FontRepository
import com.moyu.reader.data.BackupRepository
import com.moyu.reader.data.db.MoyuDatabase
import com.moyu.reader.data.preferences.SettingsRepository
import com.moyu.reader.parser.CharsetDetector
import com.moyu.reader.parser.ChapterDetector
import com.moyu.reader.parser.EpubBookParser
import com.moyu.reader.parser.ParserRegistry
import com.moyu.reader.parser.TxtBookParser

class MoyuApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    val database = MoyuDatabase.create(application)
    val settings = SettingsRepository(application)
    val parsers = ParserRegistry(
        setOf(
            TxtBookParser(CharsetDetector(), ChapterDetector()),
            EpubBookParser(),
        )
    )
    val library = LibraryRepository(application, database, parsers)
    val importer = BookImportRepository(application, database, parsers, library)
    val fonts = FontRepository(application, settings)
    val backup = BackupRepository(application, database, settings, library)
}
