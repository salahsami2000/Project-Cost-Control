package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PptxExporter
import com.example.domain.EvmCalculator
import com.example.domain.SampleProjectFactory
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PptxExporterTest {

    @Test
    fun `exportToPptx generates valid OOXML zip with 9 slides and PNG screen previews`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val state = SampleProjectFactory.createSampleProject()
        val snapshot = EvmCalculator.computeSnapshot(state)
        val budget = EvmCalculator.calculateBudgetBreakdown(state)

        val out = ByteArrayOutputStream()
        PptxExporter.exportToPptx(context, state, snapshot, budget, out)

        val bytes = out.toByteArray()
        assertTrue("PPTX output should not be empty", bytes.size > 10_000)

        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                entries[entry.name] = zis.readBytes()
                entry = zis.nextEntry
            }
        }

        assertTrue(entries.containsKey("[Content_Types].xml"))
        assertTrue(entries.containsKey("ppt/presentation.xml"))
        for (i in 1..9) {
            assertTrue("Missing slide$i.xml", entries.containsKey("ppt/slides/slide$i.xml"))
            val img = entries["ppt/media/screen_$i.png"]
            assertNotNull("Missing screen_$i.png", img)
            // Verify PNG signature (89 50 4E 47)
            assertEquals(0x89.toByte(), img!![0])
            assertEquals(0x50.toByte(), img[1])
            assertEquals(0x4E.toByte(), img[2])
            assertEquals(0x47.toByte(), img[3])
        }
    }
}
