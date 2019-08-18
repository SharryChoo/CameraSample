package com.sharry.sample.camera

import android.Manifest
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import com.sharry.lib.camera.AspectRatio
import com.sharry.lib.camera.SCameraView
import com.sharry.libcore.permission.PermissionsManager
import com.sharry.libtoolbar.ImageViewOptions
import kotlinx.android.synthetic.main.activity_record_video.*

private val ASPECT_RATIOS = arrayOf(
    AspectRatio.of(1, 1),   // 1:1
    AspectRatio.of(4, 3),   // 4:3
    AspectRatio.of(16, 9),  // 16:9
    AspectRatio.of(18, 9)   // 18:9
)

class MainActivity : AppCompatActivity(), AspectRatioFragment.Listener {

    private var isGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_video)
        initTitle()
        initViews()
        PermissionsManager.getManager(this)
            .request(Manifest.permission.CAMERA)
            .execute { granted ->
                isGranted = granted
                if (!granted) {
                    finish()
                } else {
                    cameraView.startPreview()
                }
            }
    }

    override fun onResume() {
        super.onResume()
        if (isGranted) {
            cameraView.startPreview()
        }
    }

    override fun onPause() {
        if (isGranted) {
            cameraView.stopPreview()
        }
        super.onPause()
    }

    private fun initTitle() {
        val paddingSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 20f,
            resources.displayMetrics
        ).toInt()
        toolbar.addLeftMenuImage(
            ImageViewOptions.Builder()
                .setDrawableResId(R.drawable.ic_activity_video_record_full_screen)
                .setPaddingLeft(paddingSize)
                .setListener {
                    cameraView.adjustViewBounds = !cameraView.adjustViewBounds
                }
                .build()
        )
        toolbar.addLeftMenuImage(
            ImageViewOptions.Builder()
                .setDrawableResId(R.drawable.ic_activity_video_record_aspect)
                .setPaddingLeft(paddingSize)
                .setListener {
                    AspectRatioFragment.newInstance(ASPECT_RATIOS, cameraView.aspectRatio)
                        .show(supportFragmentManager, AspectRatioFragment::class.java.simpleName)
                }
                .build()
        )
        toolbar.addLeftMenuImage(
            ImageViewOptions.Builder()
                .setDrawableResId(R.drawable.ic_activity_video_record_camera_switch)
                .setPaddingLeft(paddingSize)
                .setListener {
                    val curFacing = cameraView.facing
                    cameraView.facing = if (curFacing == SCameraView.FACING_FRONT) {
                        SCameraView.FACING_BACK
                    } else {
                        SCameraView.FACING_FRONT
                    }
                }
                .build()
        )
    }

    private fun initViews() {
        cameraView.autoFocus = true
    }

    override fun onAspectRatioSelected(ratio: AspectRatio) {
        cameraView.aspectRatio = ratio
    }

}
