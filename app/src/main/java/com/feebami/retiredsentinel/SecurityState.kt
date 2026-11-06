package com.feebami.retiredsentinel

/** Decision for one visible approach. This intentionally evaluates people separately. */
data class VisitorObservation(
    val trackId: Int,
    val identity: String,
    val carryingObject: Boolean = false,
    val approaching: Boolean = true
)

data class VisitorAlert(val trackId: Int, val identity: String, val carryingObject: Boolean)

class SecurityState(
    private val safeIdentities: List<String>,
    incidentTimeoutSec: Long,
    gracePeriodSec: Long,
    private val alertUnknown: () -> Boolean = { true },
    private val alertKnown: () -> Boolean = { false },
    private val alertObjects: () -> Boolean = { true },
    private val alertKnownObjects: () -> Boolean = { true },
) {
    private val incidentTimeoutMs = incidentTimeoutSec.coerceAtLeast(1) * 1000L
    private val confirmationMs = gracePeriodSec.coerceAtLeast(0) * 1000L
    private data class Visit(
        val startedAt: Long,
        var lastSeen: Long,
        var identity: String = "Unknown",
        var objectFrames: Int = 0,
        var alerted: Boolean = false
    )
    private val visits = mutableMapOf<Int, Visit>()

    /** Returns new alerts only; one visitor can never suppress another visitor. */
    fun update(current: List<VisitorObservation>, now: Long = android.os.SystemClock.elapsedRealtime()): List<VisitorAlert> {
        visits.entries.removeAll { now - it.value.lastSeen > incidentTimeoutMs }
        val alerts = mutableListOf<VisitorAlert>()
        for (person in current) {
            if (!person.approaching) continue
            val visit = visits.getOrPut(person.trackId) { Visit(now, now) }
            visit.lastSeen = now
            if (person.identity in safeIdentities) visit.identity = person.identity
            if (person.carryingObject) visit.objectFrames++
            if (visit.alerted || now - visit.startedAt < confirmationMs) continue

            val unknownAlert = visit.identity == "Unknown" && alertUnknown()
            val knownAlert = visit.identity != "Unknown" && alertKnown()
            val carryingAlert = visit.objectFrames >= 2 && alertObjects() &&
                (visit.identity == "Unknown" || alertKnownObjects())
            if (unknownAlert || knownAlert || carryingAlert) {
                visit.alerted = true
                alerts += VisitorAlert(person.trackId, visit.identity, visit.objectFrames >= 2)
            }
        }
        return alerts
    }
}
