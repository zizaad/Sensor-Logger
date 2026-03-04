// AccelerometerFragment.kt
package com.example.logger

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AccelerometerFragment : Fragment(), SensorEventListener {

    private lateinit var viewModel: AccelerometerViewModel

    private lateinit var chartX: LineChart
    private lateinit var chartY: LineChart
    private lateinit var chartZ: LineChart

    private lateinit var tvX: TextView
    private lateinit var tvY: TextView
    private lateinit var tvZ: TextView
    private lateinit var tvAbs: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private val maxDataPoints = 100

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_accelerometer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity()).get(AccelerometerViewModel::class.java)

        chartX = view.findViewById(R.id.chartX)
        chartY = view.findViewById(R.id.chartY)
        chartZ = view.findViewById(R.id.chartZ)
        tvX = view.findViewById(R.id.tvX)
        tvY = view.findViewById(R.id.tvY)
        tvZ = view.findViewById(R.id.tvZ)
        tvAbs = view.findViewById(R.id.tvAbs)
        btnStart = view.findViewById(R.id.btnStart)
        btnStop = view.findViewById(R.id.btnStop)
        btnSave = view.findViewById(R.id.btnSave)
        btnBack = view.findViewById(R.id.btnBack)

        setupChart(chartX, Color.parseColor("#FF4444"), "X")
        setupChart(chartY, Color.parseColor("#00AA00"), "Y")
        setupChart(chartZ, Color.parseColor("#3333FF"), "Z")

        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            tvX.text = "Акселерометр не найден"
            btnStart.isEnabled = false
        }

        updateUIFromViewModel()

        btnStart.setOnClickListener { startRecording() }
        btnStop.setOnClickListener { stopRecording() }
        btnSave.setOnClickListener { saveToCSV() }
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupChart(chart: LineChart, color: Int, label: String) {
        val context = requireContext()
        val textColor = ContextCompat.getColor(context, R.color.chart_text)
        val gridColor = ContextCompat.getColor(context, R.color.chart_grid)

        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        chart.setTouchEnabled(true)
        chart.isDragEnabled = true
        chart.setScaleEnabled(true)
        chart.setDrawGridBackground(false)
        chart.setPinchZoom(true)

        val xAxis = chart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(true)
        xAxis.granularity = 1f
        xAxis.textColor = textColor
        xAxis.gridColor = gridColor
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return String.format("%.1f", value / 1000f)
            }
        }

        val leftAxis = chart.axisLeft
        leftAxis.setDrawGridLines(true)
        leftAxis.textColor = textColor
        leftAxis.gridColor = gridColor

        chart.axisRight.isEnabled = false
        chart.data = null
    }

    private fun updateUIFromViewModel() {
        btnStart.isEnabled = !viewModel.isRecording
        btnStop.isEnabled = viewModel.isRecording

        if (viewModel.allEntriesX.isNotEmpty()) {
            val lastX = viewModel.allEntriesX.last().y
            val lastY = viewModel.allEntriesY.last().y
            val lastZ = viewModel.allEntriesZ.last().y
            val lastAbs = kotlin.math.sqrt(lastX * lastX + lastY * lastY + lastZ * lastZ)
            tvX.text = String.format("X: %.2f", lastX)
            tvY.text = String.format("Y: %.2f", lastY)
            tvZ.text = String.format("Z: %.2f", lastZ)
            tvAbs.text = String.format("Abs: %.2f", lastAbs)
        }

        if (viewModel.entriesX.isNotEmpty()) {
            updateCharts()
        }
    }

    private fun startRecording() {
        if (accelerometer == null) return

        viewModel.isRecording = true
        viewModel.startTime = System.currentTimeMillis()

        viewModel.entriesX.clear()
        viewModel.entriesY.clear()
        viewModel.entriesZ.clear()
        viewModel.allEntriesX.clear()
        viewModel.allEntriesY.clear()
        viewModel.allEntriesZ.clear()

        updateUIForRecordingState()

        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )
    }

    private fun stopRecording() {
        viewModel.isRecording = false
        updateUIForRecordingState()
        sensorManager.unregisterListener(this)
    }

    private fun updateUIForRecordingState() {
        btnStart.isEnabled = !viewModel.isRecording
        btnStop.isEnabled = viewModel.isRecording
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!viewModel.isRecording) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val elapsedMs = (System.currentTimeMillis() - viewModel.startTime).toFloat()

            requireActivity().runOnUiThread {
                val abs = kotlin.math.sqrt(x * x + y * y + z * z)
                tvX.text = String.format("X: %.2f", x)
                tvY.text = String.format("Y: %.2f", y)
                tvZ.text = String.format("Z: %.2f", z)
                tvAbs.text = String.format("Abs: %.2f", abs)

                viewModel.entriesX.add(Entry(elapsedMs, x))
                viewModel.entriesY.add(Entry(elapsedMs, y))
                viewModel.entriesZ.add(Entry(elapsedMs, z))

                if (viewModel.entriesX.size > maxDataPoints) {
                    viewModel.entriesX.removeAt(0)
                    viewModel.entriesY.removeAt(0)
                    viewModel.entriesZ.removeAt(0)
                }

                viewModel.allEntriesX.add(Entry(elapsedMs, x))
                viewModel.allEntriesY.add(Entry(elapsedMs, y))
                viewModel.allEntriesZ.add(Entry(elapsedMs, z))

                updateCharts()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    private fun updateCharts() {
        updateChart(chartX, viewModel.entriesX, Color.parseColor("#FF4444"), "X")
        updateChart(chartY, viewModel.entriesY, Color.parseColor("#00AA00"), "Y")
        updateChart(chartZ, viewModel.entriesZ, Color.parseColor("#3333FF"), "Z")
    }

    private fun updateChart(chart: LineChart, entries: List<Entry>, color: Int, label: String) {
        if (entries.isEmpty()) return

        val dataSet = LineDataSet(entries, label).apply {
            this.color = color
            lineWidth = 2f
            setDrawValues(false)
            setCircleColor(color)
            circleRadius = 2f
        }
        chart.data = LineData(dataSet)
        chart.notifyDataSetChanged()
        chart.invalidate()

        val lastX = entries.last().x
        chart.setVisibleXRangeMaximum(maxDataPoints.toFloat())
        chart.moveViewToX(lastX - maxDataPoints)
    }

    private fun saveToCSV() {
        if (viewModel.allEntriesX.isEmpty()) {
            Toast.makeText(requireContext(), "Нет данных для сохранения", Toast.LENGTH_SHORT).show()
            return
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "accelerometer_$timeStamp.csv"

        val csvContent = StringBuilder()
        csvContent.append("Time (ms),X (m/s²),Y (m/s²),Z (m/s²),Abs (m/s²)\n")

        for (i in viewModel.allEntriesX.indices) {
            val time = viewModel.allEntriesX[i].x
            val x = viewModel.allEntriesX[i].y
            val y = viewModel.allEntriesY[i].y
            val z = viewModel.allEntriesZ[i].y
            val abs = kotlin.math.sqrt(x * x + y * y + z * z)
            csvContent.append("$time,$x,$y,$z,$abs\n")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = requireContext().contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(csvContent.toString().toByteArray())
                    Toast.makeText(requireContext(), "Файл сохранён: $filename", Toast.LENGTH_LONG).show()
                }
            } ?: Toast.makeText(requireContext(), "Ошибка создания файла", Toast.LENGTH_SHORT).show()
        } else {
            if (checkPermission()) {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, filename)
                try {
                    FileOutputStream(file).use { fos ->
                        fos.write(csvContent.toString().toByteArray())
                    }
                    Toast.makeText(requireContext(), "Файл сохранён: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Нет разрешения на запись", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requireContext().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                saveToCSV()
            } else {
                Toast.makeText(requireContext(), "Разрешение не получено", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isRecording && accelerometer != null) {
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    override fun onPause() {
        super.onPause()
        if (viewModel.isRecording) {
            sensorManager.unregisterListener(this)
        }
    }
}