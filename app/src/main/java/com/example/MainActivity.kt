package com.example

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.receiver.LockerDeviceAdminReceiver
import com.example.service.LockerService
import com.example.data.LockerEntity
import com.example.data.LockerRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: LockerRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = LockerRepository(applicationContext)

        // Preload/Check defaults
        val mainScope = CoroutineScope(Dispatchers.IO)
        mainScope.launch {
            repository.getSettings()
        }

        // Standard background watcher service start
        lifecycleScope.launch {
            try {
                val serviceIntent = Intent(applicationContext, LockerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to start service safely during activity startup: ${e.message}")
            }
        }

        setContent {
            MyApplicationTheme(dynamicColor = false, darkTheme = false) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    LockerDashboard(
                        repository = repository,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Trigger recomposition if permissions are updated outside the app
    }
}

@Composable
fun LockerDashboard(
    repository: LockerRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var settings by remember { mutableStateOf<LockerEntity?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Live permission states
    var overlayPermissionGranted by remember { mutableStateOf(false) }
    var deviceAdminGranted by remember { mutableStateOf(false) }

    // Refresh permission status
    fun checkPermissions() {
        overlayPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, LockerDeviceAdminReceiver::class.java)
        deviceAdminGranted = dpm.isAdminActive(adminComponent)
    }

    // Run permission check
    LaunchedEffect(Unit) {
        checkPermissions()
        repository.settingsFlow.collect {
            settings = it
        }
    }

    val currentSettings = settings ?: return // Wait till Room loads content

    // Deep luxury dark background custom brush
    val luxuryBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Elegant Header Area matching the Tailwind code
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(top = 28.dp, bottom = 18.dp, start = 24.dp, end = 24.dp)
                    .border(width = 1.dp, color = BorderSlate),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "REMOTE SECURITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary, // Security blue
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Admin Panel",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Decorative right panel icon in HTML
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SecurityBlueLight),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(width = 2.dp, color = SecurityBlue, shape = RoundedCornerShape(4.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SecurityBlue)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dual Tab Controller: Admin & Target Device Demo
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp),
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Admin Controller",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Slate600
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Target Client Status",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Slate600
                        )
                    }
                )
            }

            // Tab Content Display with Transition
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                if (selectedTab == 0) {
                    AdminTabContent(
                        settings = currentSettings,
                        repository = repository,
                        deviceAdminGranted = deviceAdminGranted,
                        overlayPermissionGranted = overlayPermissionGranted,
                        onRequestRefresh = { checkPermissions() }
                    )
                } else {
                    ClientTabContent(
                        settings = currentSettings,
                        repository = repository,
                        deviceAdminGranted = deviceAdminGranted,
                        overlayPermissionGranted = overlayPermissionGranted,
                        onRequestRefresh = { checkPermissions() }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminTabContent(
    settings: LockerEntity,
    repository: LockerRepository,
    deviceAdminGranted: Boolean,
    overlayPermissionGranted: Boolean,
    onRequestRefresh: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Remote parameters form state
    var editPin by remember { mutableStateOf(settings.pinCode) }
    var editMessage by remember { mutableStateOf(settings.lockMessage) }
    var adminPasswordInput by remember { mutableStateOf("") }
    var isAdminAuthenticated by remember { mutableStateOf(false) }

    // Synchronize states
    LaunchedEffect(settings) {
        editPin = settings.pinCode
        editMessage = settings.lockMessage
    }

    if (!isAdminAuthenticated) {
        // High-security Admin Access Validation matching Natural Tones style
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SecurityBlueLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Admin Password Protection Icon",
                    tint = SecurityBlue,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Admin Control Panel Access",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NaturalText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "অ্যাডমিন প্যানেল সুরক্ষিত রাখতে আপনার অ্যাডমিন পাসওয়ার্ড লিখুন (ডিফল্ট: admin123)",
                fontSize = 13.sp,
                color = Slate600,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = adminPasswordInput,
                onValueChange = { adminPasswordInput = it },
                label = { Text("Admin Password") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SecurityBlue,
                    unfocusedBorderColor = BorderSlate,
                    focusedLabelColor = SecurityBlue,
                    unfocusedLabelColor = Slate600
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (adminPasswordInput == settings.adminPassword) {
                        isAdminAuthenticated = true
                    } else {
                        Toast.makeText(context, "ভুল পাসওয়ার্ড! সঠিক পাসওয়ার্ড চেষ্টা করুন।", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SecurityBlue)
            ) {
                Text("Login Admin Panel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // REMOTE COMMANDS CARD (Card 1 from HTML)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSlate, shape = RoundedCornerShape(32.dp)),
                    shape = RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Slate100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("📱", fontSize = 18.sp)
                                }
                                Column {
                                    Text(
                                        text = "S23 Ultra - Target",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = NaturalText
                                    )
                                    Text(
                                        text = "Online • Android 15",
                                        fontSize = 12.sp,
                                        color = AlertGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Dynamic Status Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (settings.isLocked) AlertGreenBg else AlertRedBg)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (settings.isLocked) "LOCKED" else "UNLOCKED",
                                    color = if (settings.isLocked) AlertGreen else AlertRed,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                if (!deviceAdminGranted || !overlayPermissionGranted) {
                                    Toast.makeText(context, "সতর্কতা: সঠিক অনুমতি মঞ্জুর না থাকলে লক স্ক্রিন কাজ নাও করতে পারে। অনুগ্রহ করে Target Client tab-এ অনুমতি দিন।", Toast.LENGTH_LONG).show()
                                }
                                coroutineScope.launch {
                                    val newLockState = !settings.isLocked
                                    repository.updateLockState(newLockState)
                                    
                                    val serviceIntent = Intent(context, LockerService::class.java).apply {
                                        putExtra("TRIGGER_OVERLAY", newLockState)
                                    }
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(serviceIntent)
                                    } else {
                                        context.startService(serviceIntent)
                                    }

                                    Toast.makeText(
                                        context,
                                        if (newLockState) "অনুরোধ পাঠানো হয়েছে: লক স্ক্রিন চালু হচ্ছে!" else "অনুরোধ পাঠানো হয়েছে: লক স্ক্রিন বন্ধ হচ্ছে!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (settings.isLocked) AlertGreen else SecurityBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (settings.isLocked) Icons.Default.Check else Icons.Default.Lock,
                                    contentDescription = "Lock/Unlock status icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (settings.isLocked) "DEACTIVATE REMOTE LOCK" else "ACTIVATE REMOTE LOCK",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // SECURITY CONSTRAINTS CARD (Card 2 from HTML)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSlate, shape = RoundedCornerShape(32.dp)),
                    shape = RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "SECURITY CONSTRAINTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val constraints = listOf(
                            "Disable Power Menu" to "Prevents device shutdown",
                            "Force PIN Authentication" to "Bypass biometrics & swipe",
                            "Persistent Boot Lock" to "Lock active after restart",
                            "Block Volume Control" to "Disable physical rockers"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            constraints.forEachIndexed { index, constraint ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = constraint.first,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = NaturalText
                                        )
                                        Text(
                                            text = constraint.second,
                                            fontSize = 12.sp,
                                            color = Slate600
                                        )
                                    }

                                    // Custom Switch representation
                                    var checked by remember { mutableStateOf(index < 3) }
                                    Switch(
                                        checked = checked,
                                        onCheckedChange = { checked = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = SecurityBlue,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedTrackColor = Slate100
                                        )
                                    )
                                }
                                if (index < constraints.size - 1) {
                                    Divider(color = Slate100, thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }

            // CARBON CURRENT PIN CODE & SETTINGS FORM (Section 3 from HTML)
            item {
                var showSettingsForm by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Carbon aesthetic block
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SoftDarkCarbon),
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CURRENT PIN CODE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate400,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "**** " + settings.pinCode.takeLast(4),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF60A5FA), // Light Blue
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    letterSpacing = 1.5.sp
                                )
                            }
                            Button(
                                onClick = { showSettingsForm = !showSettingsForm },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (showSettingsForm) "CLOSE" else "CHANGE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Slide down interactive config panel (if user clicks "CHANGE" button)
                    if (showSettingsForm) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderSlate, shape = RoundedCornerShape(32.dp)),
                            shape = RoundedCornerShape(32.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                Text(
                                    text = "UPDATE SECURITY CREDENTIALS",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate600
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = editPin,
                                    onValueChange = { input ->
                                        if (input.length <= 4 && input.all { it.isDigit() }) {
                                            editPin = input
                                        }
                                    },
                                    label = { Text("Remote Unlock PIN Code (4 Digits)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SecurityBlue,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedLabelColor = SecurityBlue,
                                        unfocusedLabelColor = Slate600
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = editMessage,
                                    onValueChange = { editMessage = it },
                                    label = { Text("Remote Warning Custom Message") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SecurityBlue,
                                        unfocusedBorderColor = BorderSlate,
                                        focusedLabelColor = SecurityBlue,
                                        unfocusedLabelColor = Slate600
                                    ),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        if (editPin.length != 4) {
                                            Toast.makeText(context, "পিন কোড অবশ্যই ৪ ডিজিটের হতে হবে!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        coroutineScope.launch {
                                            val current = repository.getSettings()
                                            repository.saveSettings(current.copy(pinCode = editPin, lockMessage = editMessage))
                                            Toast.makeText(context, "কনফিগারেশন সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
                                            showSettingsForm = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SecurityBlue)
                                ) {
                                    Text("SAVE CONFIGURATION", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // WALLPAPER CUSTOMIZATION CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSlate, shape = RoundedCornerShape(32.dp)),
                    shape = RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = "CUSTOMIZE LOCKSCREEN WALLPAPER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val themes = listOf(
                            Triple("COSMIC_MIDNIGHT", "Cosmic Midnight", Brush.radialGradient(colors = listOf(Color(0xFF1E3A8A), Color(0xFF090514)))),
                            Triple("CRIMSON_LOCK", "Crimson Lock", Brush.radialGradient(colors = listOf(Color(0xFF880E4F), Color(0xFF0C0003)))),
                            Triple("BIO_MATRIX", "Emerald Matrix", Brush.linearGradient(colors = listOf(Color(0xFF022C22), Color(0xFF010609)))),
                            Triple("STEALTH_CARBON", "Carbon Minimal", Brush.linearGradient(colors = listOf(Color(0xFF1E293B), Color(0xFF090909))))
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(themes) { item ->
                                val selected = settings.wallpaperTheme == item.first
                                Box(
                                    modifier = Modifier
                                        .size(120.dp, 80.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(item.third)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) SecurityBlue else BorderSlate,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                repository.updateWallpaperTheme(item.first)
                                                Toast.makeText(context, "ওয়ালপেপার পরিবর্তিত হয়েছে!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.second,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) SecurityBlueLight else Color.White,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // RESET ADMIN PASSWORD CARD
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSlate, shape = RoundedCornerShape(32.dp)),
                    shape = RoundedCornerShape(32.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        var editAdminPass by remember { mutableStateOf(settings.adminPassword) }
                        
                        Text(
                            text = "ADMIN PANEL CREDENTIALS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = editAdminPass,
                            onValueChange = { editAdminPass = it },
                            label = { Text("Change Admin Password") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SecurityBlue,
                                unfocusedBorderColor = BorderSlate,
                                focusedLabelColor = SecurityBlue,
                                unfocusedLabelColor = Slate600
                            ),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (editAdminPass.isEmpty()) return@Button
                                coroutineScope.launch {
                                    repository.updateAdminPassword(editAdminPass)
                                    Toast.makeText(context, "অ্যাডমিন পাসওয়ার্ড পরিবর্তিত হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate100),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("SAVE ADMIN PASSWORD", color = Slate600, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClientTabContent(
    settings: LockerEntity,
    repository: LockerRepository,
    deviceAdminGranted: Boolean,
    overlayPermissionGranted: Boolean,
    onRequestRefresh: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // SYSTEM OVERVIEW / SETUP TITLE / BENGLA INFO
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSlate, shape = RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "ডিভাইস সেটআপ নির্দেশিকা (DPC Setup Guide)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "এটি একটি অত্যন্ত শক্তিশালী দূরবর্তী লকিং অ্যাপ্লিকেশন (Remote Locker APP)। এটি আপনার বন্ধুর ফোনে ব্যাকগ্রাউন্ডে চলবে এবং আপনার ডিভাইস অ্যাডমিন প্যানেল থেকে অ্যাকশন নেওয়া সম্ভব।\n\n" +
                                "লকিং সঠিকভাবে কার্যকর করতে নিচের অনুমতিগুলো দান করুন।\n\n" +
                                "১) Display over other apps: লক স্ক্রিন ওভারলে আঁকার অনুমতি যাতে কেউ ব্যাকগ্রাউন্ড থেকে পালাতে না পারে।\n" +
                                "২) Device Admin: ফোন এবং সেটিংস আনইনস্টল সুরক্ষিত করা যাতে কেউ অ্যাপটি আনইনস্টল করতে না পারে।",
                        fontSize = 13.sp,
                        color = Slate600,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // PERMISSIONS LIST MANAGER CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSlate, shape = RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "System Security Permissions Manager",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NaturalText
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Permission 1: SYSTEM OVER_LAY
                    PermissionRow(
                        title = "Draw Over Other Apps Overlay",
                        description = "Enables locks to cover apps completely.",
                        isActive = overlayPermissionGranted,
                        onGrant = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Permission is automatically granted below Marshmallow", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    Divider(modifier = Modifier.padding(vertical = 12.dp), color = BorderSlate)

                    // Permission 2: DEVICE ADMIN
                    PermissionRow(
                        title = "Register Device Administrator",
                        description = "Secures target app against manual uninstalls.",
                        isActive = deviceAdminGranted,
                        onGrant = {
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(
                                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                    ComponentName(context, LockerDeviceAdminReceiver::class.java)
                                )
                                putExtra(
                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    "Remote Screen Locker administration capabilities are requested to secure uninstalls and lock screens on-demand."
                                )
                            }
                            context.startActivity(intent)
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Button(
                        onClick = { onRequestRefresh() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Slate100)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh icon", tint = Slate600)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("REFRESH PERMISSION SCAN", color = Slate600, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // SIMULATED REMOTE PACKET CONNECTIONS LOGS CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSlate, shape = RoundedCornerShape(32.dp)),
                shape = RoundedCornerShape(32.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Remote Sync Telemetry Logs (Simulated)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NaturalText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    val logs = listOf(
                        "SOCKET: Initialized security listener on port 8089...",
                        "DPC_MGR: Device Administrator core checked - ACTIVE",
                        "SOCKET: Remote admin connection verified with signature key...",
                        "SYS_MGR: Lock state persistent backup verified. Autostart alive.",
                        "LOCK_MGR: Active wall theme set to '${settings.wallpaperTheme}'"
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SoftDarkCarbon, shape = RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        for (log in logs) {
                            Text(
                                text = "-> $log",
                                fontSize = 11.sp,
                                color = Color(0xFF00FFCC),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    description: String,
    isActive: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NaturalText
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = Slate600
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isActive) AlertGreenBg else AlertRedBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = if (isActive) "ACTIVE" else "MISSING",
                    color = if (isActive) AlertGreen else AlertRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            IconButton(
                onClick = onGrant,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) Slate100 else SecurityBlue
                    )
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.Check else Icons.Default.ArrowForward,
                    contentDescription = "Grant status arrow",
                    tint = if (isActive) AlertGreen else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
