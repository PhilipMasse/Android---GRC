package fr.berrelesalpes.grc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import fr.berrelesalpes.grc.ui.navigation.GrcNavGraph
import fr.berrelesalpes.grc.ui.theme.GrcTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val application = application as GrcApplication

        setContent {
            GrcTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    GrcNavGraph(application = application)
                }
            }
        }
    }
}
