package com.example.facecontour

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.facecontour.databinding.ActivityMainBinding
import android.Manifest
import android.graphics.Rect
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
    )

    private var currentCamera = CameraSelector.DEFAULT_FRONT_CAMERA
    private lateinit var cameraProvider: ProcessCameraProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            initializeCamera()
        }

        binding.switchCameraButton.setOnClickListener {
            switchCamera()
        }
    }

    private fun initializeCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            startCamera()
            startImageAnalysis()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startCamera() {
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
            .also {
                it.surfaceProvider = TextureViewSurfaceProvider(binding.textureView)
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                currentCamera,
                preview
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startImageAnalysis() {
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                faceDetector.process(image)
                    .addOnSuccessListener { faces ->
                        val imageWidth = image.width
                        val imageHeight = image.height
                        val viewWidth = binding.textureView.width
                        val viewHeight = binding.textureView.height

                        for (face in faces) {
                            val bounds = face.boundingBox
                            val scaledBounds = scaleBoundingBox(
                                bounds,
                                imageWidth,
                                imageHeight,
                                viewWidth,
                                viewHeight,
                                imageProxy.imageInfo.rotationDegrees
                            )
                            drawFaceContour(scaledBounds)
                        }
                        imageProxy.close()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Face detection failed", e)
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }

        try {
            cameraProvider.bindToLifecycle(
                this,
                currentCamera,
                imageAnalysis
            )
        } catch (exc: Exception) {
            Log.e(TAG, "ImageAnalysis binding failed", exc)
        }
    }

    private fun scaleBoundingBox(
        bounds: Rect,
        imageWidth: Int,
        imageHeight: Int,
        viewWidth: Int,
        viewHeight: Int,
        rotationDegrees: Int
    ): Rect {
        val scaleX = viewWidth.toFloat() / imageWidth
        val scaleY = viewHeight.toFloat() / imageHeight

        return when (rotationDegrees) {
            90 -> Rect(
                (bounds.top * scaleX).toInt(),
                (viewHeight - bounds.right * scaleY).toInt(),
                (bounds.bottom * scaleX).toInt(),
                (viewHeight - bounds.left * scaleY).toInt()
            )

            180 -> Rect(
                (viewWidth - bounds.right * scaleX).toInt(),
                (viewHeight - bounds.bottom * scaleY).toInt(),
                (viewWidth - bounds.left * scaleX).toInt(),
                (viewHeight - bounds.top * scaleY).toInt()
            )

            270 -> Rect(
                (viewWidth - bounds.bottom * scaleX).toInt(),
                (bounds.left * scaleY).toInt(),
                (viewWidth - bounds.top * scaleX).toInt(),
                (bounds.right * scaleY).toInt()
            )

            else -> Rect(
                (bounds.left * scaleX).toInt(),
                (bounds.top * scaleY).toInt(),
                (bounds.right * scaleX).toInt(),
                (bounds.bottom * scaleY).toInt()
            )
        }
    }

    private fun drawFaceContour(bounds: Rect) {
        binding.faceContourView.setFaceRectangles(listOf(bounds))
    }

    private fun switchCamera() {
        currentCamera = if (currentCamera == CameraSelector.DEFAULT_FRONT_CAMERA) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        startCamera()
        startImageAnalysis()

        val cameraName =
            if (currentCamera == CameraSelector.DEFAULT_FRONT_CAMERA) "Front" else "Back"
        Toast.makeText(this, "Switched to $cameraName camera", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
        private const val TAG = "MainActivity"
    }
}