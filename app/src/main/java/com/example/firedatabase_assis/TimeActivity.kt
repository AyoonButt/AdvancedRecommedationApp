package com.example.firedatabase_assis

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import android.widget.TextView
import android.widget.TimePicker
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TimeActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener{

    private val calendar = Calendar.getInstance()
    private val format = SimpleDateFormat("MMMM,dd,yyyy hh:mm a", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time)

        findViewById<TextView>(R.id.tv_textTime).setOnClickListener(){
            DatePickerDialog(this, this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        Log.e("Calendar","$year--$month--$dayOfMonth")
        calendar.set(year,month,dayOfMonth)
        displayForm(calendar.timeInMillis)
        TimePickerDialog(this,this,
            calendar.get(Calendar.HOUR),
            calendar.get(Calendar.MINUTE),false).show()
    }
    private fun displayForm(timestamp: Long){
        findViewById<TextView>(R.id.tv_textTime).text= format.format(timestamp)
        Log.i("Format",timestamp.toString())
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        calendar.apply {
            set(Calendar.HOUR, hourOfDay)
            set(Calendar.MINUTE, minute)
        }
        displayForm(calendar.timeInMillis)
    }
}