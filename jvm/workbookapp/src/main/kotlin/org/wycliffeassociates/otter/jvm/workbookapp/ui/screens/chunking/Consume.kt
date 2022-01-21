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
package org.wycliffeassociates.otter.jvm.workbookapp.ui.screens.chunking

import java.io.File
import javafx.geometry.Pos
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javax.inject.Inject
import org.kordamp.ikonli.javafx.FontIcon
import org.wycliffeassociates.otter.common.audio.AudioFile
import org.wycliffeassociates.otter.common.device.IAudioPlayer
import org.wycliffeassociates.otter.jvm.controls.controllers.AudioPlayerController
import org.wycliffeassociates.otter.jvm.controls.waveform.AudioSlider
import org.wycliffeassociates.otter.jvm.controls.waveform.WaveformImageBuilder
import org.wycliffeassociates.otter.jvm.device.audio.AudioConnectionFactory
import org.wycliffeassociates.otter.jvm.utils.onChangeAndDoNow
import org.wycliffeassociates.otter.jvm.workbookapp.di.IDependencyGraphProvider
import org.wycliffeassociates.otter.jvm.workbookapp.ui.viewmodel.ChunkingViewModel
import org.wycliffeassociates.otter.jvm.workbookapp.ui.viewmodel.WorkbookDataStore
import tornadofx.*

class Consume : Fragment() {

    val playIcon = FontIcon("mdi-play").apply {iconSize = 36}
    val pauseIcon = FontIcon("mdi-pause").apply {iconSize = 36}

    val vm: ChunkingViewModel by inject()
    // val wkbk: WorkbookDataStore by inject()

    val audioController: AudioPlayerController
    val audioSlider: AudioSlider

    @Inject
    lateinit var audioPlayerProvider: AudioConnectionFactory
    val ap: IAudioPlayer
    val waveformImageBuilder = WaveformImageBuilder(wavColor = Color.web("#00153399"))

    override fun onDock() {
        super.onDock()
        vm.titleProperty.set("Consume")
        vm.stepProperty.set("Listen to the source audio for chapter ${1}. Pay attention to stories and important events.")
    }

    init {
        (app as IDependencyGraphProvider).dependencyGraph.inject(this)
        ap = audioPlayerProvider.getPlayer()
        ap.load(File("/Users/joe/Documents/test12345.mp3"))

        audioSlider = AudioSlider().apply {
            prefHeightProperty().set(400.0)
            player.set(ap)
            val wav = AudioFile(File("/Users/joe/Documents/test12345.mp3"))
            secondsToHighlightProperty.set(0)
            waveformImageBuilder.build(wav.reader()).subscribe { img ->
                waveformImageProperty.set(img)
            }
        }

        audioController = AudioPlayerController(audioSlider, ap)
    }

    override val root = vbox {
        importStylesheet(resources["/css/control.css"])
        borderpane {
            alignment = Pos.CENTER
            hgrow = Priority.ALWAYS
            vgrow = Priority.ALWAYS
            center = audioSlider
        }
        hbox {
            prefHeight = 88.0
            alignment = Pos.CENTER
            style {
                backgroundColor += Paint.valueOf("#00377C")
            }
            button {
                audioController.isPlayingProperty.onChangeAndDoNow {
                    it?.let {
                        when(it) {
                            true -> graphic = pauseIcon
                            false -> graphic = playIcon
                        }
                    }
                }
                styleClass.addAll("btn", "btn--cta")
                action {
                    audioController.toggle()
                }
                style {
                    prefHeight = 60.px
                    prefWidth = 60.px
                    borderRadius += box(90.px)
                    backgroundRadius += box(90.px)
                }
            }
        }
    }
}
