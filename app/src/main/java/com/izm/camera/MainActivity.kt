package com.izm.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.Surface
import android.view.TextureView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.util.Size
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.graphics.Bitmap
import android.os.Environment
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private var mCameraDevice : CameraDevice? = null
    private var mBackGroundHander : Handler? = null
    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null
    private var mPreviewRequest: CaptureRequest? = null
    private var mCameraCaptureSession : CameraCaptureSession? = null
    private var mCameraSize : Size? = null

    private val mStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraDevice = cameraDevice
            createCameraPreview()
        }
        override fun onDisconnected(cameraDevice: CameraDevice) {
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            cameraDevice.close()
            mCameraDevice = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        capture_button.setOnClickListener({
            try {
                mCameraCaptureSession!!.stopRepeating()
                if (textureView.isAvailable) {
                    val root = Environment.getExternalStorageDirectory()
                    val file = File(root,"test.jpg")
                    val fos = FileOutputStream(file)
                    val bitmap = textureView.bitmap
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos)
                    fos.close()
                }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if(textureView.isAvailable){
            openCamera()
        }
        else{
            mBackGroundHander = Handler()
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
                    openCamera()
                }

                override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
                    //configureTransform(width, height)
                }

                override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                    return true
                }

                override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
                }
            }
        }
    }

    fun openCamera()
    {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        // TODO カメラID選定 : 現在はindex0固定
        val cameraId = cameraManager.cameraIdList[0]

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        mCameraSize = map.getOutputSizes(SurfaceTexture::class.java)[0]

       // TODO ImageReaderの設定

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        try {
            cameraManager.openCamera(cameraId, mStateCallback, mBackGroundHander)
        }
        catch(e: CameraAccessException){
            e.printStackTrace()
        }
    }

    fun createCameraPreview()
    {
        val texture = textureView.surfaceTexture

        texture!!.setDefaultBufferSize(mCameraSize!!.width, mCameraSize!!.height)
        val surface = Surface(texture)

        try {
            mPreviewRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mPreviewRequestBuilder!!.addTarget(surface)
            mPreviewRequest = mPreviewRequestBuilder!!.build()
        }
        catch (e: CameraAccessException){
            e.printStackTrace()
        }

        try {
            mCameraDevice!!.createCaptureSession(Arrays.asList(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    // カメラがcloseされている場合
                    if (null == mCameraDevice) {
                        return
                    }

                    mCameraCaptureSession = session

                    try {
                        session.setRepeatingRequest(mPreviewRequest, null, null)
                    } catch (e: CameraAccessException) {
                        e.printStackTrace()
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                }
            }, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }
}
