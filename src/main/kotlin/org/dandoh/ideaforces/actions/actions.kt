package org.dandoh.ideaforces.actions

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import org.dandoh.ideaforces.core.fileToExecutor
import org.dandoh.ideaforces.ui.SpecifyURLForm
import javax.swing.JComponent


class SpecifyCodeforcesURLAction : AnAction() {
  class SpecifyURlDialog : DialogWrapper(true) {
    init {
      super.init()
    }

    override fun createCenterPanel(): JComponent? {
      val form = SpecifyURLForm()
      myPreferredFocusedComponent = form.url
      return form.content
    }

  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val virtualFile = FileEditorManager.getInstance(project).selectedEditor?.file ?: return
    val dialog = SpecifyURlDialog()
    if (dialog.showAndGet()) {
      val nameWithoutExtension = virtualFile.nameWithoutExtension

      val console = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Ideaforces")
      val content = ContentFactory.SERVICE.getInstance()
          .createContent(console.component, nameWithoutExtension, false)
      toolWindow.contentManager.removeAllContents(true)
      toolWindow.contentManager.addContent(content)

      val compileCommandLine =
          GeneralCommandLine("c++", "-std=c++14", virtualFile.name, "-o", nameWithoutExtension)
              .withWorkDirectory(virtualFile.parent.path)
      compileCommandLine.createProcess().onExit().thenApply {
        val stdErr = String(it.errorStream.readAllBytes())
        if (it.exitValue() == 0) {
          console.print("Complied successfully\n", ConsoleViewContentType.SYSTEM_OUTPUT)
          val commandLine = GeneralCommandLine("./${nameWithoutExtension}" )
              .withWorkDirectory(virtualFile.parent.path)
          val handler = OSProcessHandler(commandLine)
          console.attachToProcess(handler)
          handler.startNotify()
        } else {
          console.print(stdErr, ConsoleViewContentType.ERROR_OUTPUT)
        }
      }



    }

  }


  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    val virtualFile = FileEditorManager.getInstance(project).selectedEditor?.file
    e.presentation.isEnabled = false;
    virtualFile
        ?.extension
        ?.let { fileToExecutor(it) }
        ?.let { e.presentation.isEnabled = true }
  }


}

