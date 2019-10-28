package com.example.nakz

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.otaliastudios.cameraview.Facing
import husaynhakeem.io.facedetector.FaceBoundsOverlay
import husaynhakeem.io.facedetector.FaceDetector
import husaynhakeem.io.facedetector.models.Frame
import husaynhakeem.io.facedetector.models.Size
import kotlinx.android.synthetic.main.cam_activity.*

class FaceDetectMain : Activity() {

    private val faceDetector: FaceDetector by lazy {
        FaceDetector(facesBoundsOverlay)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cam_activity)
        setupCamera()
    }

    private fun setupCamera() {
        cameraView.facing = Facing.FRONT
        cameraView.addFrameProcessor {
            Log.i("orientation", FaceBoundsOverlay.centerX.toString() + " " + FaceBoundsOverlay.centerY.toString())
            Log.i("offset",FaceBoundsOverlay.xOffset.toString() + " " + FaceBoundsOverlay.yOffset.toString())
            faceDetector.process(
                Frame(
                    data = it.data,
                    rotation = it.rotation,
                    size = Size(it.size.width, it.size.height),
                    format = it.format,
                    isCameraFacingBack = cameraView.facing == Facing.BACK
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        cameraView.start()
    }

    override fun onPause() {
        super.onPause()
        cameraView.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.destroy()
    }
}
