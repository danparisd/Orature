package org.wycliffeassociates.otter.jvm.workbookapp.ui.model

data class RemoteSourceAudio(
    val slug: String,
    val title: String,
    val url: String,
    val category: SourceAudioCategory
)

enum class SourceAudioCategory {
    LANGUAGE,
    RESOURCE,
    BOOK,
    CHAPTER;

    companion object {
        fun get(category: String) = values().find { it.name == category.uppercase() }
    }
}