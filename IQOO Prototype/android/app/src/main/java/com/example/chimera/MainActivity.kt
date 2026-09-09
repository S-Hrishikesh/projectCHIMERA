package com.example.chimera

import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class MainActivity : ComponentActivity() {

    private lateinit var powerManager: PowerManager
    private val chimeraEngine = ChimeraEngine()
    
    // For local ADB test, IP is 127.0.0.1 (via adb reverse)
    // Set to your laptop's IP if testing over Wi-Fi
    private val networkClient = NetworkClient("127.0.0.1", 8080)
    
    private var isThermalThrottling = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        powerManager = getSystemService(POWER_SERVICE) as PowerManager
        powerManager.addThermalStatusListener { status ->
            // Trigger offload if thermal status is MODERATE or higher (Usually around 40-45C internal temp)
            isThermalThrottling.value = status >= PowerManager.THERMAL_STATUS_MODERATE
        }

        setContent {
            ChimeraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
    
    @Composable
    fun MainScreen() {
        var forceRemote by remember { mutableStateOf(false) }
        var isComputing by remember { mutableStateOf(false) }
        var lastExecutionMs by remember { mutableStateOf(0L) }
        var resultChecksum by remember { mutableStateOf(0f) }
        
        val coroutineScope = rememberCoroutineScope()
        
        val useRemote = isThermalThrottling.value || forceRemote

        Column(
            modifier = Modifier.padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Project Chimera", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            
            // Status Badge
            Box(
                modifier = Modifier
                    .background(if (useRemote) Color(0xFFE65100) else Color(0xFF2E7D32), shape = MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Text(
                    text = if (useRemote) "REMOTE RISC-V RVV 1.0" else "LOCAL ARM NEON",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Thermal Status Threshold Met: ${isThermalThrottling.value}", modifier = Modifier.padding(end = 8.dp))
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Manual Override (Force Remote): ")
                Switch(checked = forceRemote, onCheckedChange = { forceRemote = it })
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    coroutineScope.launch {
                        isComputing = true
                        val data = FloatArray(1_000_000) { it.toFloat() }
                        
                        if (useRemote) {
                            val totalTimeMs = measureTimeMillis {
                                try {
                                    val serialized = chimeraEngine.serializePayload(1, data)
                                    val response = networkClient.executeRemoteVectorWorkload(serialized)
                                    chimeraEngine.deserializePayload(response, data)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            lastExecutionMs = totalTimeMs
                        } else {
                            val timeUs = chimeraEngine.neonComputeKernel(data)
                            lastExecutionMs = timeUs / 1000
                        }
                        
                        // Simple checksum to verify math (sum of first 10 elements)
                        resultChecksum = data.take(10).sum()
                        isComputing = false
                    }
                },
                enabled = !isComputing
            ) {
                Text(if (isComputing) "Computing..." else "Trigger Heavy Vector Compute (1M floats)")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Latency / Execution: $lastExecutionMs ms")
            Text("Result Checksum: $resultChecksum")
        }
    }
}

@Composable
fun ChimeraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(),
        content = content
    )
}
