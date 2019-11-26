package org.dandoh.ideaforces.actions

import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlElement
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import org.dandoh.ideaforces.core.CPRunner
import org.dandoh.ideaforces.services.IdeaforcesService
import org.dandoh.ideaforces.toolwindows.IdeaforcesToolWindowFactory
import org.dandoh.ideaforces.ui.SpecifyURLForm
import org.dandoh.ideaforces.utils.updateUI
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.*
import javax.swing.JComponent
import kotlin.concurrent.thread
import kotlin.concurrent.timerTask

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

class SpecifyURLDialog(initUrl: String) : DialogWrapper(true) {
  val form = SpecifyURLForm()

  init {
    form.url.text = initUrl;
    super.init()
  }

  override fun getPreferredFocusedComponent(): JComponent? = form.url
  override fun createCenterPanel(): JComponent? = form.content

  override fun doValidate(): ValidationInfo? {
    val url = form.url.text
    if (codeforcesURLPattern.matches(url.trim())) {
      return null
    } else {
      return ValidationInfo("Invalid Codeforces Problem URL", form.url)
    }
  }
}

fun askCodeforcesURL(path: String): Pair<String, String>? {
  val dialog = SpecifyURLDialog(IdeaforcesService.getService().getUrl(path).orEmpty())
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

data class ProblemSuite(val file: VirtualFile, val console: ConsoleViewImpl)

fun makeProblemFromAction(e: AnActionEvent, onSuccess: (ProblemSuite) -> Unit) {
  val project = e.project ?: return
  val toolWindow = IdeaforcesToolWindowFactory.getToolWindow(project)
  val file = FileEditorManager.getInstance(project).selectedEditor?.file ?: return
  FileDocumentManager.getInstance().saveAllDocuments()
  when (val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console) {
    is ConsoleViewImpl ->
      toolWindow.show {
        val content = IdeaforcesToolWindowFactory.createToolWindowContent(consoleView, file.nameWithoutExtension)
        toolWindow.contentManager.removeAllContents(true)
        toolWindow.contentManager.addContent(content)
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
            updateUI {
              onSuccess(ProblemSuite(file, consoleView))
            }
          }
        }
      }

  }
}

/**
 * Specify URL Action
 */
class SpecifyCodeforcesURLAction : CompetitiveProgrammingAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val currentFile = FileEditorManager
        .getInstance(project)
        .selectedEditor
        ?.file ?: return
    val path = currentFile.path
    askCodeforcesURL(path)
  }

}

/**
 * Stop all processes
 */
class StopAllProcessesAction : AnAction("Stop All",
    "Stop all running instances", AllIcons.Actions.Suspend) {
  override fun actionPerformed(e: AnActionEvent) {
    CPRunner.stopAll()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = !CPRunner.idle()
  }
}

/**
 * Run Problem Action
 */
class RunProblemNormalAction : CompetitiveProgrammingAction() {
  override fun actionPerformed(e: AnActionEvent) {
    makeProblemFromAction(e) {
      CPRunner.startProcessWithConsole(runCommand(it.file), it.console)
      it.console.editor.contentComponent.requestFocusInWindow()
    }
  }
}

/**
 * Run Tests Action
 */
class RunProblemTestsAction : CompetitiveProgrammingAction() {
  override fun actionPerformed(e: AnActionEvent) {
    makeProblemFromAction(e) { problem ->
      val queriedUrl = IdeaforcesService.getService().getUrl(problem.file.path)
      val url = queriedUrl ?: askCodeforcesURL(problem.file.path)?.second ?: return@makeProblemFromAction
      thread {
        try {
          val webClient = WebClient()
          webClient.options.isJavaScriptEnabled = false
          webClient.options.isCssEnabled = false
          updateUI {
            problem.console.clear()
            problem.console.print("Fetching provided tests...\n", ConsoleViewContentType.SYSTEM_OUTPUT)
          }
          val page = webClient.getPage<HtmlPage>(url)
          val tests = page.getFirstByXPath<HtmlElement>("//div[@class='sample-tests']")
          val inputs = tests.getByXPath<HtmlElement>("//div[@class='input']//pre")
              .map { it.asText().trim() }
          val outputs = tests.getByXPath<HtmlElement>("//div[@class='output']//pre")
              .map { it.asText().trim() }
          val sampleTests = inputs.zip(outputs)
          updateUI {
            problem.console.print("Running tests...\n", ConsoleViewContentType.SYSTEM_OUTPUT)
          }
          sampleTests.forEachIndexed { id, (input, output) ->
            val process = CPRunner.startProcess(runCommand(problem.file))
            process.onExit().thenApply {
              if (it.exitValue() == 0) {
                val res = String(it.inputStream.readAllBytes()).trim()
                val got = res.split(Regex("""\s+"""))
                val expect = output.split(Regex("""\s+"""))

                if (got == expect) {
                  updateUI {
                    problem.console.print("Test $id: MATCHED\n", ConsoleViewContentType.USER_INPUT)
                  }
                } else {
                  updateUI {
                    problem.console.print("Test $id: NOT MATCHED\n", ConsoleViewContentType.ERROR_OUTPUT)
                    problem.console.print("Codeforces: \n", ConsoleViewContentType.SYSTEM_OUTPUT)
                    problem.console.print(output + "\n", ConsoleViewContentType.SYSTEM_OUTPUT)
                    problem.console.print("Got: \n", ConsoleViewContentType.SYSTEM_OUTPUT)
                    problem.console.print(res + "\n", ConsoleViewContentType.SYSTEM_OUTPUT)
                  }
                }
              } else {
                updateUI {
                  problem.console.print("Test $id exited with code ${it.exitValue()}\n",
                      ConsoleViewContentType.ERROR_OUTPUT)
                }
              }
            }
            Timer().schedule(timerTask {
              if (process.isAlive) {
                process.destroy()
                updateUI {
                  problem.console.print("Test $id: Time out\n",
                      ConsoleViewContentType.ERROR_OUTPUT)
                }
              }
            }, 1000)
            val writer = process.outputStream.writer()
            writer.write(input);
            writer.close()
          }
        } catch (e: Exception) {
          updateUI {
            problem.console.print("\nCouldn't run Codeforces test, perhaps check your problem URL or internet " +
                "connection?", ConsoleViewContentType.ERROR_OUTPUT)
          }
        }
      }
    }
  }

}

