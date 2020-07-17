package com.sandoval.hmsimagekitdemo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.huawei.hms.image.vision.ImageVision
import com.huawei.hms.image.vision.ImageVision.VisionCallBack
import com.huawei.hms.image.vision.ImageVisionImpl
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ImageKitVisionDemoActivity : AppCompatActivity(), View.OnClickListener {
    private val tag = "FilterActivity"
    private var permissions = arrayOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
    )
    private val mRequestCode = 100
    private var mPermissionList: MutableList<String> = ArrayList()
    private var executorService: ExecutorService = Executors.newFixedThreadPool(1)
    private var btnSubmit: Button? = null
    private var btnInit: Button? = null
    private var btnPicture: Button? = null
    private var btnStop: Button? = null
    private var btnFilter: EditText? = null
    private var btnCompress: EditText? = null
    private var btnIntensity: EditText? = null
    private var iv: ImageView? = null
    private var tv: TextView? = null
    private var tv2: TextView? = null
    private var string =
        "{\"projectId\":\"projectIdTest\",\"appId\":\"appIdTest\",\"authApiKey\":\"authApiKeyTest\",\"clientSecret\":\"clientSecretTest\",\"clientId\":\"clientIdTest\",\"token\":\"tokenTest\"}"
    private var authJson: JSONObject? = null


    private val context: Context? = null
    private var bitmap: Bitmap? = null
    private var initCodeState = -2
    private var stopCodeState = -2

    var imageVisionAPI: ImageVisionImpl? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initPermission()
        iv = findViewById(R.id.iv)
        tv = findViewById(R.id.tv)
        tv2 = findViewById(R.id.tv2)
        btnFilter = findViewById(R.id.btn_filter)
        btnInit = findViewById(R.id.btn_init)
        btnPicture = findViewById(R.id.btn_picture)
        btnIntensity = findViewById(R.id.btn_intensity)
        btnCompress = findViewById(R.id.btn_compress)
        btnSubmit = findViewById(R.id.btn_submit)
        btnStop = findViewById(R.id.btn_stop)
        btnSubmit!!.setOnClickListener(this)
        btnInit!!.setOnClickListener(this)
        btnStop!!.setOnClickListener(this)
        btnPicture!!.setOnClickListener(this)
        initJson()
    }

    private fun initJson() {
        try {
            authJson = JSONObject(string)
        } catch (e: JSONException) {
            Log.e(tag, e.toString())
        }
    }

    private fun initPermission() {
        mPermissionList.clear()
        for (i in permissions.indices) {
            if (ContextCompat.checkSelfPermission(this, permissions[i])
                != PackageManager.PERMISSION_GRANTED
            ) {
                mPermissionList.add(permissions[i])
            }
        }
        if (mPermissionList.size > 0) {
            ActivityCompat.requestPermissions(this, permissions, mRequestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var hasPermissionDismiss = false
        if (mRequestCode == requestCode) {
            for (i in grantResults.indices) {
                if (grantResults[i] == -1) {
                    hasPermissionDismiss = true
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (null != data) {
            if (resultCode == Activity.RESULT_OK) {
                when (requestCode) {
                    801 -> try {
                        val uri: Uri? = data.data
                        iv!!.setImageURI(uri)
                        bitmap = (iv!!.drawable as BitmapDrawable).bitmap
                    } catch (e: Exception) {
                        Log.e(tag, "Exception: " + e.message)
                    }
                }
            }
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.btn_submit -> {
                val filterType = btnFilter!!.text.toString()
                val intensity = btnIntensity!!.text.toString()
                val compress = btnCompress!!.text.toString()
                if ((initCodeState != 0 or stopCodeState).equals(0)) {
                    tv2!!.text =
                        "The service has not been initialized. Please initialize the service before calling it."
                }
                startFilter(filterType, intensity, compress, authJson)
            }
            R.id.btn_init -> initFilter(context)
            R.id.btn_picture -> getByAlbum(this)
            R.id.btn_stop -> stopFilter()
        }
    }


    private fun getByAlbum(act: Activity) {
        val getAlbum = Intent(Intent.ACTION_GET_CONTENT)
        val mimeTypes =
            arrayOf("image/jpeg", "image/png", "image/webp", "image/gif")
        getAlbum.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        getAlbum.type = "image/*"
        getAlbum.addCategory(Intent.CATEGORY_OPENABLE)
        act.startActivityForResult(getAlbum, 801)
    }

    @SuppressLint("SetTextI18n")
    private fun stopFilter() {
        if (null != imageVisionAPI) {
            val stopCode = imageVisionAPI!!.stop()
            tv2!!.text = "stopCode:$stopCode"
            iv!!.setImageBitmap(null)
            bitmap = null
            stopCodeState = stopCode
        } else {
            tv2!!.text = "The service has not been enabled."
            stopCodeState = 0
        }
    }

    private fun initFilter(context: Context?) {
        imageVisionAPI = ImageVision.getInstance(this)
        imageVisionAPI!!.setVisionCallBack(object : VisionCallBack {
            @SuppressLint("SetTextI18n")
            override fun onSuccess(successCode: Int) {
                val initCode = imageVisionAPI!!.init(context, authJson)
                tv2!!.text = "initCode:$initCode"
                val start2 = System.currentTimeMillis()
                initCodeState = initCode
                stopCodeState = -2
            }

            @SuppressLint("SetTextI18n")
            override fun onFailure(errorCode: Int) {
                Log.e(tag, "getImageVisionAPI failure, errorCode = $errorCode")
                tv2!!.text = "initFailed"
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun startFilter(
        filterType: String, intensity: String,
        compress: String, authJson: JSONObject?
    ) {
        val runnable = Runnable {
            val start3 = System.currentTimeMillis()
            val jsonObject = JSONObject()
            val taskJson = JSONObject()
            try {
                taskJson.put("intensity", intensity)
                taskJson.put("filterType", filterType)
                taskJson.put("compressRate", compress)
                jsonObject.put("requestId", "1")
                jsonObject.put("taskJson", taskJson)
                jsonObject.put("authJson", authJson)
                val start4 = System.currentTimeMillis()
                Log.e(tag, "prepare request parameters cost" + (start4 - start3))
                val visionResult = imageVisionAPI!!.getColorFilter(
                    jsonObject,
                    bitmap
                )
                val start5 = System.currentTimeMillis()
                Log.e(tag, "interface response cost" + (start5 - start4))
                iv!!.post(Runnable {
                    val image = visionResult.image
                    iv!!.setImageBitmap(image)
                    tv!!.text = visionResult.response.toString() + "resultCode:" + visionResult
                        .resultCode
                })
            } catch (e: JSONException) {
                Log.e(tag, "JSONException: " + e.message)
            }
        }
        executorService.execute(runnable)
    }
}