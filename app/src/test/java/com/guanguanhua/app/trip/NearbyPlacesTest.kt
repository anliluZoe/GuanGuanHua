package com.guanguanhua.app.trip

import org.junit.Assert.assertEquals
import org.junit.Test

class NearbyPlacesTest {

    @Test
    fun overpassJsonKeepsNamedNodesAndMapsKinds() {
        val raw = """
            {"elements":[
              {"type":"node","lat":25.273,"lon":110.290,"tags":{"name":"某某粉店","amenity":"restaurant"}},
              {"type":"node","lat":25.274,"lon":110.291,"tags":{"name":"客栈","tourism":"guest_house"}},
              {"type":"node","lat":25.275,"lon":110.292,"tags":{"amenity":"restaurant"}}
            ]}
        """.trimIndent()
        val places = NearbyPlaces.parseOverpass(raw, 25.273, 110.29)
        assertEquals(2, places.size)
        assertEquals("某某粉店", places[0].name)
        assertEquals("美食", places[0].kind)
        assertEquals("住宿", places[1].kind)
    }
}
