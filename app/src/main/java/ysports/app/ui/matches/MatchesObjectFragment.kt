package ysports.app.ui.matches

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.text.Spannable
import android.text.SpannableString
import android.text.format.DateUtils
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.MediaItem
import ysports.app.PlayerChooserActivity
import ysports.app.R
import ysports.app.WebActivity
import ysports.app.YouTubePlayerActivity
import ysports.app.api.fixture.Fixtures
import ysports.app.api.fixture.Media
import ysports.app.databinding.FragmentMatchesObjectBinding
import ysports.app.player.PlayerUtil
import ysports.app.ui.matches.adapter.FixtureAdapter
import ysports.app.util.AppUtil
import ysports.app.widgets.recyclerview.ItemTouchListener
import ysports.app.widgets.recyclerview.VerticalSpacingItemDecoration
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*

const val ARG_POSITION = "position"

/* Empty constructor is added due to no constructor found error occurred when app restarts */
@Suppress("PrivatePropertyName")
class MatchesObjectFragment() : Fragment() {

    constructor(arrayList: ArrayList<Fixtures>) : this() {
        this.arrayList = arrayList
    }

    private var _binding: FragmentMatchesObjectBinding? = null
    private val binding get() = _binding!!

    private lateinit var fixtureRecyclerView: RecyclerView
    private lateinit var itemDecoration: VerticalSpacingItemDecoration
    private lateinit var errorLayout: View
    private var filteredList: List<Fixtures> = ArrayList()
    private var arrayList: ArrayList<Fixtures> = ArrayList()
    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
    private val calendar: Calendar = Calendar.getInstance()
    private val currentDate = Date()
    private val playerUtil = PlayerUtil()
    private val CHROME_SCHEME = "chrome:"
    private val VIDEO_SCHEME = "video:"
    private val MEDIA_SCHEME = "media:"
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

        fixtureRecyclerView = binding.recyclerView
        errorLayout = binding.errorLayout.root
        itemDecoration = VerticalSpacingItemDecoration(10, 10, 10)

        arguments?.takeIf { it.containsKey(ARG_POSITION) }?.apply {
            val position = getInt(ARG_POSITION)
            filterRecyclerView(position, arrayList)
        }

        fixtureRecyclerView.apply {
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(itemDecoration)
            addOnItemTouchListener(
                ItemTouchListener(context, fixtureRecyclerView, object : ItemTouchListener.ClickListener {
                    override fun onClick(view: View, position: Int) {
                        val url = filteredList[position].url
                        val mediaList: ArrayList<Media> = filteredList[position].media ?: ArrayList()

                        if (!url.isNullOrEmpty()) {
                            when {
                                url.startsWith(CHROME_SCHEME) -> {
                                    val replacedURL = URLDecoder.decode(url.substring(CHROME_SCHEME.length), "UTF-8")
                                    AppUtil(context).openCustomTabs(replacedURL)
                                }
                                url.startsWith(VIDEO_SCHEME) -> {
                                    val title = "${filteredList[position].homeTeam} vs ${filteredList[position].awayTeam} (${filteredList[position].leagueName})"
                                    val replacedURL = URLDecoder.decode(url.substring(VIDEO_SCHEME.length), "UTF-8")
                                    if (replacedURL.startsWith("https://youtu.be/")) {
                                        val intent = Intent(context, YouTubePlayerActivity::class.java).apply {
                                            putExtra("VIDEO_URL", replacedURL)
                                        }
                                        startActivity(intent)
                                    } else playerUtil.loadPlayer(context, Uri.parse(replacedURL), title, true)
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
                        } else if (mediaList.isNotEmpty()) {
                            val mediaItems: List<MediaItem> = playerUtil.createMediaItems(
                                mediaList
                            )
                            playerUtil.loadPlayer(context, mediaItems, true)
                        }
                    }

                    override fun onLongClick(view: View, position: Int) {
                        val popup = PopupMenu(context, view)
                        popup.menuInflater.inflate(R.menu.pop_menu_matches, popup.menu)

                        val spannable = SpannableString("${filteredList[position].homeTeam} vs ${filteredList[position].awayTeam}")
                        spannable.setSpan(ForegroundColorSpan(ContextCompat.getColor(context, R.color.primary)), 0, spannable.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
                        popup.menu.getItem(0).title = spannable
                        popup.menu.getItem(0).isEnabled = false

                        val timestamp = filteredList[position].timestamp
                        if (!timestamp.isNullOrEmpty()) {
                            val startTime = simpleDateFormat.parse(timestamp)
                            if (startTime != null && startTime.before(currentDate)) {
                                popup.menu.getItem(1).isEnabled = false
                            }
                        }

                        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
                            when (menuItem.itemId) {
                                R.id.add_calender -> {
                                    if (!timestamp.isNullOrEmpty()) {
                                        val startTime = simpleDateFormat.parse(timestamp)
                                        val title = "${filteredList[position].homeTeam} vs ${filteredList[position].awayTeam}"
                                        val description = "${filteredList[position].leagueName}"
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
                                    var subject = ""
                                    if (!timestamp.isNullOrEmpty()) {
                                        val matchTime = simpleDateFormat.parse(timestamp) as Date
                                        val timeFormatter = SimpleDateFormat("dd. LLL KK:mm aaa", Locale.getDefault())
                                        subject = "${timeFormatter.format(matchTime)}, ${filteredList[position].homeTeam} vs ${filteredList[position].awayTeam}\n#${getString(R.string.app_name)}"
                                    }
                                    shareText(subject, getString(R.string.url_download_app), getString(R.string.share))
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

    private fun filterRecyclerView(position: Int, fixtureList: ArrayList<Fixtures>) {
        if (fixtureList.isEmpty()) return
        when (position) {
            0 -> {
                filteredList = fixtureList.filter {
                    !it.timestamp.isNullOrEmpty() && checkDate(it.timestamp) == 2
                }
                if (filteredList.isEmpty()) {
                    errorOccurred(R.string.error_no_matches)
                    return
                }
                setRecyclerAdapter(filteredList as ArrayList<Fixtures>)
            }
            1 -> {
                filteredList = fixtureList.filter {
                    !it.timestamp.isNullOrEmpty() && checkDate(it.timestamp) == 1
                }
                if (filteredList.isEmpty()) {
                    errorOccurred(R.string.error_no_matches_today)
                    return
                }
                setRecyclerAdapter(filteredList as ArrayList<Fixtures>)
            }
            2 -> {
                filteredList = fixtureList.filter {
                    !it.timestamp.isNullOrEmpty() && checkDate(it.timestamp) == 3
                }
                if (filteredList.isEmpty()) {
                    errorOccurred(R.string.error_no_matches)
                    return
                }
                setRecyclerAdapter(filteredList as ArrayList<Fixtures>)
            }
        }
    }

    private fun setRecyclerAdapter(arrayList: ArrayList<Fixtures>) {
        val fixtureAdapter = FixtureAdapter(requireContext(), arrayList)
        fixtureRecyclerView.adapter = fixtureAdapter
        fixtureRecyclerView.showView()
    }

    private fun errorOccurred(error: Int) {
        fixtureRecyclerView.hideView()
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

    private fun shareText(subject: String, text: String, title: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, title))
    }
}