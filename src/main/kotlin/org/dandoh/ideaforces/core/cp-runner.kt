package org.dandoh.ideaforces.core

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import org.dandoh.ideaforces.utils.updateUI
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


object CPRunner {
  private var processes: MutableList<Process> = mutableListOf()

  @Synchronized
  fun idle() : Boolean {
    return processes.isEmpty()
  }

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
  private fun filterProcess() {
    processes = processes.filter { it.isAlive }.toMutableList()
  }

  @Synchronized
  fun startProcess(commandLine: GeneralCommandLine): Process {
    val process = commandLine.createProcess()
    addProcess(process)
    process.onExit().thenApply {
      filterProcess()
    }
    return process
  }

  @Synchronized
  fun startProcessWithConsole(commandLine: GeneralCommandLine, consoleView: ConsoleView): CompletableFuture<Process> {
    val handler = OSProcessHandler(commandLine)
    consoleView.attachToProcess(handler)
    addProcess(handler.process)

    handler.startNotify()
    return handler.process.onExit().thenApply {
      filterProcess()
      TimeUnit.MICROSECONDS.sleep(100)
      updateUI {
        consoleView.print("\nProcess exited with code ${it.exitValue()}\n", ConsoleViewContentType.SYSTEM_OUTPUT)
      }
      return@thenApply it
    }
  }

}

