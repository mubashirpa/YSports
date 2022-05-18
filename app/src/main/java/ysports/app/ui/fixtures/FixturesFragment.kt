package ysports.app.ui.fixtures

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.MediaItem
import com.google.android.material.progressindicator.CircularProgressIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ysports.app.R
import ysports.app.WebActivity
import ysports.app.YouTubePlayerActivity
import ysports.app.adapter.FixtureAdapter
import ysports.app.api.JsonApi
import ysports.app.api.fixture.FixtureResponse
import ysports.app.api.fixture.Fixtures
import ysports.app.api.fixture.Media
import ysports.app.databinding.FragmentFixturesBinding
import ysports.app.player.PlayerUtil
import ysports.app.util.AppUtil
import ysports.app.util.RecyclerDecorationVertical
import ysports.app.util.RecyclerTouchListener
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class FixturesFragment : Fragment() {

    private var _binding: FragmentFixturesBinding? = null
    private val binding get() = _binding!!

    private lateinit var fixtureRecyclerView: RecyclerView
    private lateinit var itemDecoration: RecyclerDecorationVertical
    private lateinit var progressBar: CircularProgressIndicator
    private var fixtureApi: Call<FixtureResponse>? = null
    private lateinit var errorLayout: View
    private lateinit var retryButton: Button
    private lateinit var stateDescription: TextView
    private var fixtureList: ArrayList<Fixtures> = ArrayList()
    private var filteredList: List<Fixtures> = ArrayList()
    private val dateFormat = SimpleDateFormat("dd MM yyyy", Locale.getDefault())
    private val currentDate = Date()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFixturesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = binding.progressBar
        errorLayout = view.findViewById(R.id.error_layout)
        retryButton = binding.errorLayout.buttonRetry
        stateDescription = binding.errorLayout.stateDescription
        fixtureRecyclerView = binding.recyclerViewFixture
        itemDecoration = RecyclerDecorationVertical(10)

        retryButton.setOnClickListener {
            errorLayout.hideView()
            progressBar.showView()
            Timer().schedule(500) {
                readDatabase()
            }
        }

        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            for (id in checkedIds) {
                if (id == R.id.chip_previous) {
                    if (fixtureList.isNotEmpty()) {
                        filteredList = fixtureList.filter {
                            !it.matchDate.isNullOrEmpty() && dateFormat.parse(it.matchDate)!! < currentDate
                        }
                        if (filteredList.isEmpty()) {
                            errorOccurred(R.string.error_no_matches, false)
                            return@setOnCheckedStateChangeListener
                        }
                        setRecyclerAdapter(filteredList as ArrayList<Fixtures>)
                    }
                }
                if (id == R.id.chip_today) {
                    if (fixtureList.isNotEmpty()) {
                        filteredList = fixtureList.filter {
                            !it.matchDate.isNullOrEmpty() && dateFormat.parse(it.matchDate)!! == currentDate
                        }
                        if (filteredList.isEmpty()) {
                            errorOccurred(R.string.error_no_matches_today, false)
                            return@setOnCheckedStateChangeListener
                        }
                        setRecyclerAdapter(filteredList as ArrayList<Fixtures>)
                    }
                }
                if (id == R.id.chip_upcoming) {
                    if (fixtureList.isNotEmpty()) {
                        filteredList = fixtureList.filter {
                            !it.matchDate.isNullOrEmpty() && dateFormat.parse(it.matchDate)!! > currentDate
                        }
                        if (filteredList.isEmpty()) {
                            errorOccurred(R.string.error_no_matches, false)
                            return@setOnCheckedStateChangeListener
                        }
                        setRecyclerAdapter(filteredList as ArrayList<Fixtures>)
                    }
                }
            }
        }

        fixtureRecyclerView.apply {
            itemAnimator = DefaultItemAnimator()
            addItemDecoration(itemDecoration)
            addOnItemTouchListener(
                RecyclerTouchListener(context, fixtureRecyclerView, object : RecyclerTouchListener.ClickListener {
                    override fun onClick(view: View, position: Int) {
                        if (!filteredList[position].url.isNullOrEmpty()) {
                            val url: String = filteredList[position].url!!
                            when {
                                url.startsWith("chrome://") -> {
                                    val replacedURL: String = url.replace("chrome://", "")
                                    AppUtil(requireContext()).openCustomTabs(replacedURL)
                                }
                                url.startsWith("video://") -> {
                                    val replacedURL = url.replace("video://", "")
                                    if (replacedURL.startsWith("https://youtu.be/")) {
                                        val intent = Intent(context, YouTubePlayerActivity::class.java).apply {
                                            putExtra("VIDEO_URL", replacedURL)
                                        }
                                        startActivity(intent)
                                    }
                                }
                                else -> {
                                    val intent = Intent(context, WebActivity::class.java).apply {
                                        putExtra("WEB_URL", url)
                                    }
                                    startActivity(intent)
                                }
                            }
                        } else if (!filteredList[position].media.isNullOrEmpty()) {
                            val mediaList: ArrayList<Media> = filteredList[position].media ?: ArrayList()
                            val playerUtil = PlayerUtil()
                            val mediaItems: List<MediaItem> = playerUtil.createMediaItems(
                                mediaList
                            )
                            playerUtil.loadPlayer(context, mediaItems, true)
                        }
                    }

                    override fun onLongClick(view: View, position: Int) {
                    }
                })
            )
        }
        readDatabase()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        fixtureApi?.cancel()
    }

    private fun View.showView() {
        if (!this.isVisible) this.visibility = View.VISIBLE
    }

    private fun View.hideView() {
        if (this.isVisible) this.visibility = View.GONE
    }

    private fun readDatabase() {
        fixtureApi = JsonApi.create("https://api.npoint.io/").getFixture("831085549ee2af13a198")
        fixtureApi!!.enqueue(object : Callback<FixtureResponse> {
            override fun onResponse(call: Call<FixtureResponse>, response: Response<FixtureResponse>) {
                if (!response.isSuccessful) {
                    errorOccurred(R.string.error_retrofit_response, false)
                    return
                }
                fixtureList = response.body()?.fixtures ?: ArrayList()
                if (fixtureList.isEmpty()) {
                    errorOccurred(R.string.error_no_matches, false)
                    return
                }
                binding.chipGroupContainer.showView()
                filteredList = fixtureList.filter {
                    !it.matchDate.isNullOrEmpty() && dateFormat.parse(it.matchDate)!! == currentDate
                }
                if (filteredList.isEmpty()) {
                    errorOccurred(R.string.error_no_matches_today, false)
                    return
                }
                setRecyclerAdapter(filteredList as ArrayList<Fixtures>)
            }

            override fun onFailure(call: Call<FixtureResponse>, t: Throwable) {
                if (!isDetached) {
                    errorOccurred(R.string.error_failed_to_load_content, true)
                }
            }
        })
    }

    private fun setRecyclerAdapter(arrayList: ArrayList<Fixtures>) {
        errorLayout.hideView()
        val fixtureAdapter = FixtureAdapter(requireContext(), arrayList)
        fixtureRecyclerView.adapter = fixtureAdapter
        progressBar.hideView()
        fixtureRecyclerView.showView()
    }

    private fun errorOccurred(error: Int, showButton: Boolean) {
        progressBar.hideView()
        fixtureRecyclerView.hideView()
        stateDescription.text = getString(error)
        retryButton.isVisible = showButton
        errorLayout.showView()
    }
}