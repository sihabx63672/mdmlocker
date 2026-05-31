package com.example.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LockerEntity
import com.example.data.LockerRepository
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class LockerActivity : ComponentActivity() {
    private lateinit var repository: LockerRepository
    private var isUnlockedLocally = false
    private val activityScope = CoroutineScope(Dispatchers.Main)

    // Broadcast receiver to finish when the admin unlocks remotely
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("LockerActivity", "Remote unlock broadcast received! Terminating Locke Activity.")
            isUnlockedLocally = true
            finish()
            overridePendingTransition(0, android.R.anim.fade_out)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure Activity to span the entire lockscreen space and wake the screen up
        enableEdgeToEdge()
        configureLockWindowFlags()

        repository = LockerRepository(applicationContext)

        // Disable standard back button behavior completely
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing to prevent backing out of the lock screen!
                Log.d("LockerActivity", "Back button ignored due to secure lock constraint")
            }
        })

        // Register broadcast receiver for remote unlock actions
        registerReceiver(unlockReceiver, IntentFilter("com.example.ACTION_REMOTE_UNLOCK"))

        // Render Compose View
        setContent {
            MyApplicationTheme(dynamicColor = false, darkTheme = true) {
                LockScreenUI()
            }
        }
    }

    private fun configureLockWindowFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as android.app.KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        // Fullscreen limits
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )
    }

    // Intercept physical volume events to ensure sliders or standard bar popups don't show up!
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            Log.d("LockerActivity", "Physical Volume Button Intercepted and Suppressed.")
            return true // Consume key press completely!
        }
        return super.onKeyDown(keyCode, event)
    }

    // Relaunch immediately if the user touches background navigation keys like Home or Recent
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        activityScope.launch {
            val settings = repository.getSettings()
            if (settings.isLocked && !isUnlockedLocally) {
                Log.d("LockerActivity", "User attempted to background the lockscreen. Relaunching IMMEDIATELY!")
                val relaunchIntent = Intent(this@LockerActivity, LockerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                startActivity(relaunchIntent)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }

    @Composable
    fun LockScreenUI() {
        var settings by remember { mutableStateOf<LockerEntity?>(null) }
        var inputPin by remember { mutableStateOf("") }
        var isShaking by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }

        // Live date/time state
        var currentTimeString by remember { mutableStateOf("") }
        var currentDateString by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
            // Read settings flow
            repository.settingsFlow.collect {
                settings = it
            }
        }

        // Clock clock updater
        LaunchedEffect(Unit) {
            while (true) {
                val cal = Calendar.getInstance()
                currentTimeString = SimpleDateFormat("hh:mm", Locale.getDefault()).format(cal.time)
                currentDateString = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(cal.time)
                delay(1000)
            }
        }

        // Shake animation effect for wrong PIN error feedback
        val shakeOffset by animateDpAsState(
            targetValue = if (isShaking) 15.dp else 0.dp,
            animationSpec = keyframes {
                durationMillis = 400
                0.dp at 0
                (-12).dp at 50
                12.dp at 100
                (-12).dp at 150
                12.dp at 200
                (-12).dp at 250
                12.dp at 300
                0.dp at 400
            }
        )

        val currentSettings = settings ?: return // Wait for Room to load

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Custom wallpaper brush drawing
            WallpaperBackground(theme = currentSettings.wallpaperTheme)

            // Blur/Dusk overlay for extreme atmospheric readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            // Main Layout Scroll/Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 24.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Topic: Time and Alarm Indicators
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 40.dp)
                ) {
                    Text(
                        text = currentTimeString.ifEmpty { "00:00" },
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif,
                        style = LocalTextStyle.current.copy(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.6f),
                                offset = Offset(0f, 4f),
                                blurRadius = 12f
                            )
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentDateString.ifEmpty { "Loading..." },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Warning Text card inside lock overlay
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.65f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Red.copy(alpha = 0.4f), shape = MaterialTheme.shapes.medium)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Security Alert",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "রিমোটলি লক করা হয়েছে",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252)
                                )
                                Text(
                                    text = currentSettings.lockMessage,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // PIN indicator indicators + Keypad Pad
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .offset(x = shakeOffset)
                        .padding(bottom = 36.dp)
                ) {
                    // Password lock icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "SecuLock",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // PIN circle indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val active = i < inputPin.length
                            val color = if (errorMessage.isNotEmpty()) Color(0xFFFF5252) else Color.White
                            val size by animateDpAsState(targetValue = if (active) 16.dp else 12.dp)
                            
                            Box(
                                modifier = Modifier
                                    .size(size)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) color else Color.White.copy(alpha = 0.3f)
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live PIN check messages
                    Box(modifier = Modifier.height(24.dp)) {
                        if (errorMessage.isNotEmpty()) {
                            Text(
                                text = errorMessage,
                                color = Color(0xFFFF5252),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Keypad Panel Box (0 - 9, C, Backspace)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(280.dp)
                    ) {
                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("C", "0", "DEL")
                        )

                        for (rowData in rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (key in rowData) {
                                    KeypadButton(
                                        char = key,
                                        onClick = {
                                            if (errorMessage.isNotEmpty()) {
                                                errorMessage = "" // clear error on new tap
                                            }

                                            when (key) {
                                                "C" -> {
                                                    inputPin = ""
                                                }
                                                "DEL" -> {
                                                    if (inputPin.isNotEmpty()) {
                                                        inputPin = inputPin.dropLast(1)
                                                    }
                                                }
                                                else -> {
                                                    if (inputPin.length < 4) {
                                                        inputPin += key
                                                    }

                                                    // Submit verify immediately upon hitting 4 digits
                                                    if (inputPin.length == 4) {
                                                        activityScope.launch {
                                                            delay(150) // slight delay for dot-light effect
                                                            if (inputPin == currentSettings.pinCode) {
                                                                // UNLOCKED SUCCESSFULLY!
                                                                isUnlockedLocally = true
                                                                repository.updateLockState(false)
                                                                finish()
                                                                overridePendingTransition(0, android.R.anim.fade_out)
                                                            } else {
                                                                // INCORRECT PIN! TRIGGER SHAKE
                                                                isShaking = true
                                                                errorMessage = "ভুল পিন কোড! পিন কোড পুনরায় চেষ্টা করুন।"
                                                                inputPin = ""
                                                                delay(400)
                                                                isShaking = false
                                                            }
                                                        }
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
    fun KeypadButton(
        char: String,
        onClick: () -> Unit
    ) {
        val containerColor = if (char == "C" || char == "DEL") {
            Color.White.copy(alpha = 0.08f)
        } else {
            Color.White.copy(alpha = 0.15f)
        }

        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(containerColor)
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            when (char) {
                "DEL" -> {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Icon Keyboard",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                }
                "C" -> {
                    Text(
                        text = "C",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                else -> {
                    Text(
                        text = char,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    @Composable
    fun WallpaperBackground(theme: String) {
        when (theme) {
            "CRIMSON_LOCK" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF880E4F), Color(0xFF0D0005)),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.width * 1.5f
                        )
                    )
                    
                    // Cyber circuit accents
                    val stroke = size.width * 0.005f
                    drawLine(
                        color = Color(0xFFFF1744).copy(alpha = 0.15f),
                        start = Offset(0f, size.height * 0.2f),
                        end = Offset(size.width, size.height * 0.2f),
                        strokeWidth = stroke
                    )
                    drawLine(
                        color = Color(0xFFFF1744).copy(alpha = 0.15f),
                        start = Offset(size.width * 0.5f, size.height * 0.2f),
                        end = Offset(size.width * 0.5f, size.height * 0.9f),
                        strokeWidth = stroke
                    )
                }
            }
            "BIO_MATRIX" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF0F172A), Color(0xFF022C22)),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height)
                        )
                    )
                    
                    // Simulated Matrix green code lines
                    for (i in 1..8) {
                        val x = size.width * (i * 0.12f)
                        drawLine(
                            color = Color(0xFF10B981).copy(alpha = 0.08f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 3f
                        )
                    }
                }
            }
            "STEALTH_CARBON" -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF000000)),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f)
                        )
                    )
                }
            }
            else -> { // COSMIC_MIDNIGHT (Default)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8), Color(0xFF090514)),
                            center = Offset(size.width / 2, size.height / 3),
                            radius = size.width * 1.4f
                        )
                    )
                    
                    // Cosmic stars
                    drawCircle(
                        color = Color.White.copy(alpha = 0.3f),
                        radius = 4f,
                        center = Offset(size.width * 0.2f, size.height * 0.3f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = 6f,
                        center = Offset(size.width * 0.75f, size.height * 0.15f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = 5f,
                        center = Offset(size.width * 0.45f, size.height * 0.7f)
                    )
                }
            }
        }
    }
}
