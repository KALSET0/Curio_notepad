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
        // Modern lateral motion: incoming screen slides the full width while
        // the outgoing one drifts a third and fades (parallax both ways).
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                slideInHorizontally(animationSpec = tween(300)) { it }
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) +
                slideOutHorizontally(animationSpec = tween(300)) { -it / 3 }
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) +
                slideInHorizontally(animationSpec = tween(300)) { -it / 3 }
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) +
                slideOutHorizontally(animationSpec = tween(300)) { it }
        }
    ) {
        composable<CurioRoute.Splash>(
            // Branding hands over with a calm fade, no slide.
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { fadeIn(animationSpec = tween(200)) },
            popExitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
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
