package com.sagon.prim

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sagon.prim.R

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CombinationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Historial"

        recyclerView = findViewById(R.id.historyRecyclerView)

        val combinations = mutableListOf(
            "Guardada 1: 3, 15, 22, 30, 41, 49",
            "Guardada 2: 1, 9, 18, 25, 33, 44",
            "Guardada 3: 5, 10, 20, 30, 40, 45"
        )

        // Listener 'dummy' para HistoryActivity, ya que no queremos que los clics hagan nada aquí.
        val dummyListener = object : OnCombinationActionListener {
            override fun onCombinationClicked(combinationString: String, position: Int) {
                // No hay acción en el historial
            }
        }

        adapter = CombinationAdapter(combinations.toMutableList(), dummyListener)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}