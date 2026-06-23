package net.spooncast.openmocker.plugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout

/**
 * OpenMocker ToolWindow 의 골격 팩토리.
 *
 * 현재는 빈 placeholder 패널만 부착한다. REST 패널(T14)·WS 패널(T15)·기기 드롭다운/상태 배너는
 * 이후 task 에서 이 팩토리를 확장해 채운다.
 */
class MockerToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(JBLabel("OpenMocker", JBLabel.CENTER), BorderLayout.CENTER)
        }
        val content = toolWindow.contentManager.factory.createContent(panel, null, false)
        toolWindow.contentManager.addContent(content)
    }
}
