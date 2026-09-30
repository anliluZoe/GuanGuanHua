package com.guanguanhua.app.trip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun overpassWaysUseCenterAndPreferChineseName() {
        val raw = """
            {"elements":[
              {"type":"way","center":{"lat":30.673,"lon":104.072},"tags":{"name":"Kuanzhai Alley","name:zh":"宽窄巷子","tourism":"attraction"}},
              {"type":"node","lat":30.674,"lon":104.073,"tags":{"name:zh-Hans":"龙抄手","amenity":"restaurant"}},
              {"type":"way","center":{"lat":30.675,"lon":104.074},"tags":{"amenity":"cafe"}}
            ]}
        """.trimIndent()
        val places = NearbyPlaces.parseOverpass(raw, 30.673, 104.072)
        assertEquals(2, places.size)
        assertEquals("宽窄巷子", places[0].name)
        assertEquals("风景", places[0].kind)
        assertEquals("龙抄手", places[1].name)
        assertEquals("美食", places[1].kind)
        assertTrue(places[0].meters < places[1].meters)
    }

    @Test
    fun photonJsonKeepsNamedPlacesAndMapsKinds() {
        val raw = """
            {"features":[
              {"type":"Feature","geometry":{"type":"Point","coordinates":[104.072,30.673]},"properties":{"name":"宽窄巷子","osm_key":"tourism","osm_value":"attraction"}},
              {"type":"Feature","geometry":{"type":"Point","coordinates":[104.073,30.674]},"properties":{"name":"龙抄手","osm_key":"amenity","osm_value":"restaurant"}},
              {"type":"Feature","geometry":{"type":"Point","coordinates":[104.074,30.675]},"properties":{"osm_key":"shop","osm_value":"convenience"}}
            ]}
        """.trimIndent()
        val places = NearbyPlaces.parsePhoton(raw, 30.673, 104.072)
        assertEquals(2, places.size)
        assertEquals("宽窄巷子", places[0].name)
        assertEquals("风景", places[0].kind)
        assertEquals("龙抄手", places[1].name)
        assertEquals("美食", places[1].kind)
        assertTrue(places[0].meters < places[1].meters)
    }
}
