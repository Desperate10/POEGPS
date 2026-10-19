package com.poe.poegps.feature.presentation.screens.main

import androidx.compose.foundation.layout.statusBarsPadding
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poe.poegps.core.designsystem.PoeIcons
import com.poe.poegps.core.designsystem.PoeShapes
import com.poe.poegps.core.designsystem.PoeTheme

/** What the login form shows; null in [MainViewModel.login] means no login is needed. */
data class LoginState(val busy: Boolean = false, val error: String? = null)

/**
 * B1 · Вхід: branch, login and password over the brand. Shown over the objects screen when the server
 * says the token is missing or expired. Errors (not designed yet) appear under the fields.
 */
@Composable
fun LoginScreen(
    state: LoginState,
    filials: List<Filial>,
    savedFilial: String?,
    onSubmit: (filial: String, login: String, password: String) -> Unit,
    onExit: () -> Unit,
) {
    val c = PoeTheme.colors
    var filialCode by rememberSaveable { mutableStateOf(savedFilial?.takeIf { it != "00" }) }
    var login by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var pickFilial by rememberSaveable { mutableStateOf(false) }
    // the saved branch arrives from storage a moment after the form appears
    LaunchedEffect(savedFilial) {
        if (filialCode == null) filialCode = savedFilial?.takeIf { it != "00" }
    }
    val filial = filials.firstOrNull { it.code == filialCode }
    val missing = when {
        filial == null -> "Оберіть філію"
        login.isBlank() -> "Вкажіть логін"
        password.isEmpty() -> "Вкажіть пароль"
        else -> null
    }
    val submit = { if (missing == null && !state.busy) onSubmit(filial!!.code, login.trim(), password) }

    LightStatusBarIcons()
    // no way around the login: back leaves the app, as the old dialog did
    BackHandler(onBack = onExit)
    Column(
        Modifier
            .fillMaxSize()
            .background(c.action)
            .pointerInput(Unit) { detectTapGestures { } }
            .imePadding(),
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "49.5883° N · 34.5514° E",
                style = PoeTheme.type.monoSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 1.4.sp),
                color = c.onAction,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "POE\nGPS",
                    style = PoeTheme.type.display.copy(fontSize = 92.sp, lineHeight = 80.sp),
                    color = c.onAction,
                    maxLines = 2,
                )
                Text(
                    "Польова зйомка мережі Полтаваобленерго",
                    style = PoeTheme.type.body.copy(fontSize = 16.sp),
                    color = c.onAction,
                    modifier = Modifier.widthIn(max = 260.dp),
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(PoeShapes.sheet)
                .background(c.surface)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LoginField("Філія") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clip(PoeShapes.tile)
                        .background(c.surfaceMuted)
                        .clickable(role = Role.Button, onClickLabel = "Обрати філію") { pickFilial = true }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(filial?.code ?: "—", style = PoeTheme.type.monoStrong.copy(fontSize = 15.sp), color = c.actionText)
                    Text(
                        filial?.name ?: "Оберіть філію",
                        style = PoeTheme.type.input, color = if (filial != null) c.ink else c.muted,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    Icon(PoeIcons.ChevronDown, null, tint = c.muted, modifier = Modifier.size(18.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LoginField("Логін", Modifier.weight(1f)) {
                    LoginInput(login, { login = it }, KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next))
                }
                LoginField("Пароль", Modifier.weight(1f)) {
                    LoginInput(
                        password, { password = it },
                        KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Go),
                        password = true,
                        onGo = submit,
                    )
                }
            }
            state.error?.let { error ->
                Row(
                    Modifier.fillMaxWidth().clip(PoeShapes.tile).background(c.actionContainer).padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(PoeIcons.Alert, null, tint = c.onActionContainer, modifier = Modifier.size(18.dp))
                    Text(error, style = PoeTheme.type.captionRegular, color = c.onActionContainer)
                }
            }
            Row(
                Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(62.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (missing == null) c.ink else c.surfaceMuted)
                    .clickable(enabled = missing == null && !state.busy, role = Role.Button, onClick = submit)
                    .semantics { contentDescription = missing ?: "Увійти" }
                    .padding(start = 22.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when {
                        state.busy -> "Вхід…"
                        missing != null -> missing
                        else -> "Увійти"
                    },
                    style = PoeTheme.type.cardTitle, color = if (missing == null) c.surface else c.muted,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(if (missing == null) c.action else c.outlineVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(color = c.onAction, strokeWidth = 2.5.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(PoeIcons.ChevronRight, null, tint = c.onAction, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Text(
                "Філію можна змінити пізніше в профілі",
                style = PoeTheme.type.captionRegular, color = c.muted,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
            )
        }
    }

    if (pickFilial) {
        FilialDialog(
            filials = filials,
            selected = filialCode,
            dismissible = true,
            onSelect = { filialCode = it; pickFilial = false },
            onDismiss = { pickFilial = false },
        )
    }
}

@Composable
private fun LoginField(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = PoeTheme.type.caption, color = PoeTheme.colors.muted)
        content()
    }
}

@Composable
private fun LoginInput(
    value: String,
    onValueChange: (String) -> Unit,
    keyboard: KeyboardOptions,
    password: Boolean = false,
    onGo: () -> Unit = {},
) {
    val c = PoeTheme.colors
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = PoeTheme.type.input.copy(color = c.ink, letterSpacing = if (password) 2.sp else 0.sp),
        cursorBrush = SolidColor(c.action),
        keyboardOptions = keyboard.copy(autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(PoeShapes.tile)
                    .background(if (focused) c.surface else c.surfaceMuted)
                    .then(if (focused) Modifier.border(2.dp, c.ink, PoeShapes.tile) else Modifier)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) { inner() }
        },
    )
}

/** White status-bar icons while this is shown (the login is orange under them); the previous look comes back after. */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, view)
        val before = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = before }
    }
}

// Hilt wraps a fragment's context, so the activity is found by unwrapping
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
