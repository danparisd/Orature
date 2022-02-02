package org.wycliffeassociates.otter.jvm.workbookapp.ui.model

import org.wycliffeassociates.otter.common.data.primitives.Language

data class RemoteLanguage(val url: String, val language: Language, val resourceSlug: String = "ulb")