package com.example.weatherforecast

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

private const val PLACE_API_KEY = "AIzaSyBemx3O-uxiaMSzFza7az2OQc09RH1kSqM"
private const val WEATHER_API_KEY = "1fd0fe6f2296c252f30e81066ee1ced7"
private const val LOCATION_PERMISSION_REQ_CODE = 1000
private const val TAG = "autocompleteFragment"

class MainActivity : AppCompatActivity() {

    // FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // LocationRequest - Requirements for the location updates, i.e.,
    // how often you should receive updates, the priority, etc.
    private lateinit var locationRequest: LocationRequest

    // LocationCallback - Called when FusedLocationProviderClient has a new Location
    private lateinit var locationCallback: LocationCallback

    private lateinit var geocoder: Geocoder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val currentLocationButton = findViewById<View>(R.id.fab)

        // Checking location permissions
        fun isLocationPermissionGranted(): Boolean {
            // Check location permission
            return ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }

        // Request location permissions if denied
        if (!isLocationPermissionGranted()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), LOCATION_PERMISSION_REQ_CODE)
        }

        Places.initialize(applicationContext, PLACE_API_KEY)
        Places.createClient(this)

        geocoder = Geocoder(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.create().apply {
            // Sets interval for active location updates. This interval is inexact.
            interval = TimeUnit.SECONDS.toMillis(60)

            // Sets the fastest rate for active location updates. This interval is exact,
            // and your application will never receive updates more frequently than this value.
            fastestInterval = TimeUnit.SECONDS.toMillis(30)

            // Sets the maximum time when batched location updates are delivered. Updates may be
            // delivered sooner than this interval.
            maxWaitTime = TimeUnit.MINUTES.toMillis(2)

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val latitude = locationResult.lastLocation.latitude
                val longitude = locationResult.lastLocation.longitude
                getWeatherForecast(latitude, longitude)
                val city = geocoder.getFromLocation(latitude, longitude, 1)
                Toast.makeText(this@MainActivity, "Прогноз погоды для: ${city[0].locality}", Toast.LENGTH_LONG).show()
            }
        }

        currentLocationButton.setOnClickListener {
            if (isLocationPermissionGranted()) {
                subscribeToLocationUpdates()
            } else ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ), LOCATION_PERMISSION_REQ_CODE)
        }

        // Initialize the AutocompleteSupportFragment.
        val autocompleteFragment = supportFragmentManager
            .findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        autocompleteFragment.apply {
            // Specify the types of place data to return.
            setCountries("RU")
            setTypeFilter(TypeFilter.CITIES)
            setPlaceFields(arrayListOf(Place.Field.NAME, Place.Field.LAT_LNG))
            // Set up a PlaceSelectionListener to handle the response.
            setOnPlaceSelectedListener(object : PlaceSelectionListener {
                override fun onPlaceSelected(place: Place) {
                    val latitude = place.latLng?.latitude
                    val longitude = place.latLng?.longitude
                    getWeatherForecast(latitude, longitude)
                }
                override fun onError(status: Status) {
                    Log.i(TAG, "An error occurred: $status")
                }
            })
        }
    }

    private fun subscribeToLocationUpdates() {
        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper())
        } catch (unlikely: SecurityException) {
            Toast.makeText(this, "Lost location permissions. Couldn't get location updates. $unlikely", Toast.LENGTH_SHORT).show()
        }
    }

    private fun unsubscribeToLocationUpdates() {
        try {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        } catch (unlikely: SecurityException) {
            Toast.makeText(this, "Lost location permissions. Couldn't remove location updates. $unlikely", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getWeatherForecast(latitude: Double?, longitude: Double?) {
        val queue = Volley.newRequestQueue(this)
        val url = "https://api.openweathermap.org/data/2.5/onecall?lat=$latitude&lon=$longitude&exclude=current,minutely,hourly,alerts&lang=ru&units=metric&appid=$WEATHER_API_KEY"

        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                val forecastList = arrayListOf<Weather>()

                val currentDate = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("EEEE',' MMM dd")

                val jsonObject = JSONObject(response)
                val dailyArray = jsonObject.getJSONArray("daily")

                for (i in 0 until dailyArray.length()) {
                    val currentDayObject = dailyArray.getJSONObject(i)
                    val temperatureObject = currentDayObject.getJSONObject("temp")
                    val dayTemperature = temperatureObject.get("day").toString().toDouble()
                    val nightTemperature = temperatureObject.get("night").toString().toDouble()
                    val weatherArray = currentDayObject.getJSONArray("weather")
                    val weather: String = weatherArray.getJSONObject(0).get("main") as String
                    val weatherDescription: String = weatherArray.getJSONObject(0).get("description") as String

                    val date = currentDate.plusDays(i.toLong()).format(formatter).replaceFirstChar { it.titlecase(Locale.ROOT) }

                    val weatherIcon = when(weather) {
                        "Clouds" -> R.drawable.cloudy_weather_icon
                        "Snow" -> R.drawable.snow_weather_icon
                        "Rain" -> R.drawable.rain_weather_icon
                        "Clear" -> R.drawable.sun_weather_icon
                        else -> R.drawable.sunny_weather_icon
                    }

                    forecastList.add(Weather(weatherIcon, date, weatherDescription, dayTemperature, nightTemperature))
                }
                val adapter = WeatherRecyclerAdapter(forecastList)
                val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
                recyclerView.adapter = adapter
            },
            { Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show() })
        queue.add(stringRequest)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            LOCATION_PERMISSION_REQ_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                } else {
                    // permission denied
                    Toast.makeText(this, "Вам нужно дать разрешение на доступ к текущему местоположению",
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unsubscribeToLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        unsubscribeToLocationUpdates()
    }
}