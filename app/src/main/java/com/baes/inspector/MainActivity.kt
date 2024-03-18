package com.baes.inspector

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.baes.inspector.databinding.ActivityMainBinding
import com.baes.inspector.fragments.MainFragment
import com.baes.inspector.fragments.SettingsFragment
import com.baes.inspector.fragments.TracksFragment
import com.baes.inspector.utils.openFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        onButtonNavClick()
        openFragment(MainFragment.newInstance())
    }

    private fun onButtonNavClick(){
        binding.bNav.setOnItemSelectedListener {
            when(it.itemId){
                R.id.id_home -> openFragment(MainFragment.newInstance())
                R.id.id_tracks -> openFragment(TracksFragment.newInstance())
                R.id.id_settings -> openFragment(SettingsFragment())
            }
            true
        }
    }
}