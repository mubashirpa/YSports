package ysports.app.ui.matches

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.MediaItem
import ysports.app.PlayerChooserActivity
import ysports.app.R
import ysports.app.WebActivity
import ysports.app.YouTubePlayerActivity
import ysports.app.api.matches.Matches
import ysports.app.databinding.FragmentMatchesObjectBinding
import ysports.app.player.PlayerUtil
import ysports.app.ui.matches.adapter.MatchesAdapter
import ysports.app.util.AppUtil
import ysports.app.widgets.recyclerview.GridSpacingItemDecoration
import ysports.app.widgets.recyclerview.ItemTouchListener
import ysports.app.widgets.recyclerview.VerticalSpacingItemDecoration
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*

const val ARG_POSITION = "position"

/* Empty constructor is added due to no constructor found error occurred when app restarts */
@Suppress("PrivatePropertyName")
class MatchesObjectFragment() : Fragment() {

    constructor(list: List<Matches>) : this() {
        this.list = list
    }

    private var _binding: FragmentMatchesObjectBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private lateinit var errorLayout: View
    private var filteredList: List<Matches> = listOf()
    private var list: List<Matches> = listOf()
    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val calendar: Calendar = Calendar.getInstance()
    private val currentDate = Date()
    private val playerUtil = PlayerUtil()
    private val CHROME_SCHEME = "chrome:"
    private val VIDEO_SCHEME = "video:"
    private val MEDIA_SCHEME = "media:"
    private val YOUTUBE_SCHEME = "https://youtu.be/"
    private val TAG = "LogMatchesObjectFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMatchesObjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = binding.recyclerView
        errorLayout = binding.errorLayout.root
        val marginMin = (resources.getDimension(R.dimen.margin_min) / resources.displayMetrics.density).toInt()
        val verticalItemDecoration = VerticalSpacingItemDecoration(marginMin, marginMin)
        val gridItemDecoration = GridSpacingItemDecoration(2, marginMin, 0, includeEdge = true, isReverse = false)

        arguments?.takeIf { it.containsKey(ARG_POSITION) }?.apply {
            val position = getInt(ARG_POSITION)
            filterRecyclerView(position, list)
        }

        var recyclerLayoutManager = LinearLayoutManager(context)
        if (AppUtil(requireContext()).isTablet()) {
            recyclerLayoutManager = GridLayoutManager(context, 2)
            recyclerView.addItemDecoration(gridItemDecoration)
        } else {
            recyclerView.addItemDecoration(verticalItemDecoration)
        }

        recyclerView.apply {
            itemAnimator = DefaultItemAnimator()
            layoutManager = recyclerLayoutManager
            addOnItemTouchListener(
                ItemTouchListener(context, recyclerView, object : ItemTouchListener.ClickListener {
                    override fun onClick(view: View, position: Int) {
                        val url = filteredList[position].url
                        val media = filteredList[position].media

                        if (url != null) {
                            when {
                                url.startsWith(CHROME_SCHEME) -> {
                                    val replacedURL = URLDecoder.decode(url.substring(CHROME_SCHEME.length), "UTF-8")
                                    AppUtil(context).openCustomTabs(replacedURL)
                                }
                                url.startsWith(VIDEO_SCHEME) -> {
                                    val replacedURL = URLDecoder.decode(url.substring(VIDEO_SCHEME.length), "UTF-8")
                                    if (replacedURL.startsWith(YOUTUBE_SCHEME)) {
                                        val intent = Intent(context, YouTubePlayerActivity::class.java).apply {
                                            putExtra("VIDEO_URL", replacedURL)
                                        }
                                        startActivity(intent)
                                    } else {
                                        val title = "${filteredList[position].homeTeam} vs ${filteredList[position].awayTeam} (${filteredList[position].leagueName})"
                                        playerUtil.loadPlayer(context, Uri.parse(replacedURL), title, true)
                                    }
                                }
                                url.startsWith(MEDIA_SCHEME) -> {
                                    val replacedURL = URLDecoder.decode(url.substring(MEDIA_SCHEME.length), "UTF-8")
                                    val intent = Intent(context, PlayerChooserActivity::class.java).apply {
                                        putExtra("JSON_URL", replacedURL)
                                    }
                                    startActivity(intent)
                                }
                                else -> {
                                    val intent = Intent(context, WebActivity::class.java).apply {
                                        putExtra("WEB_URL", url)
                                    }
                                    startActivity(intent)
                                }
                            }
                        } else if (media != null) {
                            val mediaItems: List<MediaItem> = playerUtil.createMediaItems(
                                media
                            )
                            playerUtil.loadPlayer(context, mediaItems, true)
                        }
                    }

                    override fun onLongClick(view: View, position: Int) {
                        val popup = PopupMenu(context, view)
                        popup.menuInflater.inflate(R.menu.pop_menu_matches, popup.menu)

                        val timestamp = filteredList[position].timestamp
                        if (timestamp.isNotEmpty()) {
                            val startTime = simpleDateFormat.parse(timestamp)
                            if (startTime != null && startTime.before(currentDate)) {
                                popup.menu.getItem(0).isEnabled = false
                            }
                        }

                        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
                            when (menuItem.itemId) {
                                R.id.add_calender -> {
                                    if (timestamp.isNotEmpty()) {
                                        val startTime = simpleDateFormat.parse(timestamp)
                                        val title = "${filteredList[position].homeTeam} vs ${filteredList[position].awayTeam}"
                                        val description = filteredList[position].leagueName
                                        val intent = Intent(Intent.ACTION_EDIT).apply {
                                            type = "vnd.android.cursor.item/event"
                                            if (startTime != null) {
                                                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime.time)
                                                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, (startTime.time + 105 * 60 * 1000))
                                            }
                                            putExtra(CalendarContract.Events.TITLE, title)
                                            putExtra(CalendarContract.Events.DESCRIPTION, description)
                                        }
                                        startActivity(intent)
                                    }
                                    true
                                }
                                R.id.share -> {
                                    val title = "${filteredList[position].homeTeam} vs ${filteredList[position].awayTeam}"
                                    val subject = if (timestamp.isNotEmpty()) {
                                        val matchTime = simpleDateFormat.parse(timestamp) as Date
                                        val timeFormatter = SimpleDateFormat("dd. LLL KK:mm aaa", Locale.getDefault())
                                        "${timeFormatter.format(matchTime)}, ${filteredList[position].homeTeam} vs ${filteredList[position].awayTeam}\n#${getString(R.string.app_name)}"
                                    } else {
                                        "${filteredList[position].homeTeam} vs ${filteredList[position].awayTeam}\n#${getString(R.string.app_name)}"
                                    }
                                    shareText(title, subject, getString(R.string.url_download_app))
                                    true
                                }
                                else -> false
                            }
                        }
                        popup.show()
                    }
                })
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun View.showView() {
        if (!this.isVisible) this.visibility = View.VISIBLE
    }

    private fun View.hideView() {
        if (this.isVisible) this.visibility = View.GONE
    }

    private fun filterRecyclerView(position: Int, fixtureList: List<Matches>) {
        if (fixtureList.isEmpty()) return
        when (position) {
            0 -> {
                filteredList = fixtureList.filter {
                    it.timestamp.isNotEmpty() && checkDate(it.timestamp) == 2
                }
                if (filteredList.isEmpty()) {
                    errorOccurred(R.string.error_no_matches)
                    return
                }
                setRecyclerAdapter(filteredList)
            }
            1 -> {
                filteredList = fixtureList.filter {
                    it.timestamp.isNotEmpty() && checkDate(it.timestamp) == 1
                }
                if (filteredList.isEmpty()) {
                    errorOccurred(R.string.error_no_matches_today)
                    return
                }
                setRecyclerAdapter(filteredList)
            }
            2 -> {
                filteredList = fixtureList.filter {
                    it.timestamp.isNotEmpty() && checkDate(it.timestamp) == 3
                }
                if (filteredList.isEmpty()) {
                    errorOccurred(R.string.error_no_matches)
                    return
                }
                setRecyclerAdapter(filteredList)
            }
        }
    }

    private fun setRecyclerAdapter(list: List<Matches>) {
        val fixtureAdapter = MatchesAdapter(requireContext(), list)
        recyclerView.adapter = fixtureAdapter
        recyclerView.showView()
    }

    private fun errorOccurred(error: Int) {
        recyclerView.hideView()
        binding.errorLayout.stateDescription.text = getString(error)
        binding.errorLayout.buttonRetry.hideView()
        errorLayout.showView()
    }

    private fun checkDate(timestamp: String) : Int {
        val startTime = simpleDateFormat.parse(timestamp)
        if (startTime != null) {
            calendar.time = startTime
            if (DateUtils.isToday(calendar.timeInMillis)) {
                return 1
            } else if (calendar.time.before(currentDate)) {
                return 2
            } else if (calendar.time.after(currentDate)) {
                return 3
            }
        }
        Log.d(TAG, "Empty date")
        return 0
    }

    private fun shareText(title: String?, subject: String?, text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser: Intent = Intent.createChooser(sendIntent, title)
        if (sendIntent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(chooser)
        }
    }
}