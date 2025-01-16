package com.example.facecontour

import androidx.camera.core.Preview.SurfaceProvider
import android.view.Surface
import android.view.TextureView
import androidx.camera.core.SurfaceRequest
import androidx.core.content.ContextCompat

class TextureViewSurfaceProvider(private val textureView: TextureView) : SurfaceProvider {
    override fun onSurfaceRequested(request: SurfaceRequest) {
        val surfaceTexture = textureView.surfaceTexture
        surfaceTexture?.let {
            it.setDefaultBufferSize(request.resolution.width, request.resolution.height)
            val surface = Surface(it)
            request.provideSurface(surface, ContextCompat.getMainExecutor(textureView.context)) {
                surface.release()
            }
        }
    }
}