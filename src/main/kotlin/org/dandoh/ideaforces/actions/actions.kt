package org.dandoh.ideaforces.actions

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlElement
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.content.ContentFactory
import org.dandoh.ideaforces.core.CPRunner
import org.dandoh.ideaforces.services.IdeaforcesService
import org.dandoh.ideaforces.toolwindows.IdeaforcesToolWindowFactory
import org.dandoh.ideaforces.ui.SpecifyURLForm
import org.dandoh.ideaforces.utils.logIde
import org.dandoh.ideaforces.utils.updateUI
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JComponent
import kotlin.concurrent.thread

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

abstract class CompetitiveProgrammingAction : AnAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isCPContext(e)
  }
}

data class ProblemSuite(val file: VirtualFile, val consoleViewImpl: ConsoleViewImpl)

fun makeProblemFromAction(e: AnActionEvent, onSuccess: (ProblemSuite) -> Unit) {
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
        CPRunner.startProcessWithConsole(compileCommand(file), consoleView).thenApply {
          if (it.exitValue() == 0) {
            onSuccess(ProblemSuite(file, consoleView))
          }
        }
      }

  }
}

class SpecifyCodeforcesURLAction : CompetitiveProgrammingAction() {
  override fun actionPerformed(e: AnActionEvent) {
    askCodeforcesURL(e)
  }

}

class RunProblemNormalAction : CompetitiveProgrammingAction() {
  override fun actionPerformed(e: AnActionEvent) {
    makeProblemFromAction(e) {
      CPRunner.startProcessWithConsole(runCommand(it.file), it.consoleViewImpl)
    }
  }
}

class RunProblemTestsAction : CompetitiveProgrammingAction() {
  override fun actionPerformed(e: AnActionEvent) {
    makeProblemFromAction(e) {
      val queriedUrl = IdeaforcesService.getService().getUrl(it.file.path)
      val url = queriedUrl ?: askCodeforcesURL(e)?.second ?: return@makeProblemFromAction
      thread {
        val webClient = WebClient()
        webClient.options.isJavaScriptEnabled = false
        webClient.options.isCssEnabled = false
        updateUI {
          it.consoleViewImpl.clear()
          it.consoleViewImpl.print("Fetching provided tests...\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        }
        try {
          val page = webClient.getPage<HtmlPage>(url)
          val sampleTests = page.getByXPath<HtmlElement>("//div[@class='sample-test']")
          updateUI {
            it.consoleViewImpl.print("Running test...\n", ConsoleViewContentType.SYSTEM_OUTPUT)
          }
          sampleTests.forEach {
            val input = it.getFirstByXPath<HtmlElement>("div[@class='input']//pre").asText()
            val output = it.getFirstByXPath<HtmlElement>("div[@class='output']//pre").asText()
            logIde(input.trim())
            logIde(output.trim())
          }
        } catch (e: Exception) {

        }
      }.start()
    }
  }

}

