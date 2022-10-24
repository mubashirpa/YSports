package ysports.app.ui.leagues

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import ysports.app.R
import ysports.app.api.leagues.Leagues

class LeaguesAdapter(
    private val context: Context,
    private val list: List<Leagues>
) : RecyclerView.Adapter<LeaguesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_leagues, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val title = list[position].leagueTitle
        val logo = list[position].leagueLogo
        holder.title.text =  title
        Glide.with(context)
            .load(logo)
            .into(holder.logo)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val logo: ShapeableImageView = itemView.findViewById(R.id.logo)
        val title: TextView = itemView.findViewById(R.id.title)
    }
}