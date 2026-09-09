package com.facetracker.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.facetracker.app.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    // Permission launcher for Camera & Notification
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        if (cameraGranted) {
            startInAppCamera()
        } else {
            Toast.makeText(this, "Camera permission is required for face tracking", Toast.LENGTH_LONG).show()
        }
    }

    // Activity Result Launcher for Overlay Settings
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            startBackgroundFloatingService()
        } else {
            Toast.makeText(this, "Overlay permission is required to track over other apps", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupObservers()
        setupListeners()
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf(Manifest.permission.CAMERA)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            startInAppCamera()
        } else {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun setupObservers() {
        FaceTrackerState.blinkCount.observe(this) { blinks ->
            binding.tvBlinkCount.text = blinks.toString()
        }

        FaceTrackerState.smileCount.observe(this) { smiles ->
            binding.tvSmileCount.text = smiles.toString()
        }

        FaceTrackerState.smileProb.observe(this) { prob ->
            val percent = (prob * 100).toInt()
            binding.tvSmileRatio.text = "Smile Prob: $percent%"
        }

        FaceTrackerState.leftEyeOpenProb.observe(this) { left ->
            val right = FaceTrackerState.rightEyeOpenProb.value ?: 0f
            val avg = ((left + right) / 2f * 100).toInt()
            binding.tvEyeOpenRatio.text = "Eye Openness: $avg%"
        }

        FaceTrackerState.isFaceDetected.observe(this) { detected ->
            if (detected) {
                binding.tvFaceIndicator.text = "● FACE DETECTED"
                binding.tvFaceIndicator.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
            } else {
                binding.tvFaceIndicator.text = "● NO FACE DETECTED"
                binding.tvFaceIndicator.setTextColor(ContextCompat.getColor(this, R.color.accent_pink))
            }
        }

        FaceTrackerState.isServiceRunning.observe(this) { running ->
            if (running) {
                binding.btnToggleBackground.text = getString(R.string.stop_background)
                binding.btnToggleBackground.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_pink))
                binding.tvStatusBadge.text = "Background Active"
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_blue))
            } else {
                binding.btnToggleBackground.text = getString(R.string.start_background)
                binding.btnToggleBackground.setBackgroundColor(ContextCompat.getColor(this, R.color.accent_blue))
                binding.tvStatusBadge.text = "Camera Active"
                binding.tvStatusBadge.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
            }
        }
    }

    private fun setupListeners() {
        binding.btnToggleBackground.setOnClickListener {
            val isRunning = FaceTrackerState.isServiceRunning.value ?: false
            if (isRunning) {
                stopBackgroundFloatingService()
            } else {
                requestOverlayAndStartService()
            }
        }

        binding.btnReset.setOnClickListener {
            FaceTrackerState.resetCounters()
            Toast.makeText(this, "Counters reset to zero", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestOverlayAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            startBackgroundFloatingService()
        }
    }

    private fun startBackgroundFloatingService() {
        // Unbind in-app camera so background service can take camera control
        cameraProvider?.unbindAll()

        val intent = Intent(this, FloatingTrackerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Floating tracker started! You can now use other apps.", Toast.LENGTH_LONG).show()
    }

    private fun stopBackgroundFloatingService() {
        val intent = Intent(this, FloatingTrackerService::class.java)
        stopService(intent)
        // Resume in-app camera
        startInAppCamera()
    }

    private fun startInAppCamera() {
        val isServiceRunning = FaceTrackerState.isServiceRunning.value ?: false
        if (isServiceRunning) return // Background service currently has camera

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewViewMain.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceMetricsAnalyzer())
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onResume() {
        super.onResume()
        val isServiceRunning = FaceTrackerState.isServiceRunning.value ?: false
        if (!isServiceRunning) {
            startInAppCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
