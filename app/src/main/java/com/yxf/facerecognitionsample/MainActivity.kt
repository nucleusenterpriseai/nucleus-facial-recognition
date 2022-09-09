package com.yxf.facerecognitionsample

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.app.ActivityCompat
import com.yxf.facerecognition.test.GlobalInfo
import com.yxf.facerecognitionsample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {


    private val vb by lazy { ActivityMainBinding.inflate(LayoutInflater.from(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GlobalInfo.init(this)
        setContentView(vb.root)
        vb.addFace.setOnClickListener {
            startActivity(Intent(this, AddFaceActivity::class.java))
        }
        vb.recognition.setOnClickListener {
            startActivity(Intent(this, FaceRecognitionActivity::class.java))
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1000)



    }
}