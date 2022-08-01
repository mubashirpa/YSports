package ysports.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.util.JsonReader
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ExpandableListView.OnChildClickListener
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.ClippingConfiguration
import com.google.android.exoplayer2.MediaMetadata
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DataSourceInputStream
import com.google.android.exoplayer2.upstream.DataSourceUtil
import com.google.android.exoplayer2.upstream.DataSpec
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.util.Util
import com.google.common.base.Objects
import com.google.common.base.Preconditions.*
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import ysports.app.databinding.ActivityPlayerChooserBinding
import ysports.app.player.DemoUtil
import ysports.app.player.IntentUtil
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

@Suppress("PrivatePropertyName")
class PlayerChooserActivity : AppCompatActivity(), OnChildClickListener {

    private lateinit var binding: ActivityPlayerChooserBinding
    private lateinit var context: Context

    private val TAG = "ChooserActivity"
    private val GROUP_POSITION_PREFERENCE_KEY = "chooser_group_position"
    private val CHILD_POSITION_PREFERENCE_KEY = "chooser_child_position"

    private var uris: Array<String?> = arrayOf()
    private lateinit var listViewAdapter: ListViewAdapter
    private lateinit var expandableListView: ExpandableListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPlayerChooserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        context = this

        listViewAdapter = ListViewAdapter()
        expandableListView = binding.expandableListView

        expandableListView.setAdapter(listViewAdapter)
        expandableListView.setOnChildClickListener(this)

        val dataUri: String? = intent.dataString
        if (dataUri != null) {
            uris = arrayOf(dataUri)
        } else {
            val uriList: ArrayList<String> = ArrayList()
            val assetManager = assets
            try {
                for (asset in assetManager.list("")!!) {
                    if (asset.endsWith(".exolist.json")) {
                        uriList.add("asset:///$asset")
                    }
                }
            } catch (e: IOException) {
                Toast.makeText(context, "One or more sample lists failed to load", Toast.LENGTH_LONG).show()
            }
            uris = arrayOfNulls(uriList.size)
            uriList.toArray(uris)
            Arrays.sort(uris)
        }

        loadSample()
    }

    override fun onStart() {
        super.onStart()
        listViewAdapter.notifyDataSetChanged()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty()) {
            // Empty results are triggered if a permission is requested while another request was already
            // pending and can be safely ignored in this case.
            return
        }
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSample()
        } else {
            Toast.makeText(applicationContext, "One or more sample lists failed to load", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onChildClick(parent: ExpandableListView?, view: View?, groupPosition: Int, childPosition: Int, id: Long): Boolean {
        // Save the selected item first to be able to restore it if the tested code crashes.
        val prefEditor = getPreferences(MODE_PRIVATE).edit()
        prefEditor.putInt(GROUP_POSITION_PREFERENCE_KEY, groupPosition)
        prefEditor.putInt(CHILD_POSITION_PREFERENCE_KEY, childPosition)
        prefEditor.apply()

        val playlistHolder: PlaylistHolder = view?.tag as PlaylistHolder
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra(IntentUtil.PREFER_EXTENSION_DECODERS_EXTRA, true)
        IntentUtil.addToIntent(playlistHolder.mediaItems, intent)
        startActivity(intent)
        return true
    }

    private fun loadSample() {
        checkNotNull(uris)

        for (u in uris) {
            val uri = Uri.parse(u)
            if (Util.maybeRequestReadExternalStoragePermission(this, uri)) {
                return
            }
        }

        val loaderTask = ListLoader()
        loaderTask.execute(*uris)
    }

    private fun onPlaylistGroups(groups: MutableList<PlaylistGroup>, sawError: Boolean) {
        if (sawError) {
            Toast.makeText(applicationContext, "One or more sample lists failed to load", Toast.LENGTH_LONG).show()
        }
        listViewAdapter.setPlaylistGroups(groups)

        val preferences = getPreferences(MODE_PRIVATE)
        val groupPosition = preferences.getInt(GROUP_POSITION_PREFERENCE_KEY,  /* defValue= */-1)
        val childPosition = preferences.getInt(CHILD_POSITION_PREFERENCE_KEY,  /* defValue= */-1)
        // Clear the group and child position if either are unset or if either are out of bounds.
        if (groupPosition != -1 && childPosition != -1 && groupPosition < groups.size && childPosition < groups[groupPosition].playlists.size) {
            expandableListView.expandGroup(groupPosition) // shouldExpandGroup does not work without this.
            expandableListView.setSelectedChild(groupPosition, childPosition,  /* shouldExpandGroup= */true)
        }
    }

    @SuppressLint("StaticFieldLeak")
    @Suppress("LocalVariableName")
    private inner class ListLoader() : AsyncTask<String, Void, MutableList<PlaylistGroup>>() {

        var sawError = false

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg uris: String?): MutableList<PlaylistGroup> {
            val result: MutableList<PlaylistGroup> = ArrayList()
            val dataSource: DataSource = DemoUtil.getDataSourceFactory(context).createDataSource()
            for (uri in uris) {
                val dataSpec = DataSpec(Uri.parse(uri))
                val inputStream: InputStream = DataSourceInputStream(dataSource, dataSpec)
                try {
                    readPlaylistGroups(JsonReader(InputStreamReader(inputStream, "UTF-8")), result)
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading sample list: $uri", e)
                    sawError = true
                } finally {
                    DataSourceUtil.closeQuietly(dataSource)
                }
            }
            return result
        }

        @Deprecated("Deprecated in Java", ReplaceWith("onPlaylistGroups(result, sawError)"))
        override fun onPostExecute(result: MutableList<PlaylistGroup>) {
            onPlaylistGroups(result, sawError)
        }

        @Throws(IOException::class)
        private fun readPlaylistGroups(reader: JsonReader, groups: MutableList<PlaylistGroup>) {
            reader.beginArray()
            while (reader.hasNext()) {
                readPlaylistGroup(reader, groups)
            }
            reader.endArray()
        }

        @Throws(IOException::class)
        private fun readPlaylistGroup(reader: JsonReader, groups: MutableList<PlaylistGroup>) {
            var groupName = ""
            val playlistHolders: ArrayList<PlaylistHolder> = ArrayList()

            reader.beginObject()
            while (reader.hasNext()) {
                when (val name: String = reader.nextName()) {
                    "name" -> groupName = reader.nextString()
                    "samples" -> {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            playlistHolders.add(readEntry(reader, false))
                        }
                        reader.endArray()
                    }
                    "_comment" -> reader.nextString() // Ignore.
                    else -> throw IOException("Unsupported name: $name",  /* cause= */null)
                }
            }
            reader.endObject()

            val group = getGroup(groupName, groups)
            group.playlists.addAll(playlistHolders)
        }

        @Throws(IOException::class)
        private fun readEntry(reader: JsonReader, insidePlaylist: Boolean): PlaylistHolder {
            var uri: Uri? = null
            var extension: String? = null
            var title: String? = null
            var children: ArrayList<PlaylistHolder>? = null
            var subtitleUri: Uri? = null
            var subtitleMimeType: String? = null
            var subtitleLanguage: String? = null
            var drmUuid: UUID? = null
            var drmLicenseUri: String? = null
            var drmLicenseRequestHeaders: ImmutableMap<String, String> = ImmutableMap.of()
            var drmSessionForClearContent = false
            var drmMultiSession = false
            var drmForceDefaultLicenseUri = false
            val clippingConfiguration = ClippingConfiguration.Builder()

            val mediaItem = MediaItem.Builder()
            reader.beginObject()
            while (reader.hasNext()) {
                when (val name: String = reader.nextName()) {
                    "name" -> title = reader.nextString()
                    "uri" -> uri = Uri.parse(reader.nextString())
                    "extension" -> extension = reader.nextString()
                    "clip_start_position_ms" -> clippingConfiguration.setStartPositionMs(reader.nextLong())
                    "clip_end_position_ms" -> clippingConfiguration.setEndPositionMs(reader.nextLong())
                    "ad_tag_uri" -> mediaItem.setAdsConfiguration(MediaItem.AdsConfiguration.Builder(Uri.parse(reader.nextString())).build())
                    "drm_scheme" -> drmUuid = Util.getDrmUuid(reader.nextString())
                    "drm_license_uri", "drm_license_url" -> drmLicenseUri = reader.nextString()
                    "drm_key_request_properties" -> {
                        val requestHeaders: MutableMap<String, String> = HashMap()
                        reader.beginObject()
                        while (reader.hasNext()) {
                            requestHeaders[reader.nextName()] = reader.nextString()
                        }
                        reader.endObject()
                        drmLicenseRequestHeaders = ImmutableMap.copyOf(requestHeaders)
                    }
                    "drm_session_for_clear_content" -> drmSessionForClearContent = reader.nextBoolean()
                    "drm_multi_session" -> drmMultiSession = reader.nextBoolean()
                    "drm_force_default_license_uri" -> drmForceDefaultLicenseUri = reader.nextBoolean()
                    "subtitle_uri" -> subtitleUri = Uri.parse(reader.nextString())
                    "subtitle_mime_type" -> subtitleMimeType = reader.nextString()
                    "subtitle_language" -> subtitleLanguage = reader.nextString()
                    "playlist" -> {
                        checkState(!insidePlaylist, "Invalid nesting of playlists")
                        children = ArrayList()
                        reader.beginArray()
                        while (reader.hasNext()) {
                            children.add(readEntry(reader,  /* insidePlaylist= */true))
                        }
                        reader.endArray()
                    }
                    else -> throw IOException("Unsupported attribute name: $name",  /* cause= */null)
                }
            }
            reader.endObject()

            return if (children != null) {
                val mediaItems: MutableList<MediaItem> = ArrayList()
                for (i in 0 until children.size) {
                    mediaItems.addAll(children[i].mediaItems)
                }
                PlaylistHolder(title!!, mediaItems)
            } else {
                @Nullable val adaptiveMimeType = Util.getAdaptiveMimeTypeForContentType(
                    if (TextUtils.isEmpty(extension)) Util.inferContentType(uri!!) else Util.inferContentTypeForExtension(extension!!)
                )
                mediaItem
                    .setUri(uri)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
                    .setMimeType(adaptiveMimeType)
                    .setClippingConfiguration(clippingConfiguration.build())
                if (drmUuid != null) {
                    mediaItem.setDrmConfiguration(MediaItem.DrmConfiguration.Builder(drmUuid)
                        .setLicenseUri(drmLicenseUri)
                        .setLicenseRequestHeaders(drmLicenseRequestHeaders)
                        .setForceSessionsForAudioAndVideoTracks(drmSessionForClearContent)
                        .setMultiSession(drmMultiSession)
                        .setForceDefaultLicenseUri(drmForceDefaultLicenseUri)
                        .build())
                } else {
                    checkState(drmLicenseUri == null, "drm_uuid is required if drm_license_uri is set.")
                    checkState(drmLicenseRequestHeaders.isEmpty(), "drm_uuid is required if drm_key_request_properties is set.")
                    checkState(!drmSessionForClearContent, "drm_uuid is required if drm_session_for_clear_content is set.")
                    checkState(!drmMultiSession, "drm_uuid is required if drm_multi_session is set.")
                    checkState(!drmForceDefaultLicenseUri, "drm_uuid is required if drm_force_default_license_uri is set.")
                }
                if (subtitleUri != null) {
                    val subtitleConfiguration = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                        .setMimeType(checkNotNull(subtitleMimeType) {
                            "subtitle_mime_type is required if subtitle_uri is set."
                        })
                        .setLanguage(subtitleLanguage)
                        .build()
                    mediaItem.setSubtitleConfigurations(ImmutableList.of(subtitleConfiguration))
                }
                PlaylistHolder(title!!, Collections.singletonList(mediaItem.build()))
            }
        }

        private fun getGroup(groupName: String, groups: MutableList<PlaylistGroup>): PlaylistGroup {
            for (i in groups.indices) {
                if (Objects.equal(groupName, groups[i].title)) {
                    return groups[i]
                }
            }
            val group = PlaylistGroup(groupName)
            groups.add(group)
            return group
        }
    }

    private inner class ListViewAdapter : BaseExpandableListAdapter() {

        private var playlistGroups: MutableList<PlaylistGroup> = Collections.emptyList()

        override fun getGroupCount(): Int {
            return playlistGroups.size
        }

        override fun getChildrenCount(groupPosition: Int): Int {
            return getGroup(groupPosition).playlists.size
        }

        override fun getGroup(groupPosition: Int): PlaylistGroup {
            return playlistGroups[groupPosition]
        }

        override fun getChild(groupPosition: Int, childPosition: Int): PlaylistHolder {
            return getGroup(groupPosition).playlists[childPosition]
        }

        override fun getGroupId(groupPosition: Int): Long {
            return groupPosition.toLong()
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long {
            return childPosition.toLong()
        }

        override fun hasStableIds(): Boolean {
            return false
        }

        override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View {
            var view: View? = convertView
            if (view == null) {
                view = layoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
            }
            (view as TextView).text = getGroup(groupPosition).title
            return view
        }

        override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View {
            var view: View? = convertView
            if (view == null) {
                view = layoutInflater.inflate(R.layout.list_item_player_chooser, parent, false)
            }
            initializeChildView(view!!, getChild(groupPosition, childPosition))
            return view
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
            return true
        }

        fun setPlaylistGroups(playlistGroups: MutableList<PlaylistGroup>) {
            this.playlistGroups = playlistGroups
            notifyDataSetChanged()
        }

        private fun initializeChildView(view: View, playlistHolder: PlaylistHolder) {
            view.tag = playlistHolder
            val titleText: TextView = findViewById(R.id.title)
            titleText.text = playlistHolder.title
        }
    }

    private class PlaylistHolder(title: String, mediaItems: List<MediaItem>) {
        val title: String
        val mediaItems: List<MediaItem>

        init {
            checkArgument(mediaItems.isNotEmpty())
            this.title = title
            this.mediaItems = Collections.unmodifiableList(ArrayList(mediaItems))
        }
    }

    private class PlaylistGroup(val title: String) {
        val playlists: MutableList<PlaylistHolder>

        init {
            playlists = ArrayList()
        }
    }
}