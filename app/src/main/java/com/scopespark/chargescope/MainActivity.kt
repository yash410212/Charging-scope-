package com.scopespark.chargescope

import android.content.*
import android.os.*
import android.os.BatteryManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private lateinit var power: TextView
    private lateinit var voltage: TextView
    private lateinit var current: TextView
    private lateinit var battery: TextView
    private lateinit var temp: TextView
    private lateinit var diagnostic: TextView
    private lateinit var graph: GraphView
    private val handler = Handler(Looper.getMainLooper())
    private val samples = ArrayDeque<Double>()
    private val tick = object : Runnable {
        override fun run() { update(); handler.postDelayed(this, 1000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        power=findViewById(R.id.power); voltage=findViewById(R.id.voltage); current=findViewById(R.id.current)
        battery=findViewById(R.id.battery); temp=findViewById(R.id.temp); diagnostic=findViewById(R.id.diagnostic)
        graph=findViewById(R.id.graph)
        handler.post(tick)
    }
    private fun update() {
        val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val mv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val temp10 = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val microA = if (Build.VERSION.SDK_INT >= 21)
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) else 0
        val ua = microA.toLong()
        val amps = abs(ua) / 1_000_000.0
        val volts = mv / 1000.0
        val watts = if (volts > 0 && amps > 0) volts * amps else 0.0

        power.text = if (watts > 0) String.format(Locale.US, "%.2f W", watts) else "— W"
        voltage.text = String.format(Locale.US, "Voltage\n%.3f V", volts)
        current.text = if (amps > 0) String.format(Locale.US, "Current\n%.0f mA", amps*1000) else "Current\n—"
        battery.text = "Battery ${if (level >= 0) level*100/scale else "—"}%"
        temp.text = if (temp10 > 0) String.format(Locale.US, "Temperature %.1f °C", temp10/10.0) else "Temperature — °C"

        if (volts > 0) graph.add(volts.toFloat())
        if (watts > 0) {
            samples.addLast(watts); while(samples.size>60) samples.removeFirst()
        }
        val avg = if(samples.isNotEmpty()) samples.average() else 0.0
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING
        diagnostic.text = when {
            !charging -> "Diagnostic: Not charging"
            amps <= 0.0 -> "Diagnostic: Current data unavailable on this device"
            watts < 2.0 -> "Diagnostic: Low measured input • check cable/charger"
            else -> String.format(Locale.US, "Diagnostic: Charging • %.2f W avg", avg)
        }
    }
    override fun onDestroy() { handler.removeCallbacks(tick); super.onDestroy() }
}
