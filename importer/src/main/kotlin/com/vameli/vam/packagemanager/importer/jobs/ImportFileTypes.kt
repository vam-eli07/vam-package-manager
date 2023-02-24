package com.vameli.vam.packagemanager.importer.jobs

import kotlin.io.path.extension

enum class ImportFileType(val extension: ImportFileExtension) {
    VAM(ImportFileExtension.VAM),
    VAP(ImportFileExtension.VAP),
    VAR(ImportFileExtension.VAR),
    JSON(ImportFileExtension.JSON),
    ;

    companion object {
        private val cache = values().associateBy(ImportFileType::extension)

        fun fromExtension(extension: ImportFileExtension): ImportFileType? {
            return cache[extension]
        }
    }
}

enum class ImportFileExtension(val extension: String, val isBinaryType: Boolean, val description: String) {
    VAM("vam", false, "Item descriptor"),
    VAP("vap", false, "Preset file"),
    VMI("vmi", false, "Morph file"),
    VAR("var", true, "Package of multiple resources in ZIP format"),
    JSON("json", false, "Generic JSON file, used for multiple item types (scenes)"),
    ;

    val isTextType: Boolean = !isBinaryType

    companion object {
        private val cache = values().associateBy { it.extension.lowercase() }

        fun fromExtension(extension: String): ImportFileExtension? {
            return cache[extension.lowercase()]
        }

        fun FileToImport.getExtension(): ImportFileExtension? = fromExtension(path.extension)

        fun FileToImport.hasExtension(extension: ImportFileExtension): Boolean =
            path.extension.lowercase() == extension.extension
    }
}
