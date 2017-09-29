package tech.fabernovel.nfctagsemulator

import android.graphics.PorterDuff
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_wifi.view.*

class DefaultAdapter(dataSet: List<WifiModel>) : RecyclerView.Adapter<DefaultAdapter.ViewHolder>() {

    private var dataSet = dataSet.toMutableList()

    fun setData(dataSet: List<WifiModel>) {
        this.dataSet.clear()
        this.dataSet.addAll(dataSet)
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = dataSet[position]
        holder.itemView.title.text = model.network.ssid.replace("\"", "")
        holder.itemView.subtitle.text =
            if (model.missingPassword) "Mot de passe manquant"
            else when(model.status) {
                Status.CONNECTED -> "Connecté"
                Status.REACHABLE -> "Accessible"
                Status.UNREACHABLE -> "Non-accessible"
            }
        holder.itemView.icon.setImageResource(
            if (model.missingPassword) R.drawable.ic_error_white_24dp
            else when(model.status) {
                Status.CONNECTED -> R.drawable.ic_tap_and_play_white_24dp
                Status.REACHABLE -> R.drawable.ic_network_wifi_white_24dp
                Status.UNREACHABLE -> R.drawable.ic_signal_wifi_off_white_24dp
            })
        holder.itemView.icon.setColorFilter(ContextCompat.getColor(holder.itemView.context,
            if (model.missingPassword) R.color.yellow
            else when(model.status) {
                Status.CONNECTED -> R.color.green
                Status.REACHABLE -> R.color.blue
                Status.UNREACHABLE -> R.color.red
            }), PorterDuff.Mode.MULTIPLY)

    }

    override fun getItemCount(): Int = dataSet.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_wifi, parent, false)

        return ViewHolder(v)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

}
