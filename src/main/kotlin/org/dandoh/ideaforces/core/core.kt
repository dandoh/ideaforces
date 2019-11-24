package org.dandoh.ideaforces.core

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.content.ContentFactory
import org.dandoh.ideaforces.actions.SpecifyCodeforcesURLAction


fun runNormal(e: AnActionEvent, cpFile: VirtualFile, cpToolWindow: ToolWindow) {
  val project = e.project ?: return
  val virtualFile = FileEditorManager.getInstance(project).selectedEditor?.file ?: return
  val dialog = SpecifyCodeforcesURLAction.SpecifyURlDialog()
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

    console.print("Compiling...\n", ConsoleViewContentType.SYSTEM_OUTPUT)
    compileCommandLine.createProcess().onExit().thenApply {
      val stdErr = String(it.errorStream.readAllBytes())
      if (it.exitValue() == 0) {
        console.print("Complied successfully\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        val commandLine = GeneralCommandLine("./${nameWithoutExtension}")
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

fun runTests(cpFile: VirtualFile, cpToolWindow: ToolWindow) {

}

fun isCPContext(e: AnActionEvent): Boolean {
  val project = e.project ?: return false
  return when (FileEditorManager
      .getInstance(project)
      .selectedEditor
      ?.file
      ?.extension) {
    "cc", "cpp", "java", "py" -> true
    else -> false
  }
}
