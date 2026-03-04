package com.example.logger

import androidx.lifecycle.ViewModel
import com.github.mikephil.charting.data.Entry

class GyroscopeViewModel : ViewModel() {
    val entriesX = ArrayList<Entry>()
    val entriesY = ArrayList<Entry>()
    val entriesZ = ArrayList<Entry>()

    val allEntriesX = ArrayList<Entry>()
    val allEntriesY = ArrayList<Entry>()
    val allEntriesZ = ArrayList<Entry>()

    var isRecording = false
    var startTime: Long = 0L
}