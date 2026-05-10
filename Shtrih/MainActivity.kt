package com.example.shtrih2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import com.journeyapps.barcodescanner.BarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory
import com.google.zxing.BarcodeFormat
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<Swag>()
    private val database by lazy { viewModel.database }
    private var tvResult: TextView? = null
    private lateinit var barcodeView: BarcodeView
    private var filePicker: ActivityResultLauncher<String?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Привязка элементов интерфейса
        val btnLoad = findViewById<Button>(R.id.btnLoad)
        val btnScan = findViewById<Button>(R.id.btnScan)
        tvResult = findViewById<TextView>(R.id.tvResult)
        barcodeView = findViewById<BarcodeView>(R.id.barcodeView)

        // Настройка декодера для распознавания нужных форматов
        barcodeView.decoderFactory = DefaultDecoderFactory(
            listOf(BarcodeFormat.CODE_128, BarcodeFormat.EAN_13)
        )

        // Callback непрерывного сканирования
        barcodeView.decodeContinuous { result ->
            val code = result.text
            val product = database[code]
            tvResult?.text = "$code → ${product ?: "Не найдено"}"
        }

        // Выбор Excel файла
        filePicker = registerForActivityResult(
            GetContent(),
            ActivityResultCallback { uri: Uri? ->
                if (uri != null) {
                    loadExcel(uri)
                }
            }
        )

        // Обработчики кнопок
        btnLoad.setOnClickListener {
            filePicker?.launch(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
        }

        btnScan.setOnClickListener {
            startScan()
        }
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()   // возобновляем работу камеры при возвращении в активность
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()    // освобождаем камеру, когда активность не видна
    }

    private fun loadExcel(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val workbook: Workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)

            database.clear()

            for (row in sheet) {
                val barcodeCell = row.getCell(2)
                val nameCell = row.getCell(0)
                try {
                    if (barcodeCell != null && nameCell != null) {
                        val barcode = barcodeCell.stringCellValue.trim()
                        val name = nameCell.numericCellValue.toString()
                        Log.e("System.err", "$barcode $name")
                        database[barcode] = name
                    }
                } catch (e: Exception) {
                    // игнорируем строки с некорректными данными
                }
            }

            tvResult?.text = "Excel загружен: ${database.size} записей"
        } catch (e: Exception) {
            e.printStackTrace()
            tvResult?.text = "Ошибка загрузки Excel"
        }
    }

    private fun startScan() {
        // Просто возобновляем сканер, если он был приостановлен (например, после паузы)
        barcodeView.resume()
    }
}

class Swag : ViewModel() {
    val database = HashMap<String?, String?>()
}