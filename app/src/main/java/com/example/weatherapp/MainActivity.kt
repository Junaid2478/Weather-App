package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import okhttp3.*
import okio.IOException
import org.json.JSONObject
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var loading: ConstraintLayout
    private lateinit var nameText: TextView
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var tempText: TextView
    private lateinit var feelsLikeText: TextView

    val PERMISSION_ID = 42
    lateinit var mFusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById(R.id.weatherButton)
        val edittext: EditText = findViewById(R.id.cityName)
        loading = findViewById(R.id.loading)
        // I've added a loading screen for when a different city is inputted
        nameText = findViewById(R.id.name)
        titleText = findViewById(R.id.title)
        subtitleText = findViewById(R.id.subtitle)
        tempText = findViewById(R.id.temp)
        feelsLikeText = findViewById(R.id.feels_like)

        //taking text from edit text and adding it as a parameter to getWeather
        button.setOnClickListener { view ->
            val text = edittext.text.toString()

            if (text != "") {
                val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view!!.getWindowToken(), 0)
                getWeather(text)
               //gets the input service our app is using e.g. keyboard to remove it
            } else {
                Toast.makeText(this@MainActivity, "Enter City", Toast.LENGTH_LONG).show()
                // if the inputted text is empty the toast displays enter city
            }
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLastLocation()
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()) {
            if (isLocationEnabled()) {

                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    var location: Location? = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        getWeatherWithCoordinates(location)
                    }
                }
            } else {
                Toast.makeText(this, "Turn on location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation: Location = locationResult.lastLocation
            getWeatherWithCoordinates(mLastLocation)
        }
    }

    private fun isLocationEnabled(): Boolean {
        var locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_ID
        )
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_ID) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
        }
    }

    private fun clearTexts() {
            titleText.text = ""
            subtitleText.text = ""
            tempText.text = ""
            feelsLikeText.text = ""
        // returns empty strings for the results if the input is invalid
    }

    /*getWeather takes the city String as a parameter and uses it along with the api key (from strings.xml) to build the url for the http
     request to the open weather api */

    private fun getWeather(city: String) {
        loading.visibility = View.VISIBLE

        val apiKey = getString(R.string.api_key)

        val request = Request.Builder()
            .url("https://api.openweathermap.org/data/2.5/weather?q=$city&units=metric&appid=$apiKey")
            .build()

        createClient(request)
    }

    private fun getWeatherWithCoordinates(location: Location) {
        loading.visibility = View.VISIBLE

        val apiKey = getString(R.string.api_key)

        val request = Request.Builder()
            .url("https://api.openweathermap.org/data/2.5/weather?lat=${location.latitude}&lon=${location.longitude}&units=metric&appid=$apiKey")
            .build()

        createClient(request)
    }

    private fun createClient(request: Request) {
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                //Handler creates seperate thread that runs code from the main thread of the activity allowing you to change UI elements
                Handler(Looper.getMainLooper()).post {
                    clearTexts()
                    loading.visibility = View.GONE
                    Toast.makeText(this@MainActivity, e.localizedMessage, Toast.LENGTH_LONG).show()
                    // the toast displays the exception in the local language of the user
                }
            }

            override fun onResponse(call: Call, response: Response) {
                Handler(Looper.getMainLooper()).post {
                    loading.visibility = View.GONE

                }
                if (response.isSuccessful) {
                    var body = response.body?.string()
                    val json = JSONObject(body)

                    var name = json.getString("name")
                    var weather = json.getJSONArray("weather").get(0) as JSONObject
                    var title = weather.getString("main")
                    var desc = weather.getString("description")

                    var main = json.getJSONObject("main")
                    var temp = main.getDouble("temp")
                    var feelsLike = main.getDouble("feels_like")

                    // These are the different JSON objects choosen to display

                    // running from main thread
                    Handler(Looper.getMainLooper()).post {
                        nameText.text = name
                        titleText.text = title
                        subtitleText.text = desc
                        tempText.text = "Temperature is " + temp.toString() + "\u2103"
                        feelsLikeText.text = "Feels like " + feelsLike.toString() + "\u2103"
                        //adding the degrees celsius to the text
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        clearTexts()
                        loading.visibility = View.GONE
                        Toast.makeText(this@MainActivity, response.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
