package com.example.logger

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
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
            showSensorsListDialog()
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

    private fun showSensorsListDialog() {
        val sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)

        if (sensors.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Список датчиков")
                .setMessage("На устройстве не найдено датчиков")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val sensorItems = sensors.map { sensor ->
            "${sensor.name} (${sensor.stringType})"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Все датчики")
            .setItems(sensorItems) { dialog, which ->
                val selectedSensor = sensors[which]
                showSensorDetails(selectedSensor)
            }
            .setPositiveButton("Закрыть", null)
            .show()
    }

    private fun showSensorDetails(sensor: Sensor) {
        val details = """
            Название: ${sensor.name}
            Тип: ${sensor.stringType}
            Производитель: ${sensor.vendor}
            Версия: ${sensor.version}
            Разрешение: ${sensor.resolution}
            Макс. диапазон: ${sensor.maximumRange}
            Потребление: ${sensor.power} мА
            Минимальная задержка: ${sensor.minDelay} мкс
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Характеристики датчика")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }
}