package com.guanguanhua.app.data

data class TripSummary(
    val id: Long = 0,
    val name: String = "",
    val startedAt: Long = 0,
    val endedAt: Long? = null,
    val plannedEnd: String? = null,
    val createdBy: Long = 0,
    val createdByName: String? = null,
    val stopCount: Int = 0,
) {
    val active: Boolean get() = endedAt == null
}

data class TripStop(
    val id: Long = 0,
    val name: String = "",
    val kind: String = "",
    val rating: Int? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val visitedOn: String = "",
    val sortOrder: Int = 0,
    val createdBy: Long = 0,
    val createdByName: String? = null,
)

data class TripDetail(
    val id: Long = 0,
    val name: String = "",
    val startedAt: Long = 0,
    val endedAt: Long? = null,
    val plannedEnd: String? = null,
    val createdBy: Long = 0,
    val createdByName: String? = null,
    val stopCount: Int = 0,
    val stops: List<TripStop> = emptyList(),
) {
    val active: Boolean get() = endedAt == null
    fun asSummary(): TripSummary = TripSummary(
        id, name, startedAt, endedAt, plannedEnd, createdBy, createdByName,
        stopCount.takeIf { it > 0 } ?: stops.size,
    )
}

data class TripState(
    val trips: List<TripSummary> = emptyList(),
    val active: TripDetail? = null,
    val loaded: Boolean = false,
)

data class TripStopWrite(
    val name: String,
    val kind: String,
    val rating: Int? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val visitedOn: String,
)

data class NearbyPlace(
    val name: String,
    val kind: String?,
    val lat: Double,
    val lng: Double,
    val meters: Int,
)
