package org.dandoh.ideaforces.toolwindows

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory


class IdeaforcesToolWindowFactory : ToolWindowFactory, DumbAware {


  /**
   * Only call once
   */
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
    val content = ContentFactory.SERVICE.getInstance()
        .createContent(console.component, "", false)
    toolWindow.contentManager.addContent(content);
  }
}
