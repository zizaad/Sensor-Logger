package com.example.logger   // замените на ваш пакет

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment

class MainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnToggleTheme: Button = view.findViewById(R.id.btnToggleTheme)
        val btnSensorList: Button = view.findViewById(R.id.btnSensorList)
        val btnAccelerometer: Button = view.findViewById(R.id.btnAccelerometer)
        val btnGyroscope: Button = view.findViewById(R.id.btnGyroscope)

        btnToggleTheme.setOnClickListener {
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            requireActivity().recreate()
        }

        btnSensorList.setOnClickListener {
            Toast.makeText(requireContext(), "Здесь будет список всех датчиков", Toast.LENGTH_SHORT).show()
        }

        btnAccelerometer.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AccelerometerFragment())
                .addToBackStack(null)
                .commit()
        }

        btnGyroscope.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GyroscopeFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}