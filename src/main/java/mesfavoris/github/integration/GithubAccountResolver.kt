package mesfavoris.github.integration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import git4idea.DialogManager
import kotlinx.coroutines.runBlocking
import mesfavoris.github.integration.IGithubAccountResolver.GithubAccountInfo
import org.jetbrains.plugins.github.authentication.GHAccountsUtil
import org.jetbrains.plugins.github.authentication.accounts.GHAccountManager
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount
import org.jetbrains.plugins.github.authentication.ui.GithubChooseAccountDialog

/**
 * Resolves the GitHub account to use by delegating to IntelliJ's built-in GitHub plugin.
 *
 * When multiple accounts are configured and none is the project default, shows
 * [GithubChooseAccountDialog] so the user can pick one (and optionally set it as default).
 */
class GithubAccountResolver : IGithubAccountResolver {

    override fun resolveAccount(project: Project): GithubAccountInfo? {
        val account = chooseAccount(project) ?: return null
        val token = runBlocking {
            ApplicationManager.getApplication().getService(GHAccountManager::class.java)
                .findCredentials(account)
        } ?: return null
        val apiBaseUrl = account.server.toApiUrl()
        return GithubAccountInfo(token, apiBaseUrl, account.name)
    }

    private fun chooseAccount(project: Project): GithubAccount? {
        val accounts = GHAccountsUtil.accounts
        return when {
            accounts.isEmpty() -> null
            accounts.size == 1 -> accounts.first()
            else -> GHAccountsUtil.getSingleOrDefaultAccount(project) ?: promptAccountChoice(project, accounts)
        }
    }

    private fun promptAccountChoice(project: Project, accounts: Set<GithubAccount>): GithubAccount? {
        var selected: GithubAccount? = null
        ApplicationManager.getApplication().invokeAndWait {
            val dialog = GithubChooseAccountDialog(
                project, null,
                accounts, null, false, true,
                "Choose GitHub Account", "Connect"
            )
            DialogManager.show(dialog)
            if (dialog.isOK) {
                selected = dialog.account
                if (dialog.setDefault) {
                    GHAccountsUtil.setDefaultAccount(project, dialog.account)
                }
            }
        }
        return selected
    }
}
