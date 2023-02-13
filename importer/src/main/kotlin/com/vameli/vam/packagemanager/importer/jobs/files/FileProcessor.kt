package com.vameli.vam.packagemanager.importer.jobs.files

import java.nio.file.Path

interface FileProcessor {
    fun processFile(path: Path)
}


