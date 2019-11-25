package org.dandoh.ideaforces.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.DumbAware
import com.intellij.util.xmlb.XmlSerializerUtil


@State(name = "IdeaforcesService", storages = [Storage(value = "ideaforces.xml")])
class IdeaforcesService : PersistentStateComponent<IdeaforcesService>, DumbAware {

  companion object {
    fun getService(): IdeaforcesService {
      return ServiceManager.getService(IdeaforcesService::class.java)
    }
  }

  var problem2Url: Map<String, String> = mapOf()


  fun getUrl(path: String): String? {
    return problem2Url[path]
  }

  fun updateUrl(path: String, url: String) {
    problem2Url = problem2Url.plus(path to url)
  }

  override fun getState(): IdeaforcesService? {
    return this;
  }

  override fun loadState(state: IdeaforcesService) {
    XmlSerializerUtil.copyBean(state, this);
  }

}

