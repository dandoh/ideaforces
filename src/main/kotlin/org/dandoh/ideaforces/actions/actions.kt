package org.dandoh.ideaforces.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import org.dandoh.ideaforces.core.file2Executor


class SpecifyCodeforcesURLAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {

  }


  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    val virtualFile = FileEditorManager.getInstance(project).selectedEditor?.file
    e.presentation.isEnabled = false;
    virtualFile
        ?.extension
        ?.let { file2Executor(it) }
        ?.let { e.presentation.isEnabled = true }
  }


}

