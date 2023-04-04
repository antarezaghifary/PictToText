package com.samrez.picttotext.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.samrez.picttotext.R
import com.samrez.picttotext.databinding.ActivityAddPictResultBinding
import com.samrez.picttotext.model.ItemModel

class AddPictResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPictResultBinding
    private lateinit var scannedBitmap: Bitmap
    private var bitmapState: Bitmap? = null
    private lateinit var textResult: String

    private lateinit var dbReference: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPictResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        initListener()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }


    private fun initListener() {
        binding.tvResultCapture.clearFocus()
        binding.btnTakePicture.setOnClickListener {
            val pictureDialog = AlertDialog.Builder(this)
            pictureDialog.setTitle("Choose action:")
            val pictureDialogItem = arrayOf(
                "Take from Gallery",
                "Take from Camera"
            )
            pictureDialog.setItems(pictureDialogItem) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openCamera()
                }
            }
            pictureDialog.show()
        }

        binding.btnEditResult.setOnClickListener {
            val intent = Intent(this, EditResultTextActivity::class.java)
            intent.putExtra("textResult", textResult)
            launcherIntentEditResult.launch(intent)
        }

        binding.btnUploadResult.setOnClickListener {

            FirebaseApp.initializeApp(this)
            dbReference = FirebaseDatabase.getInstance("https://picttotext-default-rtdb.firebaseio.com/").getReference("items")

            val item = ItemModel(textResult)

            val databaseReference = FirebaseDatabase.getInstance("https://picttotext-default-rtdb.firebaseio.com/").reference
            val id = databaseReference.push().key
            dbReference.child(id.toString()).setValue(item).addOnCompleteListener {
                Toast.makeText(this, "Success upload data to Firebase", Toast.LENGTH_SHORT).show()

                textResult = ""
                binding.tvResultCapture.text = textResult
                binding.btnEditResult.visibility = View.GONE
                binding.btnUploadResult.visibility = View.GONE
            }.addOnFailureListener {
                Toast.makeText(this, "Failed upload data to Firebase", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private val launcherIntentEditResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            textResult = it.data?.extras?.get("textResult").toString()
            binding.tvResultCapture.text = textResult
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        launcherIntentCamera.launch(intent)
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
            val bitmap = it.data?.extras?.get("data") as Bitmap
            scannedBitmap = bitmap
            bitmapState = bitmap
            Glide.with(this).load(bitmap).into(binding.ivCapture)
            detectText(scannedBitmap)
        }
    }

    private fun openGallery() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        val chooser = Intent.createChooser(intent, "Choose a Picture")
        launcherIntentGallery.launch(chooser)
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            if (result.data?.data != null) {
                scannedBitmap =
                    MediaStore.Images.Media.getBitmap(this.contentResolver, result.data?.data)
                bitmapState =
                    MediaStore.Images.Media.getBitmap(this.contentResolver, result.data?.data)
            }
            Glide.with(this).load(result.data?.data).into(binding.ivCapture)

            detectText(scannedBitmap)
        }
    }

    private fun detectText(bitmap: Bitmap) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = imageFromBitmap(bitmap)

        recognizer.process(image)
            .addOnSuccessListener {
                binding.tvResultCapture.text = it.text
                textResult = it.text
                Toast.makeText(this, "Success detect text", Toast.LENGTH_SHORT).show()

                if (it.text == "") {
                    binding.btnEditResult.visibility = View.GONE
                    binding.btnUploadResult.visibility = View.GONE
                    binding.tvResultCapture.text = "-"
                } else {
                    binding.btnEditResult.visibility = View.VISIBLE
                    binding.btnUploadResult.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(this, "Failed detect text", Toast.LENGTH_SHORT).show()
                binding.btnEditResult.visibility = View.GONE
                binding.btnUploadResult.visibility = View.GONE
            }
    }

    private fun imageFromBitmap(bitmap: Bitmap): InputImage {
        return InputImage.fromBitmap(bitmap, 0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (!allPermissionsGranted()) {
                Toast.makeText(
                    this,
                    "No have permission.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS =
            arrayOf(
                android.Manifest.permission.CAMERA
            )
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}