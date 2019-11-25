package org.dandoh.ideaforces.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.dandoh.ideaforces.core.askCodeforcesURL
import org.dandoh.ideaforces.core.isCPContext
import org.dandoh.ideaforces.core.runNormal

abstract class CompetitiveProgrammingAction : AnAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isCPContext(e)
  }
}

class SpecifyCodeforcesURLAction : CompetitiveProgrammingAction() {
  override fun actionPerformed(e: AnActionEvent) {
    askCodeforcesURL(e)
  }

}

class RunProblemNormalAction : CompetitiveProgrammingAction() {
  override fun actionPerformed(e: AnActionEvent) {
    runNormal(e)
  }
}

