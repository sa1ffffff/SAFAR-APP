package com.safar.app.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.database.DatabaseException
import com.safar.app.AppContainer
import com.safar.app.container
import com.safar.app.data.TransactionAbortedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Base for screen ViewModels: one-off messages for the snackbar and guarded actions. */
abstract class SafarViewModel : ViewModel() {
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    protected fun say(message: String) {
        _messages.trySend(message)
    }

    /** Runs [block], turning failures into a readable message instead of a crash. */
    protected fun action(
        onDone: () -> Unit = {},
        onError: (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit,
    ) = viewModelScope.launch {
        try {
            block()
            onDone()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(e)
            say(e.friendlyMessage())
        }
    }
}

fun Throwable.friendlyMessage(): String = when (this) {
    is FirebaseNetworkException -> "You're offline. Check your connection and try again."
    is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a few minutes and try again."
    is FirebaseAuthInvalidCredentialsException -> "That code doesn't look right. Check it and try again."
    is FirebaseAuthRecentLoginRequiredException -> "For your security, sign out and back in, then try again."
    is TransactionAbortedException -> message ?: "Please try again."
    is com.safar.app.data.SafarApi.ApiException -> if (status == 413 || status == 415) message.orEmpty() else "Couldn't reach Safar's servers. Please try again."
    is java.io.IOException -> "You're offline. Check your connection and try again."
    is DatabaseException -> if (message?.contains("Permission denied", true) == true)
        "You don't have permission to do that." else "Something went wrong. Please try again."
    is IllegalArgumentException, is IllegalStateException -> message ?: "Something went wrong."
    else -> message?.takeIf { it.length < 120 } ?: "Something went wrong. Please try again."
}

val LocalSnackbar = staticCompositionLocalOf { SnackbarHostState() }

/** Shows a ViewModel's one-off messages in the app snackbar. */
@Composable
fun Messages(vm: SafarViewModel) {
    val snackbar = LocalSnackbar.current
    LaunchedEffect(vm) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }
}

/** Creates a ViewModel scoped to the current navigation entry, with access to the app container. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(key: String? = null, crossinline create: (AppContainer) -> VM): VM {
    val container = LocalContext.current.container
    return viewModel(key = key) { create(container) }
}
