package com.androidblunders.rakshak

import com.androidblunders.rakshak.core.model.ThreatLevel
import com.androidblunders.rakshak.orchestrator.DefaultThreatFusionEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultThreatFusionEngineTest {

    @Test
    fun publishUpdatesTheSharedConfidenceForSpecialisedPipelines() {
        val engine = DefaultThreatFusionEngine(emptySet())

        engine.publish(ThreatLevel.ACTIVE_THREAT, 0.82f)

        assertEquals(ThreatLevel.ACTIVE_THREAT, engine.currentThreatLevel.value)
        assertEquals(0.82f, engine.currentConfidence.value, 0f)
    }

    @Test
    fun publishClampsConfidenceToItsNormalizedRange() {
        val engine = DefaultThreatFusionEngine(emptySet())

        engine.publish(ThreatLevel.EMERGENCY, 2f)

        assertEquals(1f, engine.currentConfidence.value, 0f)
    }
}
