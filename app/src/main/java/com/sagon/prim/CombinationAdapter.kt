package com.sagon.prim

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sagon.prim.R

// Definición de la interfaz de acciones
interface OnCombinationActionListener {
    fun onCombinationClicked(combinationString: String, position: Int)
}

class CombinationAdapter(
    private var items: MutableList<String>,
    private val listener: OnCombinationActionListener
) : RecyclerView.Adapter<CombinationAdapter.CombinationViewHolder>() {

    // Propiedad pública de solo lectura para acceder a los items
    val allItems: List<String>
        get() = items

    fun updateData(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    // Función para eliminar un ítem
    fun removeItem(position: Int) {
        if (position >= 0 && position < items.size) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CombinationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_combination, parent, false)
        return CombinationViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: CombinationViewHolder, position: Int) {
        val fullString = items[position]
        holder.bind(fullString)
    }

    override fun getItemCount(): Int = items.size

    class CombinationViewHolder(
        itemView: View,
        private val listener: OnCombinationActionListener
    ) : RecyclerView.ViewHolder(itemView) {

        val combinationTextView: TextView = itemView.findViewById(R.id.combinationTextView)

        fun bind(combinationString: String) {
            combinationTextView.text = combinationString

            // Implementamos el clic que llama al listener de la Activity
            itemView.setOnClickListener {
                listener.onCombinationClicked(combinationString, adapterPosition)
            }
        }
    }
}
