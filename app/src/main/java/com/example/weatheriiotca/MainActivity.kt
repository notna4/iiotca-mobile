package com.example.weatheriiotca

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {
    private lateinit var database: FirebaseDatabase
    private lateinit var temperatures: MutableList<Int>
    private lateinit var humidities: MutableList<Int>
    private lateinit var statusTextView: TextView
    private lateinit var avgTempTextView: TextView
    private lateinit var avgHumTextView: TextView
    private lateinit var button: Button

    private val CHANNEL_ID = "humidity_alerts_channel"
    private val NOTIFICATION_ID = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableEdgeToEdge()

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance()

        // Initialize lists to hold temperatures and humidities
        temperatures = mutableListOf()
        humidities = mutableListOf()

        // Find the TextView and Button in your layout
        statusTextView = findViewById(R.id.temperature)
        avgTempTextView = findViewById(R.id.avgTemp)
        avgHumTextView = findViewById(R.id.avgHum)
        button = findViewById(R.id.button)

        // Set up the button click listener to call the clicked function
        button.setOnClickListener {
            clicked(it)
        }

        // Fetch data from Realtime Database
        fetchDataFromDatabase()
    }

    private fun fetchDataFromDatabase() {
        val dbRef = database.reference
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                temperatures.clear()
                humidities.clear()

                // Assuming your data structure is under "Status"
                val statusSnapshot = snapshot.child("Status")

                // Iterate over temperature and humidity data under "Status"
                statusSnapshot.children.forEach { data ->
                    val temperature = data.child("temperature").getValue(Int::class.java)
                    val humidity = data.child("humidity").getValue(Int::class.java)

                    temperature?.let { temperatures.add(it) }
                    humidity?.let { humidities.add(it) }
                }

                val avgTemp = temperatures.average().toInt()
                // Calculate average humidity
                val avgHum = humidities.average().toInt()

                val liveTemp = snapshot.child("temperature").getValue(Int::class.java)
                val liveHum = snapshot.child("humidity").getValue(Int::class.java)

                statusTextView.text = "$liveTemp°C | $liveHum%"
                avgTempTextView.text = "$avgTemp°C"
                avgHumTextView.text = "$avgHum%"

                // Show notification if live humidity exceeds 50%
                liveHum?.let {
                    if (it > 0) {
                        Log.d("Humidity and Temperature Check", "Humidity or temperature has exceeded the threshold.")
                        // Uncomment the following line to show the notification
                         showHumidityNotification(it)
                    }
                }
            }



            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    fun clicked(view: View?) {
        val dbRef = database.reference
        dbRef.child("button").setValue(true)
            .addOnSuccessListener {
                Log.d("DatabaseUpdate", "Successfully set 'button' to true")
            }
            .addOnFailureListener {
                Log.d("DatabaseUpdate", "Failed to set 'button' to true")
            }
    }

    private fun showHumidityNotification(humidity: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request the permission if it's not granted
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
                return
            }
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notif) // Ensure you have this icon in your drawable folder
            .setContentTitle("High Humidity Alert")
            .setContentText("Humidity has exceeded 50%. Current humidity: $humidity%")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, you can notify the user
            } else {
                // Permission denied, handle the denial
            }
        }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

    private fun enableEdgeToEdge() {
        // Your implementation for enabling edge-to-edge mode
        // This method should handle enabling immersive mode, if necessary
    }
}
