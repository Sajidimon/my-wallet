package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.WalletViewModel

@Composable
fun SecurityLockScreen(
    viewModel: WalletViewModel,
    onSuccess: () -> Unit
) {
    val pinProtected by viewModel.pinProtected.collectAsState()
    var enteredPin by remember { mutableStateOf("") }
    var triggerShake by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    if (!pinProtected) {
        SideEffect { onSuccess() }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027), // deep slate dark
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Icon(
                imageVector = if (errorMessage != null) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Lock State",
                tint = if (errorMessage != null) Color(0xFFE74C3C) else Color(0xFF2ECC71),
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 8.dp)
            )

            Text(
                text = "Secure Lock",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Enter your 4-digit PIN to access Wallet",
                color = Color.LightGray,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp),
                textAlign = TextAlign.Center
            )

            // PIN Dot indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                for (i in 1..4) {
                    val active = enteredPin.length >= i
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (active) Color(0xFF1ABC9C) else Color.White.copy(alpha = 0.2f))
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color(0xFFE74C3C),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Keypad Grid
            Column(
                modifier = Modifier.widthIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "delete")
                )

                for (row in keys) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (key in row) {
                            if (key.isEmpty()) {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                KeyButton(
                                    label = key,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .testTag("pin_key_$key"),
                                    onClick = {
                                        if (key == "delete") {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                                errorMessage = null
                                            }
                                        } else {
                                            if (enteredPin.length < 4) {
                                                enteredPin += key
                                                errorMessage = null
                                            }
                                            
                                            if (enteredPin.length == 4) {
                                                val verified = viewModel.verifyPin(enteredPin)
                                                if (verified) {
                                                    onSuccess()
                                                } else {
                                                    errorMessage = "Incorrect PIN code. Try again."
                                                    enteredPin = ""
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { onClick() }
    ) {
        if (label == "delete") {
            Icon(
                imageVector = Icons.Default.Backspace,
                contentDescription = "Backspace",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = label,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
