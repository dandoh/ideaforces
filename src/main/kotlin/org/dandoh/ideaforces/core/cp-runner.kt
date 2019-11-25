package org.dandoh.ideaforces.core

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import java.util.*
import kotlin.concurrent.timerTask


object CPRunner {
  private var processes: MutableList<Process> = mutableListOf()

  @Synchronized
  fun stopAll() {
    processes.forEach { if (it.isAlive) it.destroy() }
    processes = mutableListOf()
  }

  @Synchronized
  private fun addProcess(process: Process) {
    processes = processes.filter { it.pid() != process.pid() }.toMutableList()
    processes.add(process)
  }

  @Synchronized
  fun startProcess(commandLine: GeneralCommandLine, consoleView: ConsoleView, onExit: (Process) -> Unit): Unit {
    val handler = OSProcessHandler(commandLine)
    consoleView.attachToProcess(handler)
    handler.startNotify()
    addProcess(handler.process)
    handler.process.onExit().thenApply {
      Timer().schedule(timerTask {
        consoleView.print("\nProcess exited with code ${it.exitValue()}\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        onExit(it)
      }, 100)
    }
  }

}

