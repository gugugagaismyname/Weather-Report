package com.example.weatherreport.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.weatherreport.MainViewModel
import com.example.weatherreport.R
import com.example.weatherreport.adapters.WeatherAdapter
import com.example.weatherreport.adapters.WeatherModel
import com.example.weatherreport.databinding.FragmentHoursBinding
import org.json.JSONArray
import org.json.JSONObject


class HoursFragment : Fragment() {
    private lateinit var binding: FragmentHoursBinding
    private lateinit var adapter: WeatherAdapter
    private val model: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHoursBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRcView()

        model.isFahrenheit.observe(viewLifecycleOwner) { isFahrenheit ->
            model.liveDataCurrent.value?.let { currentData ->
                adapter.submitList(getHoursList(currentData, isFahrenheit))
            }
        }
        model.liveDataCurrent.observe(viewLifecycleOwner) { currentData ->
            model.isFahrenheit.value?.let { isFahrenheit ->
                adapter.submitList(getHoursList(currentData, isFahrenheit))
            }
        }
    }

    private fun initRcView() = with(binding){
        rcView.layoutManager = LinearLayoutManager(activity)
        adapter = WeatherAdapter(null)
        rcView.adapter = adapter

    }

    private fun getHoursList(wItem: WeatherModel, isFahrenheit: Boolean): List<WeatherModel>{
        val hoursArray = JSONArray(wItem.hours)
        val list = ArrayList<WeatherModel>()
        val unit = if (isFahrenheit) "°F" else "°C"
        for (i in 0 until hoursArray.length()){
            val item = WeatherModel(
                wItem.city,
                (hoursArray[i] as JSONObject).getString("time"),
                (hoursArray[i] as JSONObject).getJSONObject("condition").getString("text"),
                if (isFahrenheit)
                    (hoursArray[i] as JSONObject).getString("temp_f")
                else
            (hoursArray[i] as JSONObject).getString("temp_c"),
                "",
                "",
                (hoursArray[i] as JSONObject).getJSONObject("condition").getString("icon"),
                ""
            )
            list.add(item)
        }
        return list
    }

    companion object {

        @JvmStatic
        fun newInstance() = HoursFragment()
    }
}