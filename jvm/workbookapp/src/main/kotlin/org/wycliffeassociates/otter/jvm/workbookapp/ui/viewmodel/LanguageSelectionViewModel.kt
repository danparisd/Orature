/**
 * Copyright (C) 2020-2022 Wycliffe Associates
 *
 * This file is part of Orature.
 *
 * Orature is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Orature is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Orature.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.wycliffeassociates.otter.jvm.workbookapp.ui.viewmodel

import com.github.thomasnield.rxkotlinfx.observeOnFx
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.scene.control.CustomMenuItem
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import okhttp3.OkHttpClient
import okhttp3.Request
import org.wycliffeassociates.otter.common.data.primitives.Language
import org.wycliffeassociates.otter.common.domain.resourcecontainer.ImportResourceContainer
import org.wycliffeassociates.otter.jvm.controls.button.CheckboxButton
import org.wycliffeassociates.otter.jvm.workbookapp.di.IDependencyGraphProvider
import org.wycliffeassociates.otter.jvm.workbookapp.ui.model.RemoteLanguage
import tornadofx.*
import java.io.File
import java.util.function.Predicate
import javax.inject.Inject
import javax.inject.Provider

class LanguageSelectionViewModel(items: ObservableList<Language>) : ViewModel() {
    @Inject
    lateinit var importRcProvider: Provider<ImportResourceContainer>

    private val translationViewModel: TranslationViewModel by inject()

    val searchQueryProperty = SimpleStringProperty("")
    val regions = observableListOf<String>()

    val selectedRegions = observableListOf<String>()
    val menuItems = observableListOf<MenuItem>()

    val anglicizedProperty = SimpleBooleanProperty(false)
    val filteredLanguages = FilteredList(items)
    val remoteLanguages = observableListOf<RemoteLanguage>()
    val importInProgress = SimpleBooleanProperty(false)

    private var regionPredicate = Predicate<Language> { true }
    private var queryPredicate = Predicate<Language> { true }

    init {
        (app as IDependencyGraphProvider).dependencyGraph.inject(this)

        selectedRegions.onChange {
            regionPredicate = if (it.list.isEmpty()) {
                Predicate { true }
            } else {
                Predicate { language -> selectedRegions.contains(language.region) }
            }
            filteredLanguages.predicate = regionPredicate.and(queryPredicate)
        }

        searchQueryProperty.onChange { query ->
            queryPredicate = if (query.isNullOrBlank()) {
                Predicate { true }
            } else {
                Predicate { language ->
                    language.slug.contains(query, true)
                        .or(language.name.contains(query, true))
                        .or(language.anglicizedName.contains(query, true))
                }
            }
            filteredLanguages.predicate = queryPredicate.and(regionPredicate)
        }
    }

    fun resetFilter() {
        regionPredicate = Predicate { true }
        queryPredicate = Predicate { true }
        searchQueryProperty.set("")
        regions.clear()
        selectedRegions.clear()
        anglicizedProperty.set(false)
    }

    fun setFilterMenu() {
        val items = mutableListOf<MenuItem>()
        items.add(createMenuSeparator(messages["region"]))
        items.addAll(
            regions.map {
                val title = it.ifBlank { messages["unknown"] }
                createMenuItem(title, true) { selected ->
                    when (selected) {
                        true -> selectedRegions.add(it)
                        else -> selectedRegions.remove(it)
                    }
                }
            }
        )
        items.add(createMenuSeparator(messages["display"]))
        items.add(
            createMenuItem(messages["anglicized"], false) { selected ->
                anglicizedProperty.set(selected)
            }
        )
        menuItems.setAll(items)
    }

    fun importLanguage(remoteLanguage: RemoteLanguage): Completable {
        importInProgress.set(true)
        return downloadRC(remoteLanguage.url)
            .doOnSuccess { file ->
                // import
                println("RC downloaded for ${remoteLanguage.language.slug} saved as $file \nImporting...")
                importRcProvider.get()
                    .import(file)
                    .subscribeOn(Schedulers.io())
                    .observeOnFx()
                    .doOnError { e ->
                        println("Error in importing resource container $file")
                        importInProgress.set(false)
                        remoteLanguages.clear()
                    }
                    .doFinally {
                        println("Deleted: " + file.deleteRecursively())
                    }
                    .subscribe { result ->
                        println("Import status: ${result.name}")
                        translationViewModel.reset()
                        translationViewModel.loadSourceLanguages()
                        remoteLanguages.remove(remoteLanguage)
                        importInProgress.set(false)
                    }
            }
            .doOnError {
                Platform.runLater {
                    importInProgress.set(false)
                    remoteLanguages.clear()
                }
            }
            .ignoreElement()
    }

    private fun createMenuSeparator(label: String): MenuItem {
        return CustomMenuItem().apply {
            styleClass.add("wa-menu-button__separator")
            content = Label(label)
            isHideOnClick = false
        }
    }

    private fun createMenuItem(
        label: String,
        preSelected: Boolean,
        onChecked: (Boolean) -> Unit
    ): MenuItem {
        return CustomMenuItem().apply {
            content = CheckboxButton().apply {
                text = label
                selectedProperty().onChange {
                    onChecked(it)
                }
                isSelected = preSelected
            }
            isHideOnClick = false
        }
    }

    fun fetchLanguages(): Completable {
        return Completable
            .fromAction {
                val languages = languagesFromAPI()
                Platform.runLater {
                    remoteLanguages.setAll(languages)
                    remoteLanguages.sortBy { it.language.slug }
                }
            }.subscribeOn(Schedulers.io())
            .doOnError {
                println("Network error while fetching languages!")
                importInProgress.set(false)
                remoteLanguages.clear()
            }
            .observeOnFx()
    }

    private fun downloadRC(url: String): Single<File> {
        return Single
            .fromCallable {
                val request = Request.Builder().url(url).build()
                val response = OkHttpClient().newCall(request).execute()
                response.body()?.let { body ->
                    val file = kotlin.io.path.createTempFile("rc-to-import", ".zip").toFile()
                    file.outputStream().buffered().use { out ->
                        out.write(body.byteStream().buffered().readBytes())
                    }
                    file
                }
            }
            .doOnError {
                println("Error downloading $url - ${it.message}")
            }
            .subscribeOn(Schedulers.io())
            .observeOnFx()
    }

    private fun languagesFromAPI(): List<RemoteLanguage> {
        return listOf(
            RemoteLanguage(
                "https://content.bibletranslationtools.org/WA-Catalog/hi_ulb/archive/master.zip",
                Language(
                    "hi",
                    "हिन्दी, हिंदी",
                    "Hindi",
                    "ltr",
                    true,
                    "AS"
                )
            ),
            RemoteLanguage(
                "https://content.bibletranslationtools.org/WA-Catalog/vi_ulb/archive/master.zip",
                Language(
                    "vi",
                    "Vietnamese",
                    "Tiếng Việt",
                    "ltr",
                    true,
                    "AS"
                )
            ),
            RemoteLanguage(
                "https://content.bibletranslationtools.org/WA-Catalog/th_ulb/archive/master.zip",
                Language(
                    "th",
                    "ไทย",
                    "Thai",
                    "ltr",
                    true,
                    "AS"
                )
            )
        )
    }
}
