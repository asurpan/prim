package com.sagon.prim

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class ManualInputActivity : AppCompatActivity() {

    private lateinit var etCombinationName: EditText
    private lateinit var etNum1: EditText
    private lateinit var etNum2: EditText
    private lateinit var etNum3: EditText
    private lateinit var etNum4: EditText
    private lateinit var etNum5: EditText
    private lateinit var etNum6: EditText
    private lateinit var btnSaveManualCombination: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_input)

        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Habilita el botón de retroceso
        supportActionBar?.title = "Añadir Combinación Manual"

        etCombinationName = findViewById(R.id.etCombinationName)
        etNum1 = findViewById(R.id.etNum1)
        etNum2 = findViewById(R.id.etNum2)
        etNum3 = findViewById(R.id.etNum3)
        etNum4 = findViewById(R.id.etNum4)
        etNum5 = findViewById(R.id.etNum5)
        etNum6 = findViewById(R.id.etNum6)
        btnSaveManualCombination = findViewById(R.id.btnSaveManualCombination)

        btnSaveManualCombination.setOnClickListener {
            saveManualCombination()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun saveManualCombination() {
        val name = etCombinationName.text.toString().trim()
        val num1 = etNum1.text.toString().toIntOrNull()
        val num2 = etNum2.text.toString().toIntOrNull()
        val num3 = etNum3.text.toString().toIntOrNull()
        val num4 = etNum4.text.toString().toIntOrNull()
        val num5 = etNum5.text.toString().toIntOrNull()
        val num6 = etNum6.text.toString().toIntOrNull()

        // Validaciones
        if (name.isEmpty()) {
            etCombinationName.error = "Introduce un nombre para la combinación"
            return
        }

        val numbers = listOfNotNull(num1, num2, num3, num4, num5, num6)

        if (numbers.size != 6) {
            Toast.makeText(this, "Por favor, introduce los 6 números.", Toast.LENGTH_SHORT).show()
            return
        }

        if (numbers.any { it < 1 || it > 49 }) {
            Toast.makeText(this, "Los números deben estar entre 1 y 49.", Toast.LENGTH_SHORT).show()
            return
        }

        if (numbers.distinct().size != 6) {
            Toast.makeText(this, "Los números no pueden repetirse.", Toast.LENGTH_SHORT).show()
            return
        }

        val sortedNumbers = numbers.sorted()
        val combinationString = sortedNumbers.joinToString(", ")

        // Cargar combinaciones existentes
        val sharedPreferences = getSharedPreferences("LotteryCombinations", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("saved_combinations", null)
        val existingCombinations = if (json != null) {
            val type = object : TypeToken<MutableList<LotteryCombination>>() {}.type
            Gson().fromJson<MutableList<LotteryCombination>>(json, type)
        } else {
            mutableListOf()
        }

        // Añadir la nueva combinación manual
        existingCombinations.add(LotteryCombination(name, combinationString))

        // Guardar la lista actualizada
        val editor = sharedPreferences.edit()
        val updatedJson = Gson().toJson(existingCombinations)
        editor.putString("saved_combinations", updatedJson)
        editor.apply()

        Toast.makeText(this, "Combinación '$name' guardada.", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK) // Indica a MainActivity que se guardó algo
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}