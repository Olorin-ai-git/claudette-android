package com.olorin.claudette.ui.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.olorin.claudette.ui.screens.profiles.ProfileEditorScreen
import com.olorin.claudette.ui.screens.profiles.ProfileListScreen
import com.olorin.claudette.ui.screens.session.SessionScreen
import com.olorin.claudette.ui.screens.splash.SplashScreen
import java.net.URLDecoder
import java.net.URLEncoder

object NavRoutes {
    const val SPLASH = "splash"
    const val PROFILE_LIST = "profile_list"
    const val PROFILE_EDITOR = "profile_editor/{profileId}"
    const val PROFILE_EDITOR_NEW = "profile_editor/new"
    const val SESSION = "session/{profileId}/{encodedPath}"

    fun profileEditor(profileId: String) = "profile_editor/$profileId"
    fun session(profileId: String, projectPath: String): String {
        val encodedPath = URLEncoder.encode(projectPath, "UTF-8")
        return "session/$profileId/$encodedPath"
    }
}

@Composable
fun ClaudetteNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavRoutes.SPLASH) {
        composable(NavRoutes.SPLASH) {
            SplashScreen(
                onSplashComplete = {
                    navController.navigate(NavRoutes.PROFILE_LIST) {
                        popUpTo(NavRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.PROFILE_LIST) {
            ProfileListScreen(
                onAddProfile = {
                    navController.navigate(NavRoutes.PROFILE_EDITOR_NEW)
                },
                onEditProfile = { profileId ->
                    navController.navigate(NavRoutes.profileEditor(profileId))
                },
                onConnect = { profileId, path ->
                    navController.navigate(NavRoutes.session(profileId, path))
                }
            )
        }

        composable(
            NavRoutes.PROFILE_EDITOR,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: "new"
            ProfileEditorScreen(
                profileId = profileId,
                onSaved = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            NavRoutes.SESSION,
            arguments = listOf(
                navArgument("profileId") { type = NavType.StringType },
                navArgument("encodedPath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: return@composable
            val encodedPath = backStackEntry.arguments?.getString("encodedPath") ?: return@composable
            val projectPath = URLDecoder.decode(encodedPath, "UTF-8")

            SessionScreen(
                profileId = profileId,
                projectPath = projectPath,
                onDisconnect = { navController.popBackStack() }
            )
        }
    }
}
