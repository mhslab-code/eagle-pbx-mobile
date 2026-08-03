package com.eaglesistemas.eaglepbx

import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eaglesistemas.eaglepbx.telephony.SipForegroundService
import kotlinx.coroutines.delay

class IncomingCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        render(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        render(intent)
    }

    private fun render(source: Intent) {
        val caller = source.getStringExtra(SipForegroundService.EXTRA_CALLER_NAME)
            ?.takeIf(String::isNotBlank) ?: "Chamada recebida"
        val number = source.getStringExtra(SipForegroundService.EXTRA_CALLER_NUMBER).orEmpty()
        val photo = source.getStringExtra(SipForegroundService.EXTRA_CALLER_PHOTO)
        setContent {
            IncomingCallScreen(
                caller = caller,
                number = number,
                photo = photo,
                onReject = {
                    startService(Intent(this, SipForegroundService::class.java).apply {
                        action = SipForegroundService.ACTION_REJECT
                    })
                    finishAndRemoveTask()
                },
                onAnswer = {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        action = SipForegroundService.ACTION_ANSWER
                        putExtras(source)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finish()
                },
                onCallEnded = { finishAndRemoveTask() }
            )
        }
    }
}

@Composable
private fun IncomingCallScreen(
    caller: String,
    number: String,
    photo: String?,
    onReject: () -> Unit,
    onAnswer: () -> Unit,
    onCallEnded: () -> Unit
) {
    LaunchedEffect(Unit) {
        while (SipForegroundService.currentIncomingCall() != null) delay(250)
        onCallEnded()
    }
    val navy = Color(0xFF031C2E)
    val panel = Color(0xFF0E304A)
    Box(
        modifier = Modifier.fillMaxSize().background(navy).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().background(panel, RoundedCornerShape(28.dp)).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("CHAMADA RECEBIDA", color = Color(0xFF42A5FF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(28.dp))
            CallerImage(photo)
            Spacer(Modifier.height(20.dp))
            Text(caller, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(number, color = Color(0xFFB9CADD), fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))
            Text("Chamando...", color = Color(0xFF42A5FF), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(30.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Button(onClick = onReject, modifier = Modifier.weight(1f).height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE43D4A)), shape = RoundedCornerShape(14.dp)) {
                    Text("Recusar", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onAnswer, modifier = Modifier.weight(1f).height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF12B66F)), shape = RoundedCornerShape(14.dp)) {
                    Text("Atender", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CallerImage(photo: String?) {
    val bitmap = runCatching {
        val payload = photo?.substringAfter("base64,") ?: error("sem foto")
        val bytes = Base64.decode(payload, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() ?: error("foto inválida")
    }.getOrNull()
    if (bitmap != null) {
        Image(bitmap, null, Modifier.size(92.dp).clip(CircleShape), contentScale = ContentScale.Crop)
    } else {
        Image(painterResource(R.drawable.eagle_pbx_logo), null, Modifier.size(92.dp).clip(CircleShape), contentScale = ContentScale.Crop)
    }
}
