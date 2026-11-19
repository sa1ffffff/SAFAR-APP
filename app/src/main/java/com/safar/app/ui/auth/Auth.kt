package com.safar.app.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.safar.app.AppContainer
import com.safar.app.R
import com.safar.app.data.model.User
import com.safar.app.ui.Messages
import com.safar.app.ui.Motion
import com.safar.app.ui.SafarViewModel
import com.safar.app.ui.appViewModel
import com.safar.app.ui.components.Field
import com.safar.app.ui.components.SafarButton
import com.safar.app.ui.components.initials
import com.safar.app.ui.morph
import com.safar.app.ui.theme.Radii
import com.safar.app.ui.theme.Safar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

fun Context.findActivity(): Activity {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    error("No activity")
}

/** Turns what the user typed into E.164, defaulting to Pakistan (+92). Null if it isn't a plausible mobile number. */
fun normalizePhone(countryCode: String, raw: String): String? {
    val code = countryCode.filter { it.isDigit() }
    var digits = raw.filter { it.isDigit() }
    if (digits.startsWith(code) && digits.length > 10) digits = digits.removePrefix(code)
    digits = digits.trimStart('0')
    val valid = if (code == "92") digits.length == 10 && digits.startsWith("3") else digits.length in 6..14
    return if (valid && code.isNotEmpty()) "+$code$digits" else null
}

/** Shared key: the pickup ring on sign-in becomes the avatar on the phone step. */
private const val PICKUP_KEY = "pickup"

// ─── Sign in ─────────────────────────────────────────────────────────────────

class SignInViewModel(private val app: AppContainer) : SafarViewModel() {
    val busy = MutableStateFlow(false)

    fun signInWithGoogle(activity: Activity) {
        if (busy.value) return
        busy.value = true
        action(onDone = { busy.value = false }, onError = { busy.value = false }) {
            val option = GetSignInWithGoogleOption.Builder(activity.getString(R.string.google_web_client_id)).build()
            val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
            val credential = try {
                CredentialManager.create(activity).getCredential(activity, request).credential
            } catch (e: GetCredentialCancellationException) {
                return@action
            } catch (e: NoCredentialException) {
                throw IllegalStateException("No Google account on this phone. Add one in Settings → Accounts, then try again.")
            }
            if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                throw IllegalStateException("Google sign-in didn't return an account. Please try again.")
            }
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            app.auth.signIn(GoogleAuthProvider.getCredential(idToken, null))
        }
    }
}

@Composable
fun SignInScreen() {
    val vm = appViewModel { SignInViewModel(it) }
    Messages(vm)
    val c = Safar.colors
    val activity = LocalContext.current.findActivity()
    val busy by vm.busy.collectAsStateWithLifecycle()

    // Choreography: the route draws first, then the words and the action rise in, one beat apart.
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(650); revealed = true }

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding()) {
        RouteHero(Modifier.fillMaxWidth().weight(1f))
        Column(Modifier.padding(horizontal = 24.dp)) {
            Rise(revealed, 0) {
                Text("Safar", style = Safar.type.display.copy(fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-1.6).sp), color = c.content)
            }
            Spacer(Modifier.height(8.dp))
            Rise(revealed, 1) {
                Text("Share the ride. Split the fuel.\nMeet people going your way.", style = Safar.type.body.copy(fontSize = 17.sp, lineHeight = 24.sp), color = c.contentSecondary)
            }
            Spacer(Modifier.height(36.dp))
            Rise(revealed, 2) {
                GoogleButton(loading = busy, onClick = { vm.signInWithGoogle(activity) })
            }
            Rise(revealed, 3) {
                Text(
                    "Next we'll ask for your phone number, so the people you ride with can reach you at pickup.",
                    style = Safar.type.caption, color = c.contentTertiary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 20.dp),
                )
            }
        }
    }
}

/** Staggered rise-in: each item arrives 70ms after the previous one, settling with a strong ease-out. */
@Composable
private fun Rise(visible: Boolean, index: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(Motion.MEDIUM, delayMillis = index * 70, easing = Motion.Enter)) +
            slideInVertically(tween(Motion.LONG, delayMillis = index * 70, easing = Motion.Enter)) { it / 3 },
    ) { content() }
}

/**
 * The hero: faint streets, then a route that draws itself from a pickup ring to a
 * drop-off square, then a small car that keeps travelling it, slowly, on a loop.
 * The pickup ring is a real composable so it can morph into the avatar on the next stage.
 */
@Composable
private fun RouteHero(modifier: Modifier) {
    val c = Safar.colors
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) { draw.animateTo(1f, tween(1300, delayMillis = 150, easing = Motion.Glide)) }
    val drawn = draw.value >= 0.999f
    val loop = rememberInfiniteTransition(label = "car")
    val carT by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = Motion.Glide), RepeatMode.Restart), label = "car-t")
    val dropScale = remember { Animatable(0f) }
    LaunchedEffect(drawn) {
        if (drawn) dropScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
    }

    // Path anchors as fractions of the hero, so it scales to any screen.
    val start = Offset(0.16f, 0.82f)
    val end = Offset(0.82f, 0.22f)

    val density = androidx.compose.ui.platform.LocalDensity.current
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        Canvas(Modifier.fillMaxSize()) {
            val street = c.content.copy(alpha = if (c.isDark) 0.07f else 0.05f)
            // A loose street grid: a few long lines at slight angles reads as "map" without detail.
            listOf(0.18f, 0.46f, 0.74f).forEach { y ->
                drawLine(street, Offset(-20f, size.height * y), Offset(size.width + 20f, size.height * (y - 0.08f)), strokeWidth = 10.dp.toPx())
            }
            listOf(0.30f, 0.62f, 0.90f).forEach { x ->
                drawLine(street, Offset(size.width * x, -20f), Offset(size.width * (x - 0.10f), size.height + 20f), strokeWidth = 8.dp.toPx())
            }

            val path = Path().apply {
                moveTo(size.width * start.x, size.height * start.y)
                cubicTo(
                    size.width * 0.30f, size.height * 0.40f,
                    size.width * 0.64f, size.height * 0.78f,
                    size.width * end.x, size.height * end.y,
                )
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * draw.value, partial, true)
            drawPath(partial, c.content, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))

            // Drop-off square pops in once the route reaches it.
            val sq = 18.dp.toPx() * dropScale.value
            if (sq > 0f) {
                drawRoundRect(
                    c.content,
                    topLeft = Offset(size.width * end.x - sq / 2, size.height * end.y - sq / 2),
                    size = Size(sq, sq),
                    cornerRadius = CornerRadius(4.dp.toPx() * dropScale.value),
                )
            }

            // The car: a soft dot that eases along the route, fading at both ends of each lap.
            if (drawn) {
                val p = measure.getPosition(measure.length * carT)
                val fade = (minOf(carT, 1f - carT) * 8f).coerceIn(0f, 1f)
                drawCircle(c.content.copy(alpha = 0.12f * fade), radius = 14.dp.toPx(), center = p)
                drawCircle(c.background.copy(alpha = fade), radius = 6.dp.toPx(), center = p)
                drawCircle(c.content.copy(alpha = fade), radius = 6.dp.toPx(), center = p, style = Stroke(2.5.dp.toPx()))
            }
        }
        // Pickup ring, positioned on the path start. Morphs into the avatar on the phone step.
        val ring = 22.dp
        Box(
            Modifier
                .offset(
                    x = with(density) { (w * start.x).toDp() } - ring / 2,
                    y = with(density) { (h * start.y).toDp() } - ring / 2,
                )
                .morph(PICKUP_KEY)
                .size(ring)
                .clip(CircleShape)
                .background(c.background)
                .border(3.5.dp, c.content, CircleShape)
        )
    }
}

@Composable
private fun GoogleButton(loading: Boolean, onClick: () -> Unit) {
    Box {
        SafarButton("Continue with Google", onClick = onClick, loading = loading, height = 58.dp, modifier = Modifier.fillMaxWidth())
        if (!loading) {
            // The "G" sits on a white disc so it reads on both the black (light theme) and white (dark theme) button.
            Box(
                Modifier.align(Alignment.CenterStart).padding(start = 14.dp).size(32.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_google), null, tint = Color.Unspecified, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ─── Phone step (and name, if Google didn't give one) ───────────────────────

class ProfileSetupViewModel(private val app: AppContainer) : SafarViewModel() {
    private val uid = app.auth.currentUid.orEmpty()
    val name = MutableStateFlow(app.auth.displayName)
    val photo = MutableStateFlow<Any?>(app.auth.photoUrl)
    val countryCode = MutableStateFlow("+92")
    val digits = MutableStateFlow("")
    val saving = MutableStateFlow(false)

    /** True once Google (or an earlier profile) gave us a name, so the screen can ask for the number alone. */
    val nameKnown = name.value.trim().length >= 2

    init {
        // A returning user who only lacks a phone keeps their saved name and photo.
        action {
            val existing = app.users.get(uid) ?: return@action
            if (existing.name.isNotBlank()) name.value = existing.name
            if (!existing.photoUrl.isNullOrBlank()) photo.value = existing.photoUrl
        }
    }

    fun e164(): String? = normalizePhone(countryCode.value, digits.value)

    fun save() {
        val trimmed = name.value.trim()
        val phone = e164()
        when {
            trimmed.length < 2 -> { say("Add your name so riders know who you are."); return }
            phone == null -> { say("That doesn't look like a mobile number. Check it and try again."); return }
        }
        saving.value = true
        action(onError = { saving.value = false }) {
            val existing = app.users.get(uid)
            val base = existing ?: User(uid = uid, joinedAt = System.currentTimeMillis())
            var user = base.copy(
                uid = uid,
                name = trimmed,
                phone = phone!!,
                email = base.email.ifBlank { app.auth.email },
                photoUrl = base.photoUrl ?: (photo.value as? String),
                authProvider = "google",
                joinedAt = base.joinedAt.takeIf { it > 0 } ?: System.currentTimeMillis(),
            )
            // Saving completes the profile; the gate then moves the app on by itself.
            // The photo uploads afterwards, so a storage outage can't block sign-up.
            app.users.save(user)
            (photo.value as? Uri)?.let { uri ->
                runCatching { app.users.uploadAvatar(uid, uri) }.onFailure {
                    app.messages.tryEmit("Your photo couldn't upload. Add it later from Edit profile.")
                }
            }
        }
    }

    fun signOut() = app.auth.signOut()
}

@Composable
fun ProfileSetupScreen() {
    val vm = appViewModel { ProfileSetupViewModel(it) }
    Messages(vm)
    val c = Safar.colors
    val name by vm.name.collectAsStateWithLifecycle()
    val photo by vm.photo.collectAsStateWithLifecycle()
    val code by vm.countryCode.collectAsStateWithLifecycle()
    val digits by vm.digits.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val valid = normalizePhone(code, digits) != null && name.trim().length >= 2
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.photo.value = uri
    }
    val phoneFocus = remember { FocusRequester() }
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(360) // let the avatar finish its morph before the form arrives
        revealed = true
        if (vm.nameKnown) runCatching { phoneFocus.requestFocus() }
    }

    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding().imePadding()
            .padding(horizontal = 24.dp)
    ) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(40.dp))
            Box(
                Modifier.size(96.dp).clickable(remember { MutableInteractionSource() }, indication = null) {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
            ) {
                Box(Modifier.morph(PICKUP_KEY).size(96.dp).clip(CircleShape).background(c.surfaceMuted), contentAlignment = Alignment.Center) {
                    // Initials sit underneath so the circle is never blank while the photo loads (or if it can't).
                    Text(initials(name), style = Safar.type.title, color = c.content)
                    if (photo != null) AsyncImage(photo, "Your photo", Modifier.size(96.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = revealed, modifier = Modifier.align(Alignment.BottomEnd),
                    enter = scaleIn(spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(Motion.SHORT)),
                ) {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(c.primary).border(3.dp, c.background, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.PhotoCamera, "Change photo", Modifier.size(15.dp), tint = c.onPrimary) }
                }
            }
            Spacer(Modifier.height(28.dp))
            Rise(revealed, 0) {
                Text(
                    if (vm.nameKnown) "Salam, ${name.trim().substringBefore(' ')}" else "Welcome to Safar",
                    style = Safar.type.display, color = c.content,
                )
            }
            Spacer(Modifier.height(10.dp))
            Rise(revealed, 1) {
                Text(
                    "One last thing: your mobile number, so the driver or riders on your trip can call you at pickup.",
                    style = Safar.type.body, color = c.contentSecondary,
                )
            }
            Spacer(Modifier.height(28.dp))
            if (!vm.nameKnown) {
                Rise(revealed, 2) {
                    Field(
                        value = name, onValueChange = { vm.name.value = it.take(40) }, label = "Your name", placeholder = "Ayesha Khan",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { runCatching { phoneFocus.requestFocus() } }),
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
            Rise(revealed, 3) {
                PhoneInput(
                    code = code, digits = digits,
                    onCode = { vm.countryCode.value = "+" + it.filter(Char::isDigit).take(3) },
                    onDigits = { vm.digits.value = it.filter(Char::isDigit).take(12) },
                    onDone = { if (valid) vm.save() },
                    focus = phoneFocus,
                )
            }
            Spacer(Modifier.height(14.dp))
            Rise(revealed, 4) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Lock, null, Modifier.size(15.dp), tint = c.contentTertiary)
                    Text("Riders on your rides can call or WhatsApp you on this number.", style = Safar.type.caption, color = c.contentTertiary)
                }
            }
        }
        SafarButton("Start riding", onClick = vm::save, enabled = valid, loading = saving, height = 58.dp, modifier = Modifier.fillMaxWidth())
        Text(
            "Use a different Google account", style = Safar.type.label, color = c.contentSecondary, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(CircleShape).clickable { vm.signOut() }.padding(12.dp),
        )
    }
}

/**
 * One field that reads like a single number: an editable country code, then digits
 * grouped as you type ("300 123 4567"). A check eases in once the number is valid;
 * the border turns red only after a full-length number that still doesn't fit.
 */
@Composable
private fun PhoneInput(
    code: String, digits: String,
    onCode: (String) -> Unit, onDigits: (String) -> Unit, onDone: () -> Unit,
    focus: FocusRequester,
) {
    val c = Safar.colors
    val ok = normalizePhone(code, digits) != null
    val wrong = !ok && code == "+92" && digits.trimStart('0').length >= 10
    val border by animateColorAsState(
        when { wrong -> c.negative; ok -> c.content; else -> Color.Transparent }, tween(Motion.MEDIUM), label = "phone-border",
    )
    val big = Safar.type.title.copy(fontSize = 24.sp, letterSpacing = 0.5.sp)
    Column {
        Row(
            Modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(Radii.lg)).background(c.surfaceMuted)
                .border(1.5.dp, border, RoundedCornerShape(Radii.lg)).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = code, onValueChange = onCode, singleLine = true,
                textStyle = big.copy(color = c.contentSecondary),
                cursorBrush = SolidColor(c.content),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                modifier = Modifier.width(64.dp),
            )
            Box(Modifier.width(1.dp).height(28.dp).background(c.border))
            Spacer(Modifier.width(14.dp))
            BasicTextField(
                value = digits, onValueChange = onDigits, singleLine = true,
                textStyle = big.copy(color = c.content),
                cursorBrush = SolidColor(c.content),
                visualTransformation = if (code == "+92") PkGrouping else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDone() }),
                modifier = Modifier.weight(1f).focusRequester(focus),
                decorationBox = { inner ->
                    Box {
                        if (digits.isEmpty()) Text("300 123 4567", style = big, color = c.contentTertiary)
                        inner()
                    }
                },
            )
            AnimatedVisibility(
                ok,
                enter = scaleIn(spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.4f) + fadeIn(tween(Motion.SHORT)),
                exit = scaleOut(tween(Motion.SHORT, easing = Motion.Exit), targetScale = 0.6f) + fadeOut(tween(Motion.SHORT)),
            ) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(c.positive), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, "Valid number", Modifier.size(17.dp), tint = Color.White)
                }
            }
        }
        AnimatedVisibility(wrong, enter = fadeIn(tween(Motion.SHORT)), exit = fadeOut(tween(Motion.SHORT))) {
            Text(
                "Pakistani mobile numbers start with 3, like 300 123 4567.",
                style = Safar.type.caption, color = c.negative, modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
    }
}

/** Groups Pakistani mobile digits as 3-3-4 ("3001234567" → "300 123 4567"), keeping the cursor in step. */
private object PkGrouping : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val out = buildString {
            raw.forEachIndexed { i, ch ->
                if (i == 3 || i == 6) append(' ')
                append(ch)
            }
        }
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + (if (offset > 3) 1 else 0) + (if (offset > 6) 1 else 0)
            override fun transformedToOriginal(offset: Int): Int =
                (offset - (if (offset > 4) 1 else 0) - (if (offset > 8) 1 else 0)).coerceIn(0, raw.length)
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

