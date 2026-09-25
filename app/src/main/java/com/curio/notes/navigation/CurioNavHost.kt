package com.curio.notes.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.curio.notes.ui.conversation.ConversationScreen
import com.curio.notes.ui.home.HomeScreen
import com.curio.notes.ui.note.CreateNoteScreen
import com.curio.notes.ui.note.NoteDetailScreen
import com.curio.notes.ui.search.SearchScreen
import com.curio.notes.ui.settings.SettingsScreen
import com.curio.notes.ui.splash.SplashScreen

@Composable
fun CurioNavHost() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = CurioRoute.Splash,
        // Short, calm transitions everywhere: slight horizontal drift + fade.
        enterTransition = {
            fadeIn(animationSpec = tween(250)) +
                slideInHorizontally(animationSpec = tween(250)) { it / 8 }
        },
        exitTransition = { fadeOut(animationSpec = tween(200)) },
        popEnterTransition = { fadeIn(animationSpec = tween(250)) },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) +
                slideOutHorizontally(animationSpec = tween(250)) { it / 8 }
        }
    ) {
        composable<CurioRoute.Splash> {
            SplashScreen(
                onDone = {
                    navController.navigate(CurioRoute.Home) {
                        popUpTo(CurioRoute.Splash) { inclusive = true }
                    }
                }
            )
        }
        composable<CurioRoute.Home> {
            HomeScreen(
                onCreateNote = { navController.navigate(CurioRoute.CreateNote) },
                onOpenNote = { noteId -> navController.navigate(CurioRoute.NoteDetail(noteId)) },
                onOpenSearch = { navController.navigate(CurioRoute.Search) },
                onOpenSettings = { navController.navigate(CurioRoute.Settings) }
            )
        }
        composable<CurioRoute.CreateNote> {
            CreateNoteScreen(onBack = { navController.popBackStack() })
        }
        composable<CurioRoute.NoteDetail> { backStackEntry ->
            val route: CurioRoute.NoteDetail = backStackEntry.toRoute()
            NoteDetailScreen(
                noteId = route.noteId,
                onBack = { navController.popBackStack() },
                onContinueWithAI = { navController.navigate(CurioRoute.Conversation(route.noteId)) }
            )
        }
        composable<CurioRoute.Conversation> { backStackEntry ->
            val route: CurioRoute.Conversation = backStackEntry.toRoute()
            ConversationScreen(
                noteId = route.noteId,
                onBack = { navController.popBackStack() }
            )
        }
        composable<CurioRoute.Search> {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onOpenNote = { noteId -> navController.navigate(CurioRoute.NoteDetail(noteId)) }
            )
        }
        composable<CurioRoute.Settings> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
