package com.aura.launcher

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    prefs: AuraPrefs,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0F0C1E),
                        Color(0xFF1B1730),
                        Color(0xFF0F0C1E)
                    )
                )
            )
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { width -> width } + fadeIn() with
                            slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    slideInHorizontally { width -> -width } + fadeIn() with
                            slideOutHorizontally { width -> width } + fadeOut()
                }.using(SizeTransform(clip = false))
            },
            label = "OnboardingStepAnimation"
        ) { step ->
            when (step) {
                1 -> WelcomeStep(onNext = { currentStep = 2 })
                2 -> PlanStep(
                    prefs = prefs,
                    onBack = { currentStep = 1 },
                    onNext = { currentStep = 3 }
                )
                3 -> GridStep(
                    prefs = prefs,
                    onBack = { currentStep = 2 },
                    onNext = { currentStep = 4 }
                )
                4 -> AiStep(
                    prefs = prefs,
                    onBack = { currentStep = 3 },
                    onNext = { currentStep = 5 }
                )
                5 -> PermissionsStep(
                    onBack = { currentStep = 4 },
                    onNext = { currentStep = 6 }
                )
                6 -> FinishStep(
                    onBack = { currentStep = 5 },
                    onComplete = onComplete
                )
            }
        }

        // Page Indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 1..6) {
                Box(
                    modifier = Modifier
                        .size(if (currentStep == i) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentStep == i) Color(0xFF9D86FF) else Color.White.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Glowing styled Aura logo
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF9D86FF), Color(0xFF6C4DF6), Color.Transparent)
                    )
                )
                .border(2.dp, Color(0xFF9D86FF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "A",
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Welcome to Aura",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "A premium, lag-free, and smart launcher designed to transform your Android experience.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DF6)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Let's Get Started", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GridStep(
    prefs: AuraPrefs,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    var selectedColumns by remember { mutableStateOf(prefs.gridColumns) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Choose Grid Layout",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Select the number of columns you want for your app drawer.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Grid Options Selectors
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (col in 3..6) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selectedColumns == col) Color(0xFF6C4DF6).copy(alpha = 0.3f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            width = 2.dp,
                            color = if (selectedColumns == col) Color(0xFF9D86FF) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { selectedColumns = col },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = col.toString(),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cols",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Grid Columns Live Preview Mockup
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Layout Preview ($selectedColumns Columns)",
                    color = Color(0xFF9D86FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 1..selectedColumns) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.25f))
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Back", fontSize = 14.sp)
            }

            Button(
                onClick = {
                    prefs.gridColumns = selectedColumns
                    onNext()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DF6)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Next", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun AiStep(
    prefs: AuraPrefs,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    var keyText by remember { mutableStateOf(prefs.groqApiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Setup Aura AI Helper",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Paste your FREE Groq API key here (from console.groq.com) to enable AI desktop helper and smart categorizer.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = keyText,
            onValueChange = { keyText = it },
            placeholder = { Text("gsk_...", color = Color.White.copy(alpha = 0.4f)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF9D86FF),
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
            )
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Note: Your key is securely stored entirely offline on this device. It is never uploaded to any third party other than direct API requests.",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Back", fontSize = 14.sp)
            }

            Button(
                onClick = {
                    prefs.groqApiKey = keyText
                    onNext()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DF6)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text(if (keyText.isBlank()) "Skip" else "Next", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun PermissionsStep(
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current

    var locationGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var micGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    var usageStatsGranted by remember {
        mutableStateOf(RecentApps.hasUsagePermission(context))
    }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationGranted = granted
    }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted = granted
    }

    // Checking permission state when returning from settings screen
    LaunchedEffect(Unit) {
        while (true) {
            usageStatsGranted = RecentApps.hasUsagePermission(context)
            kotlinx.coroutines.delay(1000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Enable Core Features",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Aura works best with these standard launcher permissions. Enable what you want to use.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // Location permission row
        PermissionRow(
            title = "Location Weather Service",
            description = "Required to fetch local weather information for the home screen widget.",
            icon = Icons.Filled.LocationOn,
            granted = locationGranted,
            onRequest = { locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
        )

        Spacer(Modifier.height(16.dp))

        // Mic permission row
        PermissionRow(
            title = "Voice Search & Assistant",
            description = "Allows you to speak to search apps and ask the built-in AI Helper.",
            icon = Icons.Filled.Mic,
            granted = micGranted,
            onRequest = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
        )

        Spacer(Modifier.height(16.dp))

        // UsageStats row
        PermissionRow(
            title = "Smart Usage Predictions",
            description = "Required to predict and display your recently and frequently used apps.",
            icon = Icons.Filled.Speed,
            granted = usageStatsGranted,
            onRequest = { RecentApps.requestUsagePermission(context) }
        )

        Spacer(Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Back", fontSize = 14.sp)
            }

            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DF6)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Continue", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF6C4DF6).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = Color(0xFF9D86FF))
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
        }

        Spacer(Modifier.width(8.dp))

        if (granted) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF66E08F).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.Done, contentDescription = "Granted", tint = Color(0xFF66E08F))
            }
        } else {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DF6)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Grant", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun FinishStep(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isDefault = LauncherActions.isDefaultLauncher(context)
            kotlinx.coroutines.delay(1000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF66E08F).copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Done,
                contentDescription = "Success",
                tint = Color(0xFF66E08F),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "Setup Complete!",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Aura Launcher is configured and ready to roll. Set it as your home application to complete the experience.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(36.dp))

        if (!isDefault) {
            Button(
                onClick = { LauncherActions.requestSetDefault(context) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DF6)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text("Set Aura as Default Launcher", color = Color.White, fontWeight = FontWeight.Bold)
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF66E08F).copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, "Default", tint = Color(0xFF66E08F))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Aura is currently your default launcher!",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Back", fontSize = 14.sp)
            }

            Button(
                onClick = onComplete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9D86FF)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Enter Aura", color = Color(0xFF0F0C1E), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PlanStep(
    prefs: AuraPrefs,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    var selectedPro by remember { mutableStateOf(prefs.isPro) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Choose Your Aura Plan",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "Unlock premium customizers, AI backgrounds, and multitasking features.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // Plan 1: Free Plan Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (!selectedPro) Color(0xFF6C4DF6).copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.04f)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = if (!selectedPro) Color(0xFF9D86FF) else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { selectedPro = false }
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Free Plan", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("FREE", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("• Standard 4x4 Grid layout", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text("• 2 Free customizable app icons", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text("• Local weather and recent tasks panel", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Plan 2: Pro Plan Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (selectedPro) Color(0xFF6C4DF6).copy(alpha = 0.15f)
                else Color.White.copy(alpha = 0.04f)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    color = if (selectedPro) Color(0xFF9D86FF) else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable { selectedPro = true }
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Aura Pro Plan", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFB300), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("PRO", color = Color(0xFF1B1730), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("$4.99/mo", color = Color(0xFFFFB300), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text("• 10 Premium AI-Generated wallpapers", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text("• App Freezer (Hibernate system apps)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text("• Alternate Premium App Icons (Whirl, Gold, Cyberpunk)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                Text("• Advanced Custom Gestures & Grid controls", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(40.dp))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Back", fontSize = 14.sp)
            }

            Button(
                onClick = {
                    prefs.isPro = selectedPro
                    onNext()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C4DF6)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text("Next", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
