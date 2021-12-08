package com.example.weatherapp

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getSystemService
import okhttp3.*
import okio.IOException
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private lateinit var loading: ConstraintLayout
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var tempText: TextView
    private lateinit var feelsLikeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById(R.id.weatherButton)
        val edittext: EditText = findViewById(R.id.cityName)
        loading = findViewById(R.id.loading)
        // I've added a loading screen for when a different city is inputted
        titleText = findViewById(R.id.title)
        subtitleText = findViewById(R.id.subtitle)
        tempText = findViewById(R.id.temp)
        feelsLikeText = findViewById(R.id.feels_like)

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
    }

    private fun clearTexts() {
            titleText.text = ""
            subtitleText.text = ""
            tempText.text = ""
            feelsLikeText.text = ""
        // returns empty strings for the results if the input is invalid
    }

    private fun getWeather(city: String) {
        loading.visibility = View.VISIBLE

        val client = OkHttpClient()
        val apiKey = getString(R.string.api_key)


        val request = Request.Builder()
            .url("https://api.openweathermap.org/data/2.5/weather?q=$city&units=metric&appid=$apiKey")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
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

                    var weather = json.getJSONArray("weather").get(0) as JSONObject
                    var title = weather.getString("main")
                    var desc = weather.getString("description")

                    var main = json.getJSONObject("main")
                    var temp = main.getDouble("temp")
                    var feelsLike = main.getDouble("feels_like")

                    // These are the different JSON objects choosen to display

                    // running from main thread
                    Handler(Looper.getMainLooper()).post {
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