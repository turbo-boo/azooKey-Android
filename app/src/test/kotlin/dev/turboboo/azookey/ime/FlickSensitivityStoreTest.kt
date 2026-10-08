package dev.turboboo.azookey.ime

import dev.turboboo.azookey.core.FlickDirection
import dev.turboboo.azookey.core.FlickDirectionResolver
import dev.turboboo.azookey.core.PointF2
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FlickSensitivityStoreTest {
    @Test fun sensitivityAffectsThresholdInExpectedDirection() {
        val store = FlickSensitivityStore(RuntimeEnvironment.getApplication())
        store.setPercent(100)
        assertEquals(25f, store.thresholdPx(1f), 0.0001f)

        store.setPercent(200)
        assertEquals(12.5f, store.thresholdPx(1f), 0.0001f)
        assertEquals(FlickDirection.LEFT,
            FlickDirectionResolver(store.thresholdPx(1f)).resolve(
                PointF2(40f, 0f), PointF2(20f, 0f)))

        store.setPercent(50)
        assertEquals(50f, store.thresholdPx(1f), 0.0001f)
        assertEquals(FlickDirection.CENTER,
            FlickDirectionResolver(store.thresholdPx(1f)).resolve(
                PointF2(40f, 0f), PointF2(20f, 0f)))

        store.setPercent(100)
    }

    @Test fun preferencePersistsAcrossStoreInstances() {
        val context = RuntimeEnvironment.getApplication()
        val first = FlickSensitivityStore(context)
        first.setPercent(145)
        assertEquals(145, FlickSensitivityStore(context).percent())
        first.setPercent(100)
    }

    @Test fun invalidSensitivityCannotBePersisted() {
        val store = FlickSensitivityStore(RuntimeEnvironment.getApplication())
        assertThrows(IllegalArgumentException::class.java) { store.setPercent(0) }
        assertThrows(IllegalArgumentException::class.java) { store.setPercent(201) }
    }
}
