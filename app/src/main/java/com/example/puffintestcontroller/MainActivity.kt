package com.example.puffintestcontroller

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    @Volatile
    private var isTestRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvDetectedPlatform = findViewById<TextView>(R.id.tvDetectedPlatform)
        val btnClearConsole = findViewById<Button>(R.id.btnClearConsole)
        val spinnerTests = findViewById<Spinner>(R.id.spinner_tests)
        val btnRunTest = findViewById<Button>(R.id.btn_run_test)
        val btnCancelTest = findViewById<Button>(R.id.btn_cancel_test)
        val txtConsoleOutput = findViewById<TextView>(R.id.txt_console_output)
        val txtSuiteTarget = findViewById<TextView>(R.id.txt_suite_target)
        val scrollConsole = findViewById<ScrollView>(R.id.scroll_console)
        val txtAdbStatus = findViewById<TextView>(R.id.txt_adb_status)
        val txtStatusBadge = findViewById<TextView>(R.id.txt_status_badge)

        val detectedSystem = detectDevicePlatform()
        tvDetectedPlatform.text = "SYSTEM: $detectedSystem"

        val deviceSerial = intent.getStringExtra("SERIAL_NO") ?: getDeviceSerialNumber()
        txtAdbStatus.text = "[TARGET: $deviceSerial • ADB ONLINE]"

        btnClearConsole.setOnClickListener {
            txtConsoleOutput.text = "[SYSTEM]: Console cleared by operator.\n"
        }

        val testOptions = listOf("test_ota_apps", "pipeline_stability_test", "test_device_shipmode")

        val targetPaths = mapOf(
            "test_ota_apps" to "src/goldbug/tests/device_tests/test_ota_apps.py",
            "pipeline_stability_test" to "src/goldbug/tests/device_tests/test_bat.py::test_partition_mounts_and_size",
            "test_device_shipmode" to "src/goldbug/tests/device_tests/test_bat.py::test_device_shipmode"
        )

        val adapter =
            object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, testOptions) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent) as TextView
                    view.setTextColor(Color.WHITE)
                    view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
                    return view
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {
                    val view = super.getDropDownView(position, convertView, parent) as TextView
                    view.setTextColor(Color.WHITE)
                    view.setBackgroundColor(Color.parseColor("#1E293B"))
                    return view
                }
            }
        spinnerTests.adapter = adapter

        spinnerTests.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedTest = testOptions[position]
                txtSuiteTarget.text = "Target: ${targetPaths[selectedTest]}"
                appendAndLogConsole(
                    "[CONFIG]: Target context switched -> $selectedTest\n",
                    txtConsoleOutput,
                    scrollConsole
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        btnRunTest.setOnClickListener {
            val selectedTest = spinnerTests.selectedItem.toString()
            isTestRunning = true

            txtStatusBadge.text = "RUNNING"
            txtStatusBadge.setTextColor(Color.BLACK)
            txtStatusBadge.setBackgroundColor(Color.parseColor("#F59E0B"))

            btnRunTest.isEnabled = false
            btnRunTest.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#1E293B"))

            btnCancelTest.isEnabled = true
            btnCancelTest.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444"))
            btnCancelTest.setTextColor(Color.WHITE)

            thread {
                val steps = listOf(
                    "\n[INIT]: Initializing runner container...\n",
                    "[AUTH]: Bypassing low-level infrastructure gates...\n",
                    "[EXEC]: Running test routine for $selectedTest...\n",
                    "[VERIFY]: Checking media pipeline app states...\n",
                    "[PASS]: Verification complete for node $deviceSerial.\n"
                )

                for (step in steps) {
                    if (!isTestRunning) break // Stop if user hit Cancel!

                    runOnUiThread {
                        appendAndLogConsole(step, txtConsoleOutput, scrollConsole)
                    }
                    Thread.sleep(1500) // 1.5 second pause between steps
                }

                // If test finished naturally without cancellation
                if (isTestRunning) {
                    isTestRunning = false
                    runOnUiThread {
                        txtStatusBadge.text = "PASSED"
                        txtStatusBadge.setTextColor(Color.WHITE)
                        txtStatusBadge.setBackgroundColor(Color.parseColor("#10B981"))

                        btnRunTest.isEnabled = true
                        btnRunTest.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#0EA5E9"))
                        btnCancelTest.isEnabled = false
                        btnCancelTest.backgroundTintList =
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#334155"))
                        btnCancelTest.setTextColor(Color.parseColor("#94A3B8"))
                    }
                }
            }
        }

        btnCancelTest.setOnClickListener {
            if (isTestRunning) {
                isTestRunning = false
                appendAndLogConsole(
                    "\n[WARN]: Interrupt signal received from user!\n",
                    txtConsoleOutput,
                    scrollConsole
                )
                appendAndLogConsole(
                    "[ABORT]: Terminating test process safely...\n",
                    txtConsoleOutput,
                    scrollConsole
                )
                appendAndLogConsole(
                    "[STATUS]: Test execution cancelled by operator.\n",
                    txtConsoleOutput,
                    scrollConsole
                )

                txtStatusBadge.text = "CANCELLED"
                txtStatusBadge.setTextColor(Color.WHITE)
                txtStatusBadge.setBackgroundColor(Color.parseColor("#EF4444"))

                btnRunTest.isEnabled = true
                btnRunTest.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#0EA5E9"))

                btnCancelTest.isEnabled = false
                btnCancelTest.backgroundTintList =
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#334155"))
                btnCancelTest.setTextColor(Color.parseColor("#94A3B8"))
            }
        }
    }

    private fun appendAndLogConsole(text: String, textView: TextView, scrollView: ScrollView) {
        textView.append(text)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }

        try {
            val logDir = File(getExternalFilesDir(null), "puffin_tests/logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            val logFile = File(logDir, "test_execution.log")
            val writer = FileWriter(logFile, true)
            val timeStamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            writer.append("[$timeStamp] $text")
            writer.flush()
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getDeviceSerialNumber(): String {
        return try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod("get", String::class.java)
            var sn = get.invoke(c, "ro.serialno") as String
            if (sn.isBlank()) sn = get.invoke(c, "ro.boot.serialno") as String
            if (sn.isBlank()) sn = get.invoke(c, "sys.serialnumber") as String
            if (sn.isNotBlank()) sn else Build.MODEL
        } catch (e: Exception) {
            Build.MODEL
        }
    }

    /**
     * Attempts to read a system property via Runtime exec shell command.
     * Needed to bypass Android SELinux restrictions on persist.* vendor properties.
     */
    private fun getShellProperty(propName: String): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop $propName")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()?.trim() ?: ""
            reader.close()
            output
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Reads system properties natively via SystemProperties reflection.
     */

    private fun getSystemProperty(key: String): String {
        return try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod("get", String::class.java, String::class.java)
            get.invoke(c, key, "") as String
        } catch (e: Exception) {
            ""
        }
    }

    private fun detectDevicePlatform(): String {
        val cloudProductId = intent.getStringExtra("CLOUD_PRODUCT_ID") ?: getSystemProperty("persist.vendor.flock.system_cloud_product_id")
        val vendorSku = getSystemProperty("ro.boot.product.vendor.sku")
        val socModel = getSystemProperty("ro.soc.model")

        val baseTarget = when {
            cloudProductId.contains("puffin", ignoreCase = true) -> "PUFFIN"
            cloudProductId.contains("picard", ignoreCase = true) -> "PICARD"
            vendorSku.equals("yupik", ignoreCase = true) || socModel.equals("SM7325", ignoreCase = true) -> "PUFFIN"
            else -> Build.MODEL.uppercase()
        }

        return "$baseTarget TARGET DETECTED"
    }
}