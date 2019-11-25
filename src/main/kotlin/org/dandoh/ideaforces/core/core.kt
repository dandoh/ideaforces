package org.dandoh.ideaforces.core

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.ContentFactory
import org.dandoh.ideaforces.services.IdeaforcesService
import org.dandoh.ideaforces.toolwindows.IdeaforcesToolWindowFactory
import org.dandoh.ideaforces.ui.SpecifyURLForm
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JComponent

val codeforcesURLPattern = Regex("""(http|https)://(www\.)?codeforces\.com/contest/\d+/problem/[A-Z](\?.+=.+)?""")

fun compileCommand(file: VirtualFile): GeneralCommandLine {
  return when (file.extension) {
    "cc", "cpp" -> GeneralCommandLine("c++", "-std=c++14", file.name, "-o", file.nameWithoutExtension)
        .withWorkDirectory(file.parent.path)
    "javac" -> GeneralCommandLine("javac", file.name)
    else -> GeneralCommandLine()
  }.withWorkDirectory(file.parent.path)
}

fun runCommand(file: VirtualFile): GeneralCommandLine {
  return when (file.extension) {
    "cc", "cpp" -> GeneralCommandLine("./${file.nameWithoutExtension}")
    "java" -> GeneralCommandLine("java", file.nameWithoutExtension)
    else -> GeneralCommandLine()
  }.withWorkDirectory(file.parent.path)

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

class SpecifyURLDialog(initUrl: String?) : DialogWrapper(true) {
  val form = SpecifyURLForm()

  init {
    form.url.text = initUrl
    super.init()
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return form.url
  }

  override fun createCenterPanel(): JComponent? {
    return form.content
  }

  override fun doValidate(): ValidationInfo? {
    val url = form.url.text
    if (codeforcesURLPattern.matches(url.trim())) {
      return null
    } else {
      return ValidationInfo("Invalid Codeforces Problem URL", form.url)
    }
  }
}

fun askCodeforcesURL(e: AnActionEvent): Pair<String, String>? {
  val project = e.project ?: return null
  val currentFile = FileEditorManager
      .getInstance(project)
      .selectedEditor
      ?.file ?: return null
  val path = currentFile.path
  val dialog = SpecifyURLDialog(IdeaforcesService.getService().getUrl(path))
  if (dialog.showAndGet()) {
    val url = dialog.form.url.text?.trim()
    url?.let {
      IdeaforcesService.getService().updateUrl(path, it)
      return Pair(path, url)
    }
  }
  return null
}

fun runNormal(e: AnActionEvent) {
  val project = e.project ?: return
  val toolWindow = IdeaforcesToolWindowFactory.getToolWindow(project)
  val file = FileEditorManager.getInstance(project).selectedEditor?.file ?: return
  when (val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console) {
    is ConsoleViewImpl ->
      toolWindow.show {
        val content = ContentFactory.SERVICE.getInstance()
            .createContent(consoleView.component, file.nameWithoutExtension, false)
        toolWindow.contentManager.removeAllContents(true)
        toolWindow.contentManager.addContent(content)
        toolWindow.contentManager.requestFocus(content, true)
        consoleView.editor.contentComponent.requestFocusInWindow()
        consoleView.editor.contentComponent.addKeyListener(object : KeyListener {
          override fun keyTyped(e: KeyEvent) {}
          override fun keyReleased(e: KeyEvent) {}
          override fun keyPressed(e: KeyEvent) {
            if (e.isControlDown && e.keyCode == KeyEvent.VK_C) CPRunner.stopAll()
          }
        })

        CPRunner.stopAll()
        CPRunner.startProcess(compileCommand(file), consoleView) {
          if (it.exitValue() == 0) {
            CPRunner.startProcess(runCommand(file), consoleView) {}
          }
        }
      }

  }


}

fun runTests(cpFile: VirtualFile, cpToolWindow: ToolWindow) {

}

