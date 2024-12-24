package com.example.weatherreport.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weatherreport.MainViewModel
import com.example.weatherreport.Manager
import com.example.weatherreport.R
import com.example.weatherreport.adapters.VpAdapter
import com.example.weatherreport.adapters.WeatherModel
import com.example.weatherreport.databinding.FragmentMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.tabs.TabLayoutMediator
import com.squareup.picasso.Picasso
import org.json.JSONObject

const val API_KEY = "032f28765ce14e5a852104736241612"

class MainFragment : Fragment() {
    private lateinit var fLocationClient: FusedLocationProviderClient
    private val flist = listOf(
        HoursFragment.newInstance(),
        DaysFragment.newInstance()
    )
    private val tList = listOf(
        "Hours",
        "Days"
    )
    private lateinit var pLauncher: ActivityResultLauncher<String>
    private lateinit var binding: FragmentMainBinding
    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        init()
        updateCurrentCard()
        binding.switchF.setOnCheckedChangeListener { _, isChecked ->
            model.isFahrenheit.value = isChecked
            model.liveDataCurrent.value?.let { currentData ->
                parseCurrentData(JSONObject(model.rawJson), currentData)
            }
            model.liveDataList.value?.let {
                parseDays(JSONObject(model.rawJson))
            }
        }
    }

    private fun init() = with(binding){
        fLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val adapter = VpAdapter(activity as FragmentActivity, flist)
        vp.adapter = adapter
        TabLayoutMediator(tabLayout, vp){
            tab, pos -> tab.text = tList[pos]
        }.attach()
        ibSync.setOnClickListener{
            tabLayout.selectTab(tabLayout.getTabAt(0))
            checkLocation()
        }
        ibSearch.setOnClickListener{
            Manager.searchByNameDialog(requireContext(), object : Manager.Listener{
                override fun onClick(name: String?) {
                    if (name != null) {
                        requestWeatherData(name)
                    }
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        checkLocation()
    }

    private fun checkLocation(){
        if(isLocationEnabled()){
            getLocation()
        } else {
            Manager.locationSettingsDialog(requireContext(), object : Manager.Listener{
                override fun onClick(name: String?) {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }

            })
        }


    }

    private fun isLocationEnabled(): Boolean{
        val lm = activity?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(){
        val ct = CancellationTokenSource()
        fLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, ct.token).addOnCompleteListener{
            requestWeatherData("${it.result.latitude},${it.result.longitude}")
        }
    }

    private fun updateCurrentCard() = with(binding){
        model.liveDataCurrent.observe(viewLifecycleOwner){
            val isFahrenheit = model.isFahrenheit.value ?: false
            val unit = if (isFahrenheit) "F" else "C"
            val maxMinTemp = "${(it.maxTemp.toDouble() + 0.5).toInt()}°$unit/${(it.minTemp.toDouble() + 0.5).toInt()}°$unit"
            tvData.text = it.time
            tvCity.text = it.city
            tvCurrentTemp.text = if (it.currentTemp.isNotEmpty()) {
                "${(it.currentTemp.toDouble() + 0.5).toInt()}°$unit"
            } else {
                maxMinTemp
            }
            tvCondition.text = it.condition
            tvMaxMin.text = if(it.currentTemp.isEmpty()) "" else maxMinTemp
            Picasso.get().load("https:" + it.imageURL).into(imageWeather)
        }
    }

    private fun permissionListener() {
        pLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){
            Toast.makeText(activity, "Permission is $it", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermission(){
        if(!isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)){
            permissionListener()
            pLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun requestWeatherData(city: String){
        val url = "https://api.weatherapi.com/v1/forecast.json?key=" +
                API_KEY +
                "&q=" +
                city +
                "&days=" +
                "5" +
                "&aqi=no&alerts=no"

        val queue = Volley.newRequestQueue(context)
        val request = StringRequest(
            Request.Method.GET,
            url,
            {
                result ->
                model.rawJson = result
                val mainObject = JSONObject(result)
                val list = parseDays(mainObject)
                parseCurrentData(mainObject, list[0])
            },
            {
                error -> Log.d("MyLog", "Error: $error")
            }
        )
        queue.add(request)
    }

    private fun parseWeatherData(result: String){
        val mainObject = JSONObject(result)
        val list = parseDays(mainObject)
        parseCurrentData(mainObject, list[0])

    }

    private fun parseDays(mainObject: JSONObject): List<WeatherModel> {
        val list = ArrayList<WeatherModel>()
        val daysArray = mainObject.getJSONObject("forecast").getJSONArray("forecastday")
        val name = mainObject.getJSONObject("location").getString("name")
        val isFahrenheit = model.isFahrenheit.value ?: false

        for (i in 0 until daysArray.length()) {
            val day = daysArray[i] as JSONObject
            val maxTemp = if (isFahrenheit) day.getJSONObject("day").getString("maxtemp_f") else day.getJSONObject("day").getString("maxtemp_c")
            val minTemp = if (isFahrenheit) day.getJSONObject("day").getString("mintemp_f") else day.getJSONObject("day").getString("mintemp_c")

            val item = WeatherModel(
                name,
                day.getString("date"),
                day.getJSONObject("day").getJSONObject("condition").getString("text"),
                "",
                maxTemp,
                minTemp,
                day.getJSONObject("day").getJSONObject("condition").getString("icon"),
                day.getJSONArray("hour").toString()
            )
            list.add(item)
        }
        model.liveDataList.value = list
        return list
    }


    private fun parseCurrentData(mainObject: JSONObject, weatherItem: WeatherModel) {
        val isFahrenheit = model.isFahrenheit.value ?: false
        val currentTemp = if (isFahrenheit) {
            mainObject.getJSONObject("current").getString("temp_f")
        } else {
            mainObject.getJSONObject("current").getString("temp_c")
        }

        val item = WeatherModel(
            mainObject.getJSONObject("location").getString("name"),
            mainObject.getJSONObject("current").getString("last_updated"),
            mainObject.getJSONObject("current").getJSONObject("condition").getString("text"),
            currentTemp,
            if (isFahrenheit) weatherItem.maxTemp else weatherItem.maxTemp,
            if (isFahrenheit) weatherItem.minTemp else weatherItem.minTemp,
            mainObject.getJSONObject("current").getJSONObject("condition").getString("icon"),
            weatherItem.hours
        )
        model.liveDataCurrent.value = item
    }


    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}