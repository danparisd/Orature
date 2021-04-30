package org.wycliffeassociates.otter.jvm.workbookapp.ui.screens.chunking

import org.wycliffeassociates.otter.jvm.markerapp.app.view.MarkerView
import javafx.geometry.Pos
import javafx.scene.layout.BorderPane
import javafx.scene.layout.Region
import javafx.scene.control.ButtonBar
import javafx.scene.paint.Color
import javafx.scene.text.FontPosture
import javafx.scene.text.FontWeight
import tornadofx.*

class ChunkingWizard : Wizard() {

    val vm: ChunkingViewModel by inject()

    override fun onDock() {
        val top = vbox {
            addClass(WizardStyles.header)
            alignment = Pos.CENTER
            label {
                textProperty().bind(vm.titleProperty)
                style {
                    fontSize = 18.pt
                    fontFamily = "Atkinson Hyperlegible"
                    fontWeight = FontWeight.BOLD
                    fontStyle = FontPosture.REGULAR
                }
            }
            label {
                textProperty().bind(vm.stepProperty)
                style {
                    fontSize = 12.pt
                    fontFamily = "Atkinson Hyperlegible"
                    fontWeight = FontWeight.NORMAL
                    fontStyle = FontPosture.REGULAR
                }
            }
            hbox {
                alignment = Pos.CENTER
                addClass(WizardStyles.buttons)
                spacer()
                button() {
                    styleClass.addAll("btn", "btn--secondary")
                    textProperty().bind(backButtonTextProperty)
                    runLater {
                        enableWhen(canGoBack)
                    }
                    action { back() }
                }
                spacer()
                button() {
                    styleClass.addAll("btn", "btn--secondary")
                    textProperty().bind(nextButtonTextProperty)
                    runLater {
                        enableWhen(canGoNext.and(hasNext).and(currentPageComplete))
                    }
                    action { next() }
                }
                spacer()
                style {
                    borderWidth += box(0.px)
                    borderColor += box(Color.TRANSPARENT)
                }
            }
        }
        root.bottom.getChildList()!!.clear()
        root.top.replaceWith(top)
        root.bottom.replaceWith(Region())
        root.left.replaceWith(Region())
    }

    init {
        add<Consume>()
        add<Verbalize>()
        add<ChunkPage>()
    }

    override fun closeWizard() {
        complete.set(false)
        workspace.navigateBack()
    }
}
