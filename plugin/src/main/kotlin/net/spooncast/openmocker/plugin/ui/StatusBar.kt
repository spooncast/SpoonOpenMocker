package net.spooncast.openmocker.plugin.ui

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import net.spooncast.openmocker.plugin.session.ConnectionState
import java.awt.FlowLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities

class StatusBar(private val project: Project) : JPanel(FlowLayout(FlowLayout.LEFT, 6, 2)) {

    private val label = JBLabel("● 연결 안 됨").also { add(it) }

    fun update(state: ConnectionState, errorMessage: String? = null) {
        SwingUtilities.invokeLater {
            when (state) {
                ConnectionState.IDLE -> {
                    label.text = "● 연결 안 됨"
                    label.foreground = JBColor.GRAY
                }
                ConnectionState.FORWARDING -> {
                    label.text = "◎ 포워딩 중…"
                    label.foreground = JBColor.ORANGE
                }
                ConnectionState.CONNECTED -> {
                    label.text = "● 연결됨"
                    label.foreground = JBColor.GREEN
                }
                ConnectionState.ERROR -> {
                    label.text = "✗ 오류"
                    label.foreground = JBColor.RED
                    notifyError(errorMessage ?: "알 수 없는 오류")
                }
            }
        }
    }

    private fun notifyError(message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("OpenMocker")
            .createNotification("SpoonOpenMocker 연결 실패", message, NotificationType.ERROR)
            .notify(project)
    }
}
