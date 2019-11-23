package org.dandoh.ideaforces.utils

import com.intellij.openapi.application.ApplicationManager
import java.util.logging.Logger


fun logIDE(anything: Any?) {
  Logger.getGlobal().info(anything.toString())
}


fun updateUI(updater: () -> Unit) {
  ApplicationManager.getApplication().invokeLater(updater)
}

