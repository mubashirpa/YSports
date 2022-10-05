package ysports.app.ui.matches.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ysports.app.R
import ysports.app.api.matches.Matches
import java.text.SimpleDateFormat
import java.util.*

class MatchesAdapter(
    private val context: Context,
    private val list: List<Matches>
) : RecyclerView.Adapter<MatchesAdapter.ViewHolder>() {

    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val calendar: Calendar = Calendar.getInstance()
    private val timeFormatter = SimpleDateFormat("KK:mm aaa", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("dd LLL yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_matches, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timestamp = list[position].timestamp
        var matchTime = context.resources.getString(R.string._00_00)
        var matchDate = Calendar.getInstance().get(Calendar.YEAR).toString()
        if (timestamp.isNotEmpty()) {
            calendar.time = simpleDateFormat.parse(timestamp) as Date
            matchTime = timeFormatter.format(calendar.time)
            matchDate = dateFormatter.format(calendar.time)
        }
        holder.matchTime.text = matchTime.uppercase()
        holder.matchDate.text = matchDate

        holder.homeTeam.text = list[position].homeTeam
        holder.awayTeam.text = list[position].awayTeam
        val scoreHomeTeam = list[position].homeTeamScore
        val scoreAwayTeam = list[position].awayTeamScore
        if (scoreHomeTeam != null) holder.homeTeamScore.text = "$scoreHomeTeam"
        if (scoreAwayTeam != null) holder.awayTeamScore.text = "$scoreAwayTeam"

        Glide.with(context)
            .load(list[position].homeTeamLogo)
            .placeholder(R.drawable.img_logo_team_placeholder)
            .into(holder.homeTeamLogo)
        Glide.with(context)
            .load(list[position].awayTeamLogo)
            .placeholder(R.drawable.img_logo_team_placeholder)
            .into(holder.awayTeamLogo)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val homeTeamLogo: ImageView = itemView.findViewById(R.id.home_team_logo)
        val matchTime: TextView = itemView.findViewById(R.id.match_time)
        val matchDate: TextView = itemView.findViewById(R.id.match_date)
        val homeTeamScore: TextView = itemView.findViewById(R.id.home_team_score)
        val awayTeamLogo: ImageView = itemView.findViewById(R.id.away_team_logo)
        val homeTeam: TextView = itemView.findViewById(R.id.home_team)
        val awayTeam: TextView = itemView.findViewById(R.id.away_team)
        val awayTeamScore: TextView = itemView.findViewById(R.id.away_team_score)
    }
}