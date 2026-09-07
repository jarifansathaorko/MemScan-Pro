package com.example

import com.example.memorylab.MemoryLabTarget
import com.example.memscan.backend.MockBackend
import com.example.memscan.engine.MemoryFreezer
import com.example.memscan.engine.MemoryScanner
import com.example.memscan.model.DataType
import com.example.memscan.model.FrozenAddress
import com.example.memscan.model.ScanMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MemoryScanRobolectricTest {

    @Test
    fun testAllDataTypeConversionsAndLittleEndian() {
        // Int8
        val i8 = DataType.INT8.parseToBytes("-42")
        assertNotNull(i8)
        assertEquals("-42", DataType.INT8.formatBytes(i8!!))

        // Int16
        val i16 = DataType.INT16.parseToBytes("12345")
        assertNotNull(i16)
        assertEquals(2, i16!!.size)
        val shortVal = ByteBuffer.wrap(i16).order(ByteOrder.LITTLE_ENDIAN).short
        assertEquals(12345.toShort(), shortVal)
        assertEquals("12345", DataType.INT16.formatBytes(i16))

        // Int32
        val i32 = DataType.INT32.parseToBytes("12450")
        assertNotNull(i32)
        assertEquals(4, i32!!.size)
        val intVal = ByteBuffer.wrap(i32).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(12450, intVal)
        assertEquals("12450", DataType.INT32.formatBytes(i32))

        // Int64
        val i64 = DataType.INT64.parseToBytes("9876543210")
        assertNotNull(i64)
        assertEquals(8, i64!!.size)
        val longVal = ByteBuffer.wrap(i64).order(ByteOrder.LITTLE_ENDIAN).long
        assertEquals(9876543210L, longVal)
        assertEquals("9876543210", DataType.INT64.formatBytes(i64))

        // Float32
        val f32 = DataType.FLOAT32.parseToBytes("31.42")
        assertNotNull(f32)
        assertEquals(4, f32!!.size)
        val floatVal = ByteBuffer.wrap(f32).order(ByteOrder.LITTLE_ENDIAN).float
        assertEquals(31.42f, floatVal, 0.001f)

        // Float64
        val f64 = DataType.FLOAT64.parseToBytes("183.7")
        assertNotNull(f64)
        assertEquals(8, f64!!.size)
        val doubleVal = ByteBuffer.wrap(f64).order(ByteOrder.LITTLE_ENDIAN).double
        assertEquals(183.7, doubleVal, 0.001)

        // Raw bytes
        val hex = DataType.RAW_BYTES.parseToBytes("DE AD BE EF")
        assertNotNull(hex)
        assertArrayEquals(byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte()), hex)
    }

    @Test
    fun testMemoryLabTargetVariablesAndReset() {
        MemoryLabTarget.reset()
        assertEquals(12450, MemoryLabTarget.views.value)
        assertEquals(427, MemoryLabTarget.subscribers.value)
        assertEquals(31.42f, MemoryLabTarget.revenue.value, 0.001f)
        assertEquals(183.7, MemoryLabTarget.watchTime.value, 0.001)

        // Increment Views
        MemoryLabTarget.incrementViews(1)
        assertEquals(12451, MemoryLabTarget.views.value)

        // Increment Subscribers
        MemoryLabTarget.incrementSubscribers(10)
        assertEquals(437, MemoryLabTarget.subscribers.value)

        // Reset
        MemoryLabTarget.reset()
        assertEquals(12450, MemoryLabTarget.views.value)
        assertEquals(427, MemoryLabTarget.subscribers.value)
    }

    @Test
    fun testEndToEndScanWorkflowAgainstMemoryLab() = runBlocking {
        MemoryLabTarget.reset()
        val mockBackend = MockBackend()
        mockBackend.attach(MemoryLabTarget.PID)

        val scanner = MemoryScanner(mockBackend)
        val regions = mockBackend.getMemoryRegions(MemoryLabTarget.PID)
        assertFalse(regions.isEmpty())

        // First scan: Exact value 12450 (Views variable)
        val targetBytes12450 = DataType.INT32.parseToBytes("12450")!!
        val results1 = scanner.performFirstScan(
            pid = MemoryLabTarget.PID,
            dataType = DataType.INT32,
            mode = ScanMode.EXACT,
            targetBytes = targetBytes12450,
            regions = regions
        )

        assertTrue("Should find at least one match for 12450", results1.isNotEmpty())
        val viewsMatch = results1.find { it.address == MemoryLabTarget.ADDR_VIEWS }
        assertNotNull("Should find MemoryLabTarget views address", viewsMatch)
        assertEquals("12450", viewsMatch!!.currentFormatted)

        // Mutate the variable (User clicks "+1 View" in MemoryLab)
        MemoryLabTarget.incrementViews(1)
        assertEquals(12451, MemoryLabTarget.views.value)

        // Next scan: Value Increased
        val resultsIncreased = scanner.performNextScan(
            pid = MemoryLabTarget.PID,
            previousResults = results1,
            dataType = DataType.INT32,
            mode = ScanMode.INCREASED,
            targetBytes = null
        )
        val viewsIncreasedMatch = resultsIncreased.find { it.address == MemoryLabTarget.ADDR_VIEWS }
        assertNotNull("Should match increased Views address", viewsIncreasedMatch)
        assertEquals("12451", viewsIncreasedMatch!!.currentFormatted)

        // Next scan: Exact value 12451
        val targetBytes12451 = DataType.INT32.parseToBytes("12451")!!
        val resultsExact = scanner.performNextScan(
            pid = MemoryLabTarget.PID,
            previousResults = resultsIncreased,
            dataType = DataType.INT32,
            mode = ScanMode.EXACT,
            targetBytes = targetBytes12451
        )
        assertEquals(1, resultsExact.count { it.address == MemoryLabTarget.ADDR_VIEWS })

        // Write new memory value: 99999
        val newBytes = DataType.INT32.parseToBytes("99999")!!
        val writeSuccess = mockBackend.writeMemory(MemoryLabTarget.PID, MemoryLabTarget.ADDR_VIEWS, newBytes)
        assertTrue(writeSuccess)

        // Check that MemoryLab variable was updated via write synchronization
        assertEquals(99999, MemoryLabTarget.views.value)
    }

    @Test
    fun testMemoryFreezerLogic() = runBlocking {
        MemoryLabTarget.reset()
        val mockBackend = MockBackend()
        mockBackend.attach(MemoryLabTarget.PID)

        val freezer = MemoryFreezer(
            backendProvider = { mockBackend },
            getPid = { MemoryLabTarget.PID }
        )

        val frozenBytes = DataType.INT32.parseToBytes("77777")!!
        val frozenItem = FrozenAddress(
            address = MemoryLabTarget.ADDR_VIEWS,
            dataType = DataType.INT32,
            targetValue = frozenBytes,
            currentValue = frozenBytes,
            description = "Views Lock",
            isFrozen = true
        )

        freezer.addAddress(frozenItem)
        assertTrue(freezer.frozenList.value.any { it.address == MemoryLabTarget.ADDR_VIEWS && it.isFrozen })

        // Wait for freeze daemon cycle to write
        delay(250)

        // Verify value was written by the daemon
        assertEquals(77777, MemoryLabTarget.views.value)

        freezer.stop()
    }

    @Test
    fun testSmartMultiStrategyScan() = runBlocking {
        MemoryLabTarget.reset()
        val mockBackend = MockBackend()
        mockBackend.attach(MemoryLabTarget.PID)

        val regions = mockBackend.getMemoryRegions(MemoryLabTarget.PID)
        val scanner = MemoryScanner(mockBackend)
        val results = scanner.performSmartScan(
            pid = MemoryLabTarget.PID,
            rawInput = "12450",
            regions = regions,
            filter = com.example.memscan.model.RegionFilter.ALL_REGIONS
        )

        assertTrue("Should detect exact int32 representation of 12450", results.isNotEmpty())
        assertTrue(results.any { it.address == MemoryLabTarget.ADDR_VIEWS })
        val match = results.first { it.address == MemoryLabTarget.ADDR_VIEWS }
        assertEquals(com.example.memscan.model.ScanStrategy.NUMERIC_EXACT, match.strategy)
    }

    @Test
    fun testMemoryInspectorStructCluster() = runBlocking {
        MemoryLabTarget.reset()
        val mockBackend = MockBackend()
        mockBackend.attach(MemoryLabTarget.PID)

        val inspector = com.example.memscan.engine.MemoryInspector(
            backendProvider = { mockBackend },
            getPid = { MemoryLabTarget.PID }
        )

        val fields = inspector.inspectObjectLayout(MemoryLabTarget.ADDR_VIEWS, radiusBytes = 32)
        assertTrue("Cluster should detect nearby fields", fields.size >= 2)
        assertTrue(fields.any { it.address == MemoryLabTarget.ADDR_VIEWS && it.int32Val == 12450 })
    }
}
