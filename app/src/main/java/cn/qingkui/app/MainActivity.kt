package cn.qingkui.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cn.qingkui.app.ui.QingkuiApp
import cn.qingkui.app.ui.theme.QingkuiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QingkuiTheme {
                QingkuiApp()
            }
        }
    }
}
