package com.example.weatherforecast

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.roundToInt

class WeatherRecyclerAdapter(private val forecastList: List<Weather>):
    RecyclerView.Adapter<WeatherRecyclerAdapter.WeatherViewHolder>(){

    class WeatherViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val weatherIcon: ImageView = itemView.findViewById(R.id.weather_image)
        val date: TextView = itemView.findViewById(R.id.day)
        val weather: TextView = itemView.findViewById(R.id.weather)
        val dayTemperature: TextView = itemView.findViewById(R.id.day_temperature)
        val nightTemperature: TextView = itemView.findViewById(R.id.night_temperature)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int, ): WeatherViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.day_forecast, parent, false)
        return WeatherViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeatherViewHolder, position: Int) {
        val forecast = forecastList[position]
        holder.weatherIcon.setImageResource(forecast.weatherIcon)
        holder.date.text = forecast.date
        holder.weather.text = forecast.weather
        holder.dayTemperature.text = forecast.dayTemperature.roundToInt().toString() + '°'
        holder.nightTemperature.text = forecast.nightTemperature.roundToInt().toString() + '°'
    }

    override fun getItemCount(): Int {
        return forecastList.size
    }

}