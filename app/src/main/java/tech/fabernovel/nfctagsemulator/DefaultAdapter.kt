package tech.fabernovel.nfctagsemulator

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_wifi.view.*

class DefaultAdapter(dataSet: List<String>) : RecyclerView.Adapter<DefaultAdapter.ViewHolder>() {

    var dataSet = dataSet.toMutableList()
    set(values) {
        dataSet.clear()
        dataSet.addAll(values)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.textView.text = dataSet[position]
    }

    override fun getItemCount(): Int = dataSet.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_wifi, parent, false)

        return ViewHolder(v)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

}
