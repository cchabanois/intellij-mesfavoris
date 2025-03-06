package mesfavoris.internal

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.io.Ksuid
import com.intellij.util.xmlb.annotations.Attribute

@Service(Service.Level.PROJECT)
@State(name = "MesFavorisProjectId", storages = [(Storage(StoragePathMacros.WORKSPACE_FILE))], reportStatistic = false)
internal class MesFavorisProjectIdManager : SimplePersistentStateComponent<ProjectIdState>(ProjectIdState()) {
    companion object {
        fun getInstance(project: Project) = project.service<MesFavorisProjectIdManager>()
        fun getProjectId(project : Project) : String {
            val projectIdManager = project.service<MesFavorisProjectIdManager>()
            var projectId = projectIdManager.state.id
            if (projectId == null) {
                // do not use project name as part of id, to ensure that project dir renaming also will not cause data loss
                projectId = Ksuid.generate()
                projectIdManager.state.id = projectId
            }
            return projectId
        }
    }
}

internal class ProjectIdState : BaseState() {
    @get:Attribute
    var id by string()
}