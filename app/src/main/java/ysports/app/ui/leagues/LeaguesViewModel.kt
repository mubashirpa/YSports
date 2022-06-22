package ysports.app.ui.leagues

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LeaguesViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Leagues are coming soon"
    }
    val text: LiveData<String> = _text
}