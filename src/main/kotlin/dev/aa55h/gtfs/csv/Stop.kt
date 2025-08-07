package dev.aa55h.gtfs.csv

data class Stop(
    var stopId: String?,
    var stopCode: String?,
    var stopName: String?,
    var stopDesc: String?,
    var stopLat: String?,
    var stopLon: String?,
    var zoneId: String?,
    var stopUrl: String?,
    var locationType: String?,
    var parentStation: String?,
    var stopTimezone: String?,
    var wheelchairBoarding: String?,
    var platformCode: String?
) {
    fun hasInvalidCoords(): Boolean {
        return stopLat.isNullOrBlank() || stopLon.isNullOrBlank() ||
                stopLat == "0" || stopLon == "0"
    }
    
    companion object {
        fun fromArray(values: Array<String>): Stop {
            return Stop(
                stopId = values.getOrNull(0),
                stopCode = values.getOrNull(1),
                stopName = values.getOrNull(2),
                stopDesc = values.getOrNull(3),
                stopLat = values.getOrNull(4),
                stopLon = values.getOrNull(5),
                zoneId = values.getOrNull(6),
                stopUrl = values.getOrNull(7),
                locationType = values.getOrNull(8),
                parentStation = values.getOrNull(9),
                stopTimezone = values.getOrNull(10),
                wheelchairBoarding = values.getOrNull(11),
                platformCode = values.getOrNull(12)
            )
        }

        fun Array<String>.toStop(): Stop {
            return fromArray(this)
        }
    }
}