package com.edge.smartboard.ui.login

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edge.smartboard.theme.*
import com.edge.smartboard.viewmodel.AuthState
import com.edge.smartboard.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val authState  by viewModel.authState.collectAsStateWithLifecycle()
    val savedUrl   by viewModel.serverUrl.collectAsStateWithLifecycle()

    var serverUrl  by remember(savedUrl) { mutableStateOf(savedUrl) }
    var email      by remember { mutableStateOf("") }
    var password   by remember { mutableStateOf("") }
    var showPw     by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var errorMsg   by remember { mutableStateOf("") }

    // Surface / text tokens that work in both themes
    val bg      = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val onBg    = MaterialTheme.colorScheme.onBackground
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    LaunchedEffect(authState) {
        when (val s = authState) {
            is AuthState.Success -> onLoginSuccess()
            is AuthState.Error   -> { errorMsg = s.message; viewModel.resetState() }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(56.dp))

            // ── Logo ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                "EDGE SMARTBOARD",
                style = MaterialTheme.typography.headlineMedium,
                color = primary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
            Text(
                "AI",
                style = MaterialTheme.typography.displaySmall,
                color = onBg,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Enterprise Classroom Intelligence Platform",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(36.dp))

            // ── Server URL card ───────────────────────────────
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Dns,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Backend Server",
                            style = MaterialTheme.typography.labelMedium,
                            color = primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        placeholder = { Text("http://192.168.x.x:8000/", style = MaterialTheme.typography.bodySmall) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.saveServerUrl(serverUrl) }) {
                                Icon(Icons.Default.Save, null, tint = primary, modifier = Modifier.size(18.dp))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = primary,
                            unfocusedBorderColor = outline,
                            focusedTextColor     = onBg,
                            unfocusedTextColor   = onBg,
                            cursorColor          = primary
                        )
                    )
                    Text(
                        "Tap 💾 to save. Leave empty to use Demo Mode.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Login card ────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Sign In",
                        style = MaterialTheme.typography.headlineSmall,
                        color = onBg,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Enter your credentials to continue",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Email
                    CleanTextField(
                        value = email,
                        onValueChange = { email = it; errorMsg = "" },
                        label = "Email Address",
                        leadingIcon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )

                    // Password
                    CleanTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = "" },
                        label = "Password",
                        leadingIcon = Icons.Default.Lock,
                        isPassword = true,
                        showPassword = showPw,
                        onTogglePassword = { showPw = !showPw }
                    )

                    // Remember Me + Forgot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                colors = CheckboxDefaults.colors(checkedColor = primary)
                            )
                            Text(
                                "Remember Me",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = {}) {
                            Text(
                                "Forgot Password?",
                                color = primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Error
                    AnimatedVisibility(visible = errorMsg.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                errorMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    // Login button
                    Button(
                        onClick = {
                            viewModel.saveServerUrl(serverUrl)
                            viewModel.login(email, password)
                        },
                        enabled = email.isNotEmpty() && password.isNotEmpty() && authState !is AuthState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Login, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "LOGIN",
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }

                    // Divider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            "  or  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }

                    // Demo Mode
                    OutlinedButton(
                        onClick = { viewModel.demoLogin() },
                        enabled = authState !is AuthState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.PlayCircle,
                                null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "DEMO MODE",
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "(No server needed)",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "v1.0.0 • Edge Smartboard AI",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(56.dp))
        }
    }
}

@Composable
fun CleanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val onBg    = MaterialTheme.colorScheme.onBackground
    val outline = MaterialTheme.colorScheme.outline

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(leadingIcon, null, tint = primary, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = { onTogglePassword?.invoke() }) {
                    Icon(
                        if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = primary,
            unfocusedBorderColor = outline,
            focusedTextColor     = onBg,
            unfocusedTextColor   = onBg,
            cursorColor          = primary
        )
    )
}

// Keep NeonTextField as alias so other screens don't break
@Composable
fun NeonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) = CleanTextField(value, onValueChange, label, leadingIcon, keyboardType, isPassword, showPassword, onTogglePassword)
