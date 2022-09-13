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
import ysports.app.api.fixture.Fixtures
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class FixtureAdapter(
    private val context: Context,
    private val arrayList: ArrayList<Fixtures>
) : RecyclerView.Adapter<FixtureAdapter.ViewHolder>() {

    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val calendar: Calendar = Calendar.getInstance()
    private val timeFormatter = SimpleDateFormat("KK:mm aaa", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("dd LLL yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_fixture, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timestamp = arrayList[position].timestamp
        var matchTime = context.resources.getString(R.string._00_00)
        var matchDate = Calendar.getInstance().get(Calendar.YEAR).toString()
        if (!timestamp.isNullOrEmpty()) {
            calendar.time = simpleDateFormat.parse(timestamp) as Date
            matchTime = timeFormatter.format(calendar.time)
            matchDate = dateFormatter.format(calendar.time)
        }
        holder.matchTime.text = matchTime.uppercase()
        holder.matchDate.text = matchDate

        holder.homeTeam.text = arrayList[position].homeTeam
        holder.awayTeam.text = arrayList[position].awayTeam
        Glide.with(context)
            .load(arrayList[position].homeTeamLogo)
            .placeholder(R.drawable.img_logo_team_placeholder)
            .into(holder.homeTeamLogo)
        Glide.with(context)
            .load(arrayList[position].awayTeamLogo)
            .placeholder(R.drawable.img_logo_team_placeholder)
            .into(holder.awayTeamLogo)
    }

    override fun getItemCount(): Int {
        return arrayList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val homeTeamLogo: ImageView = itemView.findViewById(R.id.home_team_logo)
        val matchTime: TextView = itemView.findViewById(R.id.match_time)
        val matchDate: TextView = itemView.findViewById(R.id.match_date)
        val awayTeamLogo: ImageView = itemView.findViewById(R.id.away_team_logo)
        val homeTeam: TextView = itemView.findViewById(R.id.home_team)
        val awayTeam: TextView = itemView.findViewById(R.id.away_team)
    }
}