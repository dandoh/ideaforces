package org.dandoh.ideaforces.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.DialogWrapper
import org.dandoh.ideaforces.core.isCPContext
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

  }


  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = isCPContext(e)
  }


}

