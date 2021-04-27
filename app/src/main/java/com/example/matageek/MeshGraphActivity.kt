package com.example.matageek

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.matageek.databinding.ActivityMeshGraphBinding

class MeshGraphActivity : AppCompatActivity() {
    private lateinit var _bind: ActivityMeshGraphBinding
    private val bind get() = _bind

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _bind = ActivityMeshGraphBinding.inflate(layoutInflater)
        setContentView(bind.root)
        // set app bar back
        setSupportActionBar(bind.meshGraphToolBar)
        supportActionBar?.title = "Mesh Graph"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}