package org.dandoh.ideaforces.configuration

import com.google.wireless.android.sdk.stats.AndroidStudioEvent
import com.intellij.execution.Executor
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import icons.PluginIcons
import javax.swing.Icon

class CPPConfigurationType : ConfigurationType {
  override fun getIcon(): Icon {
    return PluginIcons.ICON_CODEFORCES
  }

  override fun getConfigurationTypeDescription(): String {
    return "C/C++ Codeforces"
  }

  override fun getId(): String {
    return "C_C++_CP"
  }

  override fun getDisplayName(): String {
    return "C/C++"
  }

  override fun getConfigurationFactories(): Array<ConfigurationFactory> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

}

class CPPConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
  override fun createTemplateConfiguration(project: Project): RunConfiguration {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}

class CPPRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    RunConfigurationBase<CPPRunConfiguration.Data>(project, factory, name) {
  override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  data class Data(val int: Int)

}

class IdeaforcesRunConfigurationProducer : RunConfigurationProducer<>(true) {

}