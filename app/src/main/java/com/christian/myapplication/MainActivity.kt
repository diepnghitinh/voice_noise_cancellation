package com.christian.myapplication

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color.argb
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Switch
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    var samplerateString: String? = null
    var buffersizeString:kotlin.String? = null
    var amplificationLabel: TextView? = null
    var noiseLabel: TextView? = null
    var speechLabel:TextView? = null
    var quietLabel:TextView? = null

    var amplifyBar: SeekBar? = null
    var startStopButton: ToggleButton? = null

    var noiseReductionSwitch: Switch? = null
    var compressionSwitch:android.widget.Switch? = null
    var saveIOSwitch:android.widget.Switch? = null

    private var activityInference: ActivityInference? = null
    private var averageBuffer: MovingAverageBuffer? = null
    var timeBuffer:MovingAverageBuffer? = null
    private val CNNUpdate = 62

    var audiofileIn: String? = null
    var folderName =
        Environment.getExternalStorageDirectory().toString() + "/IntegratedApp_Android/"
    var audiofileOut: String? = null

    var quiet = argb(64, 0, 26, 153)
    var noise = argb(64, 153, 0, 77)
    var speech = argb(64, 0, 153, 77)
    var white = argb(255, 255, 255, 255)

    var prefs: SharedPreferences? = null
    var prefEdit: SharedPreferences.Editor? = null
    val appPreferences = "appPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //getSupportActionBar().setTitle(Html.fromHtml("<small>Integrated App: VAD and Noise Classifier</small>"));
        //getSupportActionBar().setTitle(Html.fromHtml("<small>Integrated App: VAD and Noise Classifier</small>"));
        supportActionBar!!.title = " Signal and Image Processing Lab"
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.setDisplayUseLogoEnabled(true)
        supportActionBar!!.setLogo(R.mipmap.ic_utd_logo)

        if (Build.VERSION.SDK_INT >= 17) {
            val audioManager = this.getSystemService(AUDIO_SERVICE) as AudioManager
            Log.d(
                "Sampling Rate",
                audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            )
            Log.d(
                "Frame Size",
                audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
            )
            samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
            buffersizeString =
                audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
        }

        initializeIds()
        //load the Settings structure which pass user data from between jni and java
        //load the Settings structure which pass user data from between jni and java
        System.loadLibrary("FrequencyDomain")
        loadSettings()

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefEdit = prefs!!.edit()

        disableButtons()
        setLabelColor(-1)

        val folder = File(folderName)
        if (!folder.exists()) {
            folder.mkdirs()
        }

        amplifyBar!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val ampValue: Float = getConvertedValue(progress, true)
                updateSettingsAmplification(ampValue)
                amplificationLabel!!.text = String.format("  Amplification: %.2fx", ampValue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    fun getConvertedValue(intVal: Int, sliderToAmp: Boolean): Float {
        //float floatVal = 0.0f;
        val mappedValue = 1.0f * intVal / (amplifyBar!!.max / 2.0f)
        return if (sliderToAmp) {
            mappedValue * mappedValue
        } else {
            StrictMath.sqrt(mappedValue.toDouble()).toFloat()
        }
    }

    fun disableButtons() {
        noiseReductionSwitch!!.isEnabled = false
        compressionSwitch!!.isEnabled = false
        amplifyBar!!.isEnabled = false
        saveIOSwitch!!.isEnabled = true
    }

    fun enableButtons() {
        noiseReductionSwitch!!.isEnabled = true
        compressionSwitch!!.isEnabled = true
        amplifyBar!!.isEnabled = true
        saveIOSwitch!!.isEnabled = false
    }

    fun setLabelColor(detectedClass: Int) {
        when (detectedClass) {
            0 -> {
                quietLabel!!.setBackgroundColor(quiet)
                noiseLabel!!.setBackgroundColor(white)
                speechLabel!!.setBackgroundColor(white)
            }
            1 -> {
                quietLabel!!.setBackgroundColor(white)
                noiseLabel!!.setBackgroundColor(noise)
                speechLabel!!.setBackgroundColor(white)
            }
            2 -> {
                quietLabel!!.setBackgroundColor(white)
                noiseLabel!!.setBackgroundColor(white)
                speechLabel!!.setBackgroundColor(speech)
            }
            else -> {
                quietLabel!!.setBackgroundColor(white)
                noiseLabel!!.setBackgroundColor(white)
                speechLabel!!.setBackgroundColor(white)
            }
        }
    }

    fun initializeIds() {
        noiseLabel = findViewById(R.id.noiseLabel) as TextView
        speechLabel = findViewById(R.id.speechLabel) as TextView
        quietLabel = findViewById(R.id.quietLabel) as TextView
        startStopButton = findViewById(R.id.startStopButton) as ToggleButton
        amplifyBar = findViewById(R.id.amplificationBar) as SeekBar
        amplificationLabel = findViewById(R.id.amplification) as TextView
        noiseReductionSwitch = findViewById(R.id.noiseReductionSwitch) as Switch
        compressionSwitch = findViewById(R.id.compressionSwitch) as Switch
        saveIOSwitch = findViewById(R.id.saveIOSwitch) as Switch
        activityInference = ActivityInference(applicationContext)
        averageBuffer = MovingAverageBuffer(13)
        timeBuffer = MovingAverageBuffer(samplerateString!!.toInt() / buffersizeString!!.toInt())
    }


    fun onSettingsClick(view: View) {
        var intent: Intent? = null
        when (view.id) {
            R.id.noiseReductionSettings -> intent = Intent(this, NoiseReductionSettings::class.java)
            R.id.compressionSettings -> intent = Intent(this, CompressionSettings::class.java)
        }

        intent?.let { startActivity(it) }
    }

    fun onStartStopClick(view: View?) {
        if (startStopButton!!.isChecked) {
            enableButtons()
            if (saveIOSwitch!!.isChecked) {
                val dateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Date())
                audiofileIn = folderName + "Input_" + dateFormat + ".wav"
                audiofileOut = folderName + "Output_" + dateFormat + ".wav"
                Log.d("Filename In", audiofileIn!!)
                Log.d("Filename Out", audiofileOut!!)
            }
            setAudioPlay(1)
            if (Build.VERSION.SDK_INT >= 17) {
                //AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
                samplerateString =
                    Integer.toString(getFs()) //audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
                buffersizeString =
                    Integer.toString(getStepSize()) //audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
            }
            if (samplerateString == null) samplerateString = "44100"
            if (buffersizeString == null) buffersizeString = "512"
            System.loadLibrary("FrequencyDomain")
            FrequencyDomain(
                samplerateString!!.toInt(), buffersizeString!!.toInt(),
                saveIOSwitch!!.isChecked,
                audiofileIn!!, audiofileOut!!
            )
            handler.postDelayed(r, getGuiUpdateRate().toLong())
            handler.postDelayed(classify, CNNUpdate.toLong())
        } else {
            disableButtons()
            setAudioPlay(0)
            setLabelColor(-1)
            System.loadLibrary("FrequencyDomain")
            StopAudio(audiofileIn!!, audiofileOut!!)
            handler.removeCallbacks(r)
            handler.removeCallbacks(classify)
        }
    }

    // Close app using other buttons (home or overview)
    // uncomment super.onBackPressed() if you want to enable.
    // If someone intends to close app using backbutton,
    // it is recommended to close from task manager as well
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
        //        if(id == R.id.home) {
//            finish();
//            return true;
//        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onBackPressed() {
        //super.onBackPressed();
        // dont call **super**, if u want disable back button in current screen.
    }

    var handler = Handler()

    val r: Runnable = object : Runnable {
        override fun run() {
            //setLabelColor(getDetectedClass()+1);
            setLabelColor(if (getdbPower() < 60.0f) 0 else getDetectedClass() + 1)
            handler.postDelayed(this, getGuiUpdateRate().toLong())
        }
    }

    val classify: Runnable = object : Runnable {
        override fun run() {
            val startTime = System.currentTimeMillis()
            val prob = activityInference!!.getActivityProb(GetMelImage())
            averageBuffer!!.addDatum((prob[1] + 0.15).toFloat())
            timeBuffer!!.addDatum((System.currentTimeMillis() - startTime).toFloat())
            setDetectedClass(averageBuffer!!.movingAverage)
            handler.postDelayed(this, CNNUpdate.toLong())
        }
    }

    fun noiseReductionSelect(view: View?) {
        noiseReductionOn(noiseReductionSwitch!!.isChecked)
        updateSettingsAmplification(getConvertedValue(amplifyBar!!.progress, false))
    }

    fun compressionSelect(view: View?) {
        compressionOn(compressionSwitch!!.isChecked)
        updateSettingsAmplification(getConvertedValue(amplifyBar!!.progress, false))
    }

    fun getExtensionOfFile(name: String): String? {
        var fileExtension = ""

        // If fileName do not contain "." or starts with "." then it is not a valid file
        if (name.contains(".") && name.lastIndexOf(".") != 0) {
            fileExtension = name.substring(name.lastIndexOf(".") + 1)
        }
        return fileExtension
    }

    private external fun FrequencyDomain(
        samplerate: Int,
        buffersize: Int,
        storeAudioFlag: Boolean,
        fileIn: String, fileOut: String
    )

    private external fun StopAudio(fileIn: String, fileOut: String)
    private external fun getFs(): Int
    private external fun setAudioPlay(on: Int)
    private external fun getDetectedClass(): Int
    private external fun getGuiUpdateRate(): Float
    private external fun noiseReductionOn(on: Boolean)
    private external fun compressionOn(on: Boolean)
    private external fun getStepSize(): Int
    private external fun loadSettings()
    private external fun destroySettings()
    private external fun updateSettingsAmplification(ampValue: Float)
    private external fun GetMelImage(): FloatArray?
    private external fun setDetectedClass(detectedClass: Float)
    private external fun getdbPower(): Float
}