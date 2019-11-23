package org.dandoh.ideaforces.actions

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.DialogWrapper
import org.dandoh.ideaforces.core.fileToExecutor
import org.dandoh.ideaforces.ui.SpecifyURLForm
import org.dandoh.ideaforces.utils.logIde
import java.nio.file.Paths
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
      val outputExecutablePath = Paths.get(virtualFile.parent.path, nameWithoutExtension)

      val commandLine =
          GeneralCommandLine("c++", "--help")
              .withWorkDirectory(virtualFile.parent.path)
      commandLine.createProcess().onExit().thenApply { process ->

        val output = String(process.inputStream.readBytes())
        logIde(output)
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

