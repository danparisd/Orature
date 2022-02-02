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
package org.wycliffeassociates.otter.jvm.workbookapp.ui.screens.translation

import com.github.thomasnield.rxkotlinfx.observeOnFx
import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ListView
import org.kordamp.ikonli.javafx.FontIcon
import org.kordamp.ikonli.material.Material
import org.wycliffeassociates.otter.jvm.controls.bar.FilteredSearchBar
import org.wycliffeassociates.otter.jvm.controls.breadcrumbs.BreadCrumb
import org.wycliffeassociates.otter.jvm.controls.card.LanguageCardCell
import org.wycliffeassociates.otter.jvm.controls.dialog.confirmdialog
import org.wycliffeassociates.otter.jvm.controls.styles.tryImportStylesheet
import org.wycliffeassociates.otter.jvm.workbookapp.ui.NavigationMediator
import org.wycliffeassociates.otter.jvm.workbookapp.ui.components.LanguageCell
import org.wycliffeassociates.otter.jvm.workbookapp.ui.components.LanguageType
import org.wycliffeassociates.otter.jvm.workbookapp.ui.model.RemoteLanguage
import org.wycliffeassociates.otter.jvm.workbookapp.ui.viewmodel.LanguageSelectionViewModel
import org.wycliffeassociates.otter.jvm.workbookapp.ui.viewmodel.SettingsViewModel
import org.wycliffeassociates.otter.jvm.workbookapp.ui.viewmodel.TranslationViewModel
import tornadofx.*

class SourceLanguageSelection : Fragment() {
    private val translationViewModel: TranslationViewModel by inject()
    private val settingsViewModel: SettingsViewModel by inject()
    private val viewModel = LanguageSelectionViewModel(translationViewModel.sourceLanguages)
    private val navigator: NavigationMediator by inject()

    private lateinit var remoteLanguageLV: ListView<RemoteLanguage>
    private lateinit var errorLabel: Label

    private val breadCrumb = BreadCrumb().apply {
        titleProperty.bind(
            translationViewModel.selectedSourceLanguageProperty.stringBinding {
                it?.name ?: messages["sourceLanguage"]
            }
        )
        iconProperty.set(FontIcon(Material.HEARING))
        onClickAction {
            translationViewModel.selectedSourceLanguageProperty.value?.let {
                navigator.back()
            }
        }
    }

    override val root = stackpane {
        alignment = Pos.TOP_LEFT
        vbox {
            addClass("translation-wizard__root")

            label(messages["pickSourceLanguage"]) {
                addClass("translation-wizard__title")
            }
            add(
                FilteredSearchBar().apply {
                    leftIconProperty.set(FontIcon(Material.HEARING))
                    promptTextProperty.set(messages["search"])
                    filterItems.bind(viewModel.menuItems) { it }
                    viewModel.searchQueryProperty.bindBidirectional(textProperty)
                }
            )
            listview(viewModel.filteredLanguages) {
                addClass("translation-wizard__list")
//                vgrow = Priority.ALWAYS
                setCellFactory {
                    LanguageCell(LanguageType.SOURCE, viewModel.anglicizedProperty) {
                        translationViewModel.selectedSourceLanguageProperty.set(it)
                    }
                }
                viewModel.searchQueryProperty.onChange {
                    it?.let { if (it.isNotBlank()) scrollTo(0) }
                }
            }
            vbox {
                paddingBottom = 15.0
                spacing = 10.0

                label("Could not find your language? Search it online and import") {
                    addClass("translation-wizard__title")
                    paddingRight = 10.0
                }
                button {
                    text = "Fetch languages online"
                    addClass("btn", "btn--secondary")
                    setOnAction {
                        onFetchLanguages(this)
                    }
                }
                hbox {
                    label("Network Error! Please check you connection and try again.") {
                        addClass("translation-wizard__status-label")
                        errorLabel = this
                        isVisible = false
                        isManaged = false
                    }
                    listview(viewModel.remoteLanguages) {
                        addClass("translation-wizard__list")

                        remoteLanguageLV = this
                        isVisible = false
                        isManaged = false

                        cellFormat {
                            graphic = cell(this.item)
                            this.setOnMouseClicked {
                                viewModel.importLanguage(this.item)
                                    .subscribe(
                                        { },
                                        {
                                            errorLabel.isVisible = true
                                            errorLabel.isManaged = true
                                        })

                            }
                        }
                    }
                }
            }
        }
    }

    init {
        tryImportStylesheet(resources.get("/css/translation-wizard.css"))
        tryImportStylesheet(resources.get("/css/language-card-cell.css"))
        tryImportStylesheet(resources.get("/css/filtered-search-bar.css"))
        tryImportStylesheet(resources.get("/css/confirm-dialog.css"))

        translationViewModel.sourceLanguages.onChange {
            viewModel.regions.setAll(
                it.list
                    .distinctBy { language -> language.region }
                    .map { language -> language.region }
            )
            viewModel.setFilterMenu()
        }

        setUpDialog()
    }

    override fun onDock() {
        navigator.dock(this, breadCrumb)
        viewModel.resetFilter()
        translationViewModel.reset()
        translationViewModel.loadSourceLanguages()
    }

    private fun cell(item: RemoteLanguage): Node {
//        return HBox().apply {
//            label(item.language.anglicizedName) {
//                maxWidth = Double.MAX_VALUE
//                hgrow = Priority.ALWAYS
//            }
//            button("Get") {
//                hgrow = Priority.NEVER
//                setOnAction {
//                    viewModel.importLanguage(item)
//                    isDisable = true
//                }
//            }
//        }
        return LanguageCardCell().apply {
            iconProperty.set(FontIcon(Material.CLOUD_DOWNLOAD))
            languageNameProperty.set(item.language.name)
            languageSlugProperty.set(item.resourceSlug)

//            setOnMouseClicked {
//                viewModel.importLanguage(item)
//            }
        }
    }

    private fun setUpDialog() {
        val importDialog = confirmdialog {
            titleTextProperty.set(messages["importResource"])
            messageTextProperty.set(messages["importResourceMessage"])
            progressTitleProperty.set(messages["pleaseWait"])
            showProgressBarProperty.set(true)
            orientationProperty.set(settingsViewModel.orientationProperty.value)
            themeProperty.set(settingsViewModel.appColorMode.value)
        }
        viewModel.importInProgress.onChange {
            Platform.runLater { if (it) importDialog.open() else importDialog.close() }
        }
    }

    fun onFetchLanguages(element: Node) {
        viewModel.fetchLanguages()
            .subscribe(
                {
                    errorLabel.isVisible = false
                    errorLabel.isManaged = false
                    remoteLanguageLV.isVisible = true
                    remoteLanguageLV.isManaged = true
                }, {
                    errorLabel.isVisible = true
                    errorLabel.isManaged = true
                    remoteLanguageLV.isVisible = false
                    remoteLanguageLV.isManaged = false
                }
            )
    }
}
