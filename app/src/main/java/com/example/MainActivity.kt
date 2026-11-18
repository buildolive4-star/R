package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.notifications.PixoraNotificationManager
import com.example.ui.datemate.DateMateAppRoot
import com.example.ui.theme.DateMateTheme

const val ROUTE_EXPLORE = "home"
const val ROUTE_CHATS = "chat"
const val ROUTE_SAVED = "discover"
const val ROUTE_SETTINGS = "profile"
const val ROUTE_DETAIL = "detail/{imageId}"
const val ROUTE_CHAT_ROOM = "chat/{chatId}/{senderRole}"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        PixoraNotificationManager.ensureChannel(this)

        setContent {
            DateMateTheme {
                DateMateAppRoot()
            }
        }
    }
}
