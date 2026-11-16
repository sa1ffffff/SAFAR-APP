package com.safar.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.safar.app.R
import com.safar.app.container
import com.safar.app.data.Gate
import com.safar.app.ui.account.AboutScreen
import com.safar.app.ui.account.EditProfileScreen
import com.safar.app.ui.account.HelpScreen
import com.safar.app.ui.account.SettingsScreen
import com.safar.app.ui.auth.ProfileSetupScreen
import com.safar.app.ui.auth.SignInScreen
import com.safar.app.ui.chat.ChatScreen
import com.safar.app.ui.main.MainScreen
import com.safar.app.ui.notifications.NotificationsScreen
import com.safar.app.ui.onboarding.SafetyScreen
import com.safar.app.ui.onboarding.WelcomeScreen
import com.safar.app.ui.post.LocationPickerScreen
import com.safar.app.ui.post.PostRideScreen
import com.safar.app.ui.profile.UserProfileScreen
import com.safar.app.ui.review.ReviewScreen
import com.safar.app.ui.ride.RideDetailScreen
import com.safar.app.ui.theme.Radii
import com.safar.app.ui.theme.Safar

object Routes {
    const val MAIN = "main"
    const val RIDE = "ride/{id}"
    const val POST = "post?edit={edit}"
    const val PICK = "pick/{field}"
    const val CHAT = "chat/{uid}"
    const val USER = "user/{uid}"
    const val REVIEW = "review/{requestId}"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val EDIT_PROFILE = "profile/edit"
    const val HELP = "help"
    const val ABOUT = "about"
    const val SAFETY = "safety"

    fun ride(id: String) = "ride/$id"
    fun post(editId: String? = null) = if (editId == null) "post" else "post?edit=$editId"
    fun pick(field: String) = "pick/$field"
    fun chat(uid: String) = "chat/$uid"
    fun user(uid: String) = "user/$uid"
    fun review(requestId: String) = "review/$requestId"
}

/** Result key a location picker writes into the previous entry's SavedStateHandle. */
const val PICK_RESULT = "pick_result"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SafarRoot(pendingOpen: String?, onPendingOpenHandled: () -> Unit) {
    val session = LocalContext.current.container.session
    val gate by session.gate.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val appMessages = LocalContext.current.container.messages
    LaunchedEffect(appMessages) { appMessages.collect { snackbar.showSnackbar(it) } }

    CompositionLocalProvider(LocalSnackbar provides snackbar) {
        Box(Modifier.fillMaxSize().background(Safar.colors.background)) {
            SharedTransitionLayout {
                AnimatedContent(
                    targetState = gate.stage(),
                    transitionSpec = { stageTransition(initialState, targetState) },
                    label = "gate",
                ) { stage ->
                    CompositionLocalProvider(LocalSharedScope provides this@SharedTransitionLayout, LocalStageScope provides this) {
                        when (stage) {
                            Stage.Loading -> Splash()
                            Stage.Welcome -> WelcomeScreen()
                            Stage.SignIn -> SignInScreen()
                            Stage.Profile -> ProfileSetupScreen()
                            Stage.App -> AppGraph(pendingOpen, onPendingOpenHandled)
                        }
                    }
                }
            }
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).padding(bottom = 72.dp),
            ) { data ->
                Snackbar(
                    data,
                    shape = RoundedCornerShape(Radii.md),
                    containerColor = Safar.colors.primary,
                    contentColor = Safar.colors.onPrimary,
                )
            }
        }
    }
}

private enum class Stage { Loading, Welcome, SignIn, Profile, App }

private fun Gate.stage() = when (this) {
    Gate.Loading -> Stage.Loading
    Gate.Welcome -> Stage.Welcome
    Gate.SignedOut -> Stage.SignIn
    is Gate.NeedsProfile -> Stage.Profile
    is Gate.Ready -> Stage.App
}

/**
 * Stage changes are the app's big moments, so each has its own character:
 * - into sign-in or profile: a calm cross-fade that lets the shared pickup ring morph;
 * - into the app: the home screen rises from slightly below and grows into place,
 *   like pulling away from the curb, while the profile step recedes;
 * - signing out: the reverse, settling back down.
 */
private fun AnimatedContentTransitionScope<Stage>.stageTransition(from: Stage, to: Stage): ContentTransform = when {
    to == Stage.App && from != Stage.Loading ->
        (fadeIn(tween(Motion.LONG, easing = Motion.Enter)) +
            scaleIn(tween(Motion.LONG + 80, easing = Motion.Enter), initialScale = 0.92f) +
            slideInVertically(tween(Motion.LONG + 80, easing = Motion.Enter)) { it / 12 }) togetherWith
            (fadeOut(tween(Motion.SHORT, easing = Motion.Exit)) + scaleOut(tween(Motion.MEDIUM, easing = Motion.Exit), targetScale = 1.04f))
    from == Stage.App ->
        (fadeIn(tween(Motion.MEDIUM, delayMillis = 60, easing = Motion.Enter)) + scaleIn(tween(Motion.LONG, easing = Motion.Enter), initialScale = 1.04f)) togetherWith
            (fadeOut(tween(Motion.SHORT, easing = Motion.Exit)) + scaleOut(tween(Motion.MEDIUM, easing = Motion.Exit), targetScale = 0.94f))
    else ->
        fadeIn(tween(Motion.MEDIUM, delayMillis = 80, easing = Motion.Enter)) togetherWith fadeOut(tween(Motion.SHORT, easing = Motion.Exit))
}.using(SizeTransform(clip = false))

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize().background(Safar.colors.background), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Image(
            painterResource(R.drawable.ic_launcher_foreground), null,
            Modifier.size(120.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Safar.colors.content),
        )
    }
}

private const val NAV_MS = 280

@Composable
private fun AppGraph(pendingOpen: String?, onPendingOpenHandled: () -> Unit) {
    val nav = rememberNavController()
    LaunchedEffect(pendingOpen) {
        val chatWith = pendingOpen?.removePrefix(com.safar.app.MainActivity.OPEN_CHAT_PREFIX)
            ?.takeIf { pendingOpen.startsWith(com.safar.app.MainActivity.OPEN_CHAT_PREFIX) && it.isNotBlank() }
        when {
            pendingOpen == com.safar.app.MainActivity.OPEN_NOTIFICATIONS -> nav.navigate(Routes.NOTIFICATIONS) { launchSingleTop = true }
            chatWith != null -> nav.navigate(Routes.chat(chatWith)) { launchSingleTop = true }
        }
        if (pendingOpen != null) onPendingOpenHandled()
    }
    NavHost(
        nav, startDestination = Routes.MAIN,
        enterTransition = { slideInHorizontally(tween(NAV_MS, easing = FastOutSlowInEasing)) { it / 4 } + fadeIn(tween(NAV_MS)) },
        exitTransition = { fadeOut(tween(NAV_MS / 2)) },
        popEnterTransition = { fadeIn(tween(NAV_MS)) },
        popExitTransition = { slideOutHorizontally(tween(NAV_MS, easing = FastOutSlowInEasing)) { it / 4 } + fadeOut(tween(NAV_MS / 2)) },
    ) {
        composable(Routes.MAIN) { MainScreen(nav) }
        composable(Routes.RIDE, listOf(navArgument("id") { type = NavType.StringType })) {
            RideDetailScreen(rideId = it.arguments?.getString("id").orEmpty(), nav = nav)
        }
        composable(Routes.POST, listOf(navArgument("edit") { type = NavType.StringType; nullable = true; defaultValue = null })) { entry ->
            PostRideScreen(editId = entry.arguments?.getString("edit"), nav = nav, entry = entry)
        }
        composable(Routes.PICK, listOf(navArgument("field") { type = NavType.StringType })) {
            LocationPickerScreen(field = it.arguments?.getString("field").orEmpty(), nav = nav)
        }
        composable(Routes.CHAT, listOf(navArgument("uid") { type = NavType.StringType })) {
            ChatScreen(otherUid = it.arguments?.getString("uid").orEmpty(), nav = nav)
        }
        composable(Routes.USER, listOf(navArgument("uid") { type = NavType.StringType })) {
            UserProfileScreen(uid = it.arguments?.getString("uid").orEmpty(), nav = nav)
        }
        composable(Routes.REVIEW, listOf(navArgument("requestId") { type = NavType.StringType })) {
            ReviewScreen(requestId = it.arguments?.getString("requestId").orEmpty(), nav = nav)
        }
        composable(Routes.NOTIFICATIONS) { NotificationsScreen(nav) }
        composable(Routes.SETTINGS) { SettingsScreen(nav) }
        composable(Routes.EDIT_PROFILE) { EditProfileScreen(nav) }
        composable(Routes.HELP) { HelpScreen(nav) }
        composable(Routes.ABOUT) { AboutScreen(nav) }
        composable(Routes.SAFETY) { SafetyScreen(onDone = { nav.popBackStack() }) }
    }
}

fun NavHostController.back() {
    if (previousBackStackEntry != null) popBackStack()
}
