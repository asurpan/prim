package com.sagon.prim

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox // Importar CheckBox
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var etNumAdditionalCombinations: EditText
    private lateinit var btnSaveSettings: Button
    private lateinit var switchNotifications: Switch
    private lateinit var cbMonday: CheckBox // ¡NUEVA VARIABLE!
    private lateinit var cbThursday: CheckBox // ¡NUEVA VARIABLE!
    private lateinit var cbSaturday: CheckBox // ¡NUEVA VARIABLE!
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Ajustes"

        etNumAdditionalCombinations = findViewById(R.id.etNumAdditionalCombinations)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        switchNotifications = findViewById(R.id.switchNotifications)
        cbMonday = findViewById(R.id.cbMonday) // ¡INICIALIZAR!
        cbThursday = findViewById(R.id.cbThursday) // ¡INICIALIZAR!
        cbSaturday = findViewById(R.id.cbSaturday) // ¡INICIALIZAR!

        sharedPreferences = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        loadSettings()

        btnSaveSettings.setOnClickListener {
            saveSettings()
        }

        // Deshabilitar/Habilitar CheckBoxes según el estado del Switch de notificaciones
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            cbMonday.isEnabled = isChecked
            cbThursday.isEnabled = isChecked
            cbSaturday.isEnabled = isChecked
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

    private fun loadSettings() {
        val numAdditionalCombinations = sharedPreferences.getInt("num_additional_combinations", 5)
        etNumAdditionalCombinations.setText(numAdditionalCombinations.toString())

        val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        switchNotifications.isChecked = notificationsEnabled

        // Cargar estado de los CheckBoxes (por defecto, true para todos si no se han guardado)
        cbMonday.isChecked = sharedPreferences.getBoolean("notify_monday", true)
        cbThursday.isChecked = sharedPreferences.getBoolean("notify_thursday", true)
        cbSaturday.isChecked = sharedPreferences.getBoolean("notify_saturday", true)

        // Ajustar estado de habilitación de los CheckBoxes al cargar
        cbMonday.isEnabled = notificationsEnabled
        cbThursday.isEnabled = notificationsEnabled
        cbSaturday.isEnabled = notificationsEnabled
    }

    private fun saveSettings() {
        val editor = sharedPreferences.edit()

        val numStr = etNumAdditionalCombinations.text.toString()
        if (numStr.isNotEmpty()) {
            val num = numStr.toIntOrNull()
            if (num != null && num >= 1 && num <= 10) {
                editor.putInt("num_additional_combinations", num)
            } else {
                Toast.makeText(this, "El número de combinaciones debe estar entre 1 y 10.", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            Toast.makeText(this, "Por favor, introduce un número de combinaciones.", Toast.LENGTH_SHORT).show()
            return
        }

        editor.putBoolean("notifications_enabled", switchNotifications.isChecked)

        // ¡Guardar estado de los CheckBoxes!
        editor.putBoolean("notify_monday", cbMonday.isChecked)
        editor.putBoolean("notify_thursday", cbThursday.isChecked)
        editor.putBoolean("notify_saturday", cbSaturday.isChecked)

        editor.apply()
        Toast.makeText(this, "Ajustes guardados correctamente.", Toast.LENGTH_SHORT).show()
    }
}