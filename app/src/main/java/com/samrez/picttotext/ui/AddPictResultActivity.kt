package com.samrez.picttotext.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.samrez.picttotext.R
import com.samrez.picttotext.databinding.ActivityAddPictResultBinding

class AddPictResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPictResultBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPictResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}