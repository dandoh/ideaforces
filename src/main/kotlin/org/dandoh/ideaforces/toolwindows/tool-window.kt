package org.dandoh.ideaforces.toolwindows

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory


class IdeaforcesToolWindowFactory : ToolWindowFactory, DumbAware {
  companion object {
    fun getToolWindow(project: Project): ToolWindow {
      return ToolWindowManager.getInstance(project).getToolWindow("Ideaforces")
    }

    fun createToolWindowContent(consoleView: ConsoleView, displayName: String? = null): Content {
      val panel = SimpleToolWindowPanel(false, true)
//      val actionToolbar = ActionManager.getInstance()
//          .createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, false)
//      displayName?.let {
//        panel.toolbar = actionToolbar.component
//      }
      panel.setContent(consoleView.component)


      return ContentFactory.SERVICE.getInstance()
          .createContent(panel, displayName, false)

    }
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
    val content = createToolWindowContent(console)
    toolWindow.contentManager.addContent(content);
  }
}
