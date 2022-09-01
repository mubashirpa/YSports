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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_fixture, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val matchTime = arrayList[position].matchTime ?: context.resources.getString(R.string._00_00)
        val matchDate = if (!arrayList[position].matchDate.isNullOrEmpty()) getDate(arrayList[position].matchDate!!) else Calendar.getInstance().get(Calendar.YEAR).toString()
        holder.matchTime.text = matchTime
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

    private fun getDate(date: String) : String {
        var newDate = date
        val dateFormat = SimpleDateFormat("dd MM yyyy", Locale.getDefault())
        val matchDate = dateFormat.parse(date)
        val toFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        if (matchDate != null) {
            newDate = toFormat.format(matchDate)
        }
        return newDate
    }
}