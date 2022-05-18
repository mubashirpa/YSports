package ysports.app.api.fixture

import com.google.gson.annotations.SerializedName

data class FixtureResponse(
    @SerializedName("fixtures") val fixtures: ArrayList<Fixtures>?
)