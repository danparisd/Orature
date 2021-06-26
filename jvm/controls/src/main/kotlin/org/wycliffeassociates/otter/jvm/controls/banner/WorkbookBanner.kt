package org.wycliffeassociates.otter.jvm.controls.banner

import javafx.beans.binding.Bindings
import javafx.beans.binding.ObjectBinding
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.scene.control.Control
import javafx.scene.control.Skin
import javafx.scene.image.Image
import javafx.scene.layout.Background
import javafx.scene.layout.BackgroundImage
import javafx.scene.layout.BackgroundPosition
import javafx.scene.layout.BackgroundRepeat
import javafx.scene.layout.BackgroundSize
import org.wycliffeassociates.otter.jvm.controls.skins.banner.WorkbookBannerSkin
import java.io.File
import java.util.concurrent.Callable

class WorkbookBanner : Control() {

    val backgroundImageFileProperty = SimpleObjectProperty<File>()
    val bookTitleProperty = SimpleStringProperty()
    val resourceTitleProperty = SimpleStringProperty()
    val hideDeleteButtonProperty = SimpleBooleanProperty(false)

    val deleteTitleProperty = SimpleStringProperty("delete")
    val exportTitleProperty = SimpleStringProperty("export")

    val onDeleteActionProperty = SimpleObjectProperty<EventHandler<ActionEvent>>()
    val onExportActionProperty = SimpleObjectProperty<EventHandler<ActionEvent>>()

    init {
        styleClass.setAll("workbook-banner")
    }

    fun backgroundBinding(): ObjectBinding<Background?> {
        return Bindings.createObjectBinding(
            Callable {
                backgroundImageFileProperty.value?.let {
                    Background(backgroundImage(it))
                }
            },
            backgroundImageFileProperty
        )
    }

    private fun backgroundImage(file: File): BackgroundImage {
        val image = Image(file.inputStream())
        val backgroundSize = BackgroundSize(
            1.0,
            1.0,
            true,
            true,
            false,
            true
        )
        return BackgroundImage(
            image,
            BackgroundRepeat.NO_REPEAT,
            BackgroundRepeat.NO_REPEAT,
            BackgroundPosition.DEFAULT,
            backgroundSize
        )
    }

    fun onDeleteAction(op: () -> Unit) {
        onDeleteActionProperty.set(EventHandler { op.invoke() })
    }

    fun onExportAction(op: () -> Unit) {
        onExportActionProperty.set(EventHandler { op.invoke() })
    }

    override fun createDefaultSkin(): Skin<*> {
        return WorkbookBannerSkin(this)
    }
}
