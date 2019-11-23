package org.dandoh.ideaforces.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil


@State(name = "Ideaforces", storages = [Storage("ideaforces.xml")])
class IdeaforcesService : PersistentStateComponent<IdeaforcesService.Data>, DumbAware {

  companion object {
    fun getService(project: Project): IdeaforcesService {
      return ServiceManager
          .getService(project, IdeaforcesService::class.java)
    }
  }

  /**
   * Persistent
   */
  data class Data(val counter: Int) {
    constructor() : this(3)
  }

  private val data = Data();

  override fun getState(): Data? {
    return data.copy()
  }

  override fun loadState(state: Data) {
    XmlSerializerUtil.copyBean(state, data);
  }

}

