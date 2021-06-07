package com.example.matageek

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.matageek.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var _bind: ActivityMainBinding
    private val bind get() = _bind

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _bind = ActivityMainBinding.inflate(layoutInflater)

        setSupportActionBar(bind.toolbar)
        val navController =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment).navController
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        bind.toolbar.setupWithNavController(navController, appBarConfiguration)

        setContentView(bind.root)
    }

}