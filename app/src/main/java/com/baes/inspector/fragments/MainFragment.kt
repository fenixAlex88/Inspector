package com.baes.inspector.fragments

import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.baes.inspector.databinding.FragmentMainBinding
import com.baes.inspector.utils.checkPermission
import com.baes.inspector.utils.showToast
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class MainFragment : Fragment() {
    private lateinit var pLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var binding: FragmentMainBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        settingsOsm()
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registerPermissions()
        checkLocPermission()
    }

    private fun registerPermissions() {
        pLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                if (it[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                    initOSM()
                } else {
                    showToast("Вы не дали разрешение на использование местоположения")
                }
            }
    }

    private fun settingsOsm() {
        Configuration.getInstance().load(
            activity as AppCompatActivity,
            activity?.getSharedPreferences("osm_pref", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
    }

    private fun initOSM() = with(binding) {
        map.controller.setZoom(20.0)
        val mLocProvider = GpsMyLocationProvider(activity)
        val mLocOverlay = MyLocationNewOverlay(mLocProvider, map)
        mLocOverlay.enableMyLocation()
        mLocOverlay.enableFollowLocation()
        mLocOverlay.runOnFirstFix {
            map.overlays.clear()
            map.overlays.add(mLocOverlay)
        }
    }

    private fun checkLocPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkPermissionAfter10()
        } else {
            checkPermissionBefore10()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissionAfter10() {
        if (checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) && checkPermission(
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        ) {
            initOSM()
            checkLocationEnabled()
        } else {
            pLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            )
        }
    }

    private fun checkPermissionBefore10() {
        if (checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            initOSM()
            checkLocationEnabled()
        } else {
            pLauncher.launch(
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
    }

    private fun checkLocationEnabled() {
        val lManager = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isEnabled = lManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isEnabled) {
            showToast("GPS выключен!")
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}