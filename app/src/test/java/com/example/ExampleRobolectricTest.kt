package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.memscan.model.DataType
import com.example.memscan.model.ScanMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MemScan Pro", appName)
  }

  @Test
  fun `verify data type parsing and formatting`() {
    val int32Bytes = DataType.INT32.parseToBytes("450")
    assertNotNull(int32Bytes)
    assertEquals("450", DataType.INT32.formatBytes(int32Bytes!!))

    val floatBytes = DataType.FLOAT32.parseToBytes("1.75")
    assertNotNull(floatBytes)
    assertTrue(DataType.FLOAT32.formatBytes(floatBytes!!).startsWith("1.75"))

    val stringBytes = DataType.STRING_UTF8.parseToBytes("Runner")
    assertNotNull(stringBytes)
    assertEquals("Runner", DataType.STRING_UTF8.formatBytes(stringBytes!!))
  }
}
