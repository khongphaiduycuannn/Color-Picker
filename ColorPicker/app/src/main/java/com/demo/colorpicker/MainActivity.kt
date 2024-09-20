package com.demo.colorpicker

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.demo.colorpicker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.cpvColorPickerView.setAlphaSliderVisible(true)
        binding.cpvColorPickerView.setOnColorChangedListener {
            binding.vCurrentColor.setBackgroundColor(it)
        }
    }
}