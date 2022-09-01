package ysports.app.ui.leagues

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ysports.app.R
import ysports.app.api.leagues.Leagues

class LeaguesAdapter(
    private val context: Context,
    private val arrayList: ArrayList<Leagues>
) : RecyclerView.Adapter<LeaguesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_leagues, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val title = arrayList[position].title
        val logo = arrayList[position].logo
        holder.title.text =  title
        Glide.with(context)
            .load(logo)
            .placeholder(R.drawable.bg_image_placeholder_circle)
            .into(holder.logo)
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val logo: ImageView = itemView.findViewById(R.id.logo)
        val title: TextView = itemView.findViewById(R.id.title)
    }
}