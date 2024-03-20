package com.baes.inspector.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.baes.inspector.MainViewModal
import com.baes.inspector.R
import com.baes.inspector.databinding.FragmentMainBinding
import com.baes.inspector.location.LocationModel
import com.baes.inspector.location.LocationService
import com.baes.inspector.utils.DialogManager
import com.baes.inspector.utils.TimeUtils
import com.baes.inspector.utils.checkPermission
import com.baes.inspector.utils.showToast
import org.osmdroid.config.Configuration
import org.osmdroid.library.BuildConfig
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Timer
import java.util.TimerTask

class MainFragment : Fragment() {
    private var pl: Polyline? = null
    private var isServiceRunning = false
    private var isFirstStart = true
    private var timer: Timer? = null
    private var startTime = 0L
    private lateinit var pLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var binding: FragmentMainBinding
    private lateinit var mLocOverlay: MyLocationNewOverlay
    private val model: MainViewModal by activityViewModels()

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
        setOnClicks()
        checkServiceState()
        updateTime()
        registerLocReceiver()
        locUpdates()
    }

    private fun setOnClicks() = with(binding) {
        val listener = onClicks()
        fStartStop.setOnClickListener(listener)
    }

    private fun onClicks(): View.OnClickListener {
        return View.OnClickListener {
            when (it.id) {
                R.id.fStartStop -> startStopService()
            }
        }
    }

    private fun locUpdates() = with(binding) {
        model.locUpdates.observe(viewLifecycleOwner) {
            val distance = "Distance: ${String.format("%.1f", it.distance)} m"
            val velocity = "Velocity: ${String.format("%.1f", 3.6f * it.velocity)} km/h"
            val aVelocity = "Average velocity: ${getAverageSpeed(it.distance)} km/h"
            tvDistance.text = distance
            tvVelocity.text = velocity
            tvAverageVel.text = aVelocity
            updatePolyline(it.deoPointsList)
        }
    }

    private fun updateTime() {
        model.timeData.observe(viewLifecycleOwner) {
            binding.tvTime.text = it
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = Timer()
        startTime = LocationService.startTime
        timer?.schedule(object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    model.timeData.value = getCurrentTime()
                }
            }

        }, 1000, 1000)
    }

    private fun getAverageSpeed(distance: Float): String {
        return String().format(
            "%.1f",
            3.6f * (distance / ((System.currentTimeMillis() - startTime) / 1000.0f))
        )
    }

    private fun getCurrentTime(): String {
        return "Time: ${TimeUtils.getTime(System.currentTimeMillis() - startTime)}"
    }

    private fun startStopService() {
        if (!isServiceRunning) {
            startLocService()
        } else {
            activity?.stopService(Intent(activity, LocationService::class.java))
            binding.fStartStop.setImageResource(R.drawable.ic_play)
            timer?.cancel()
        }
        isServiceRunning = !isServiceRunning
    }

    private fun checkServiceState() {
        isServiceRunning = LocationService.isRunning
        if (isServiceRunning) {
            binding.fStartStop.setImageResource(R.drawable.ic_stop)
            startTimer()
        }
    }

    private fun startLocService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity?.startForegroundService(Intent(activity, LocationService::class.java))
        } else {
            activity?.startService(Intent(activity, LocationService::class.java))
        }
        binding.fStartStop.setImageResource(R.drawable.ic_stop)
        LocationService.startTime = System.currentTimeMillis()
        startTimer()
    }

    override fun onResume() {
        super.onResume()
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
        pl = Polyline()
        pl?.outlinePaint?.color = Color.RED
        map.controller.setZoom(16.0)
        val mLocProvider = GpsMyLocationProvider(activity)
        mLocOverlay = MyLocationNewOverlay(mLocProvider, map)
        mLocOverlay.enableMyLocation()
        mLocOverlay.enableFollowLocation()
        mLocOverlay.runOnFirstFix {
            map.overlays.clear()
            map.overlays.add(mLocOverlay)
            map.overlays.add(pl)
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
            DialogManager.showLocEnableDialog(
                activity as AppCompatActivity,
                object : DialogManager.Listener {
                    override fun onClick() {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                }
            )
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationService.LOC_MODEL_INTENT) {
                val locModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(
                        LocationService.LOC_MODEL_INTENT,
                        LocationModel::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra(LocationService.LOC_MODEL_INTENT) as? LocationModel
                }
                model.locUpdates.value = locModel
            }
        }
    }

    private fun registerLocReceiver() {
        val locFilter = IntentFilter(LocationService.LOC_MODEL_INTENT)
        LocalBroadcastManager.getInstance(activity as AppCompatActivity)
            .registerReceiver(receiver, locFilter)
    }

    private fun addPoint(list: List<GeoPoint>) {
        pl?.addPoint(list[list.size - 1])
    }

    private fun fillPolyline(list: List<GeoPoint>) {
        list.forEach {
            pl?.addPoint(it)
        }
    }

    private fun updatePolyline(list: List<GeoPoint>) {
        if (list.size > 1 && isFirstStart) {
            fillPolyline(list)
            isFirstStart = false
        } else {
            addPoint(list)
        }
    }

    override fun onDetach() {
        super.onDetach()
        LocalBroadcastManager.getInstance(activity as AppCompatActivity)
            .unregisterReceiver(receiver)
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}