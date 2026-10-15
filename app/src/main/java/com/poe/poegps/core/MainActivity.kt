package com.poe.poegps.core

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import com.poe.poegps.R
import com.poe.poegps.core.designsystem.DarkPoeColors
import com.poe.poegps.core.designsystem.LightPoeColors
import com.poe.poegps.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

// Screens already moved to Compose draw their own header, so the old toolbar is hidden on them
private val REDESIGNED = setOf(R.id.mainFragment, R.id.editorFragment, R.id.tpEditorFragment)

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private var redesigned = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // Toolbar is always colorPrimary, so status bar icons stay light
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            binding.toolbar.updatePadding(left = bars.left, top = bars.top, right = bars.right)
            binding.flFragment.updatePadding(
                left = bars.left,
                top = if (redesigned) bars.top else 0,
                right = bars.right,
                bottom = bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }

        val navHost = supportFragmentManager.findFragmentById(R.id.usersNavHostFragment) as NavHostFragment
        navHost.navController.addOnDestinationChangedListener { _, destination, _ ->
            redesigned = destination.id in REDESIGNED
            val dark = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            binding.toolbar.visibility = if (redesigned) View.GONE else View.VISIBLE
            binding.root.setBackgroundColor(
                if (redesigned) (if (dark) DarkPoeColors else LightPoeColors).ground.toArgb() else Color.TRANSPARENT
            )
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = redesigned && !dark
            ViewCompat.requestApplyInsets(binding.root)
        }
    }
}
