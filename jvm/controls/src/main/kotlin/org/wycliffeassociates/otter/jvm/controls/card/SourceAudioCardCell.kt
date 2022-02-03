package org.wycliffeassociates.otter.jvm.controls.card

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Node
import javafx.scene.layout.HBox
import tornadofx.addClass
import tornadofx.*

class SourceAudioCardCell : HBox() {
    val categoryProperty = SimpleStringProperty()
    val iconProperty = SimpleObjectProperty<Node>()
    val titleProperty = SimpleStringProperty()
    val slugProperty = SimpleStringProperty()

    init {
        styleClass.setAll("language-card-cell")

        label {
            addClass("language-card-cell__icon")
            graphicProperty().bind(iconProperty)
        }

        vbox {
            addClass("language-card-cell__title")
            label(titleProperty).apply {
                addClass("language-card-cell__name")
            }
            label(slugProperty).apply {
                addClass("language-card-cell__slug")
            }
        }
    }
}