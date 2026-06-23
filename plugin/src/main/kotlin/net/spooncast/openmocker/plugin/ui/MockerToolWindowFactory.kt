package net.spooncast.openmocker.plugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import net.spooncast.openmocker.plugin.net.ControlClient
import net.spooncast.openmocker.plugin.settings.MockerSettings

class MockerToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val settings = MockerSettings.getInstance().state
        val client = ControlClient(port = settings.port)
        val restPanel = RestPanel(client)

        val content = toolWindow.contentManager.factory.createContent(restPanel, "REST", false)
        toolWindow.contentManager.addContent(content)

        toolWindow.contentManager.addContentManagerListener(object :
            com.intellij.ui.content.ContentManagerListener {
            override fun contentRemoved(event: com.intellij.ui.content.ContentManagerEvent) {
                restPanel.stopPolling()
            }
        })
    }
}
