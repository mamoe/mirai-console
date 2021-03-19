/*
 * Copyright 2019-2021 Mamoe Technologies and contributors.
 *
 *  此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 *  Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 *  https://github.com/mamoe/mirai/blob/master/LICENSE
 */


package net.mamoe.mirai.console.intellij.creator.steps

import com.intellij.ide.util.projectWizard.ModuleWizardStep
import kotlinx.coroutines.*
import net.mamoe.mirai.console.intellij.creator.MiraiProjectModel
import net.mamoe.mirai.console.intellij.creator.MiraiVersionKind
import net.mamoe.mirai.console.intellij.creator.PluginCoordinates
import net.mamoe.mirai.console.intellij.creator.checkNotNull
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*

class PluginCoordinatesStep(
    private val model: MiraiProjectModel
) : ModuleWizardStep() {

    private lateinit var panel: JPanel

    @field:Validation.NotBlank("ID")
    private lateinit var idField: JTextField

    private lateinit var nameField: JTextField
    private lateinit var authorField: JTextField
    private lateinit var dependsOnField: JTextField
    private lateinit var infoArea: JTextArea
    private lateinit var miraiVersionKindBox: JComboBox<MiraiVersionKind>
    private lateinit var miraiVersionBox: JComboBox<String>

    override fun getComponent() = panel

    private val versionKindChangeListener: ItemListener = ItemListener { event ->
        if (event.stateChange != ItemEvent.SELECTED) return@ItemListener

        updateVersionItems()
    }

    override fun getPreferredFocusedComponent(): JComponent = idField

    override fun updateStep() {
        miraiVersionKindBox.removeAllItems()
        miraiVersionKindBox.isEnabled = true
        MiraiVersionKind.values().forEach { miraiVersionKindBox.addItem(it) }
        miraiVersionKindBox.selectedItem = MiraiVersionKind.DEFAULT
        miraiVersionKindBox.addItemListener(versionKindChangeListener) // when selected, change versions

        miraiVersionBox.removeAllItems()
        miraiVersionBox.addItem(VERSION_LOADING_PLACEHOLDER)
        miraiVersionBox.selectedItem = VERSION_LOADING_PLACEHOLDER

        model.availableMiraiVersionsOrFail.invokeOnCompletion {
            updateVersionItems()
        }

        fun String.convertCapitalized(): String = buildString {
            for (char in this@convertCapitalized) {
                if (char.isUpperCase() && lastOrNull()?.isLetterOrDigit() == true) {
                    append('-')
                    append(char.toLowerCase())
                } else {
                    append(char)
                }
            }
        }

        if (idField.text.isNullOrEmpty()) {
            model.projectCoordinates.checkNotNull("projectCoordinates").run {
                idField.text = "$groupId.$artifactId"
            }
        }
    }

    private fun updateVersionItems() {
        GlobalScope.launch(Dispatchers.Main + CoroutineName("updateVersionItems")) {
            if (!model.availableMiraiVersionsOrFail.isCompleted) return@launch
            miraiVersionBox.removeAllItems()
            val expectingKind = miraiVersionKindBox.selectedItem as? MiraiVersionKind ?: MiraiVersionKind.DEFAULT
            model.availableMiraiVersionsOrFail.await()
                .sortedDescending()
                .filter { v ->
                    expectingKind.isThatKind(v)
                }
                .forEach { v -> miraiVersionBox.addItem(v) }
            miraiVersionBox.isEnabled = true
        }
    }

    override fun updateDataModel() {
        model.pluginCoordinates = PluginCoordinates(
            id = idField.text,
            author = authorField.text.ifBlank { null },
            name = nameField.text,
            info = infoArea.text,
            dependsOn = dependsOnField.text,
        )
        model.miraiVersion = miraiVersionBox.selectedItem?.toString() ?: "+"
    }

    override fun validate(): Boolean {
        if (miraiVersionBox.selectedItem?.toString() == VERSION_LOADING_PLACEHOLDER) {
            Validation.popup("请等待获取版本号", miraiVersionBox)
            return false
        }
        return Validation.doValidation(this)
    }

    companion object {
        const val VERSION_LOADING_PLACEHOLDER = "Loading..."
    }
}
