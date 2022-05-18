package ysports.app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ysports.app.R
import ysports.app.api.fixture.Fixtures

class MatchesAdapter(
    private val context: Context,
    private val arrayList: ArrayList<Fixtures>
) : RecyclerView.Adapter<MatchesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_home_matches, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.homeTeam.text = arrayList[position].homeTeam ?: ""
        holder.awayTeam.text = arrayList[position].awayTeam ?: ""
        Glide.with(context)
            .load(arrayList[position].homeTeamLogo ?: "")
            .placeholder(R.drawable.img_logo_team_placeholder)
            .into(holder.homeTeamLogo)
        Glide.with(context)
            .load(arrayList[position].awayTeamLogo ?: "")
            .placeholder(R.drawable.img_logo_team_placeholder)
            .into(holder.awayTeamLogo)
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val homeTeamLogo: ImageView = itemView.findViewById(R.id.home_team_logo)
        val awayTeamLogo: ImageView = itemView.findViewById(R.id.away_team_logo)
        val homeTeam: TextView = itemView.findViewById(R.id.home_team_name)
        val awayTeam: TextView = itemView.findViewById(R.id.away_team_name)
    }
}