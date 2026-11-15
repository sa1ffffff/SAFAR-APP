package com.safar.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safar.app.ui.theme.Safar
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

val TopBarHeight = 64.dp

/**
 * Secondary-screen scaffold: content scrolls under a floating glass top bar,
 * and an optional bottom action area floats on glass above the nav bar.
 * [content] receives the padding it must leave for the bars.
 */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    bottomBarHeight: androidx.compose.ui.unit.Dp = 88.dp,
    content: @Composable BoxScope.(PaddingValues) -> Unit,
) {
    val haze = remember { HazeState() }
    val status = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val padding = PaddingValues(
        top = status + TopBarHeight,
        bottom = nav + if (bottomBar != null) bottomBarHeight else 16.dp,
    )
    CompositionLocalProvider(LocalHaze provides haze) {
        Box(modifier.fillMaxSize().background(Safar.colors.background).imePadding()) {
            // Only the bars blur; glass inside the content must not nest inside the haze source.
            Box(Modifier.fillMaxSize().haze(haze)) {
                CompositionLocalProvider(LocalHaze provides null) { content(padding) }
            }
            TopBar(title, onBack, actions, Modifier.align(Alignment.TopCenter))
            if (bottomBar != null) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) { bottomBar() }
            }
        }
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit, modifier: Modifier = Modifier) {
    val c = Safar.colors
    Row(
        modifier.fillMaxWidth().statusBarsPadding().height(TopBarHeight).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.width(96.dp)) {
            if (onBack != null) GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            if (title.isNotEmpty()) {
                Glass(shape = RoundedCornerShape(50), elevation = 6.dp) {
                    Text(
                        title, style = Safar.type.bodyStrong, color = c.content, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
        Row(Modifier.width(96.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), content = actions)
    }
}

/** Floating glass action bar for a screen's primary action. */
@Composable
fun BottomActionBar(content: @Composable RowScope.() -> Unit) {
    Glass(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp).navigationBarsPadding().padding(bottom = 8.dp),
        shape = RoundedCornerShape(26.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}
