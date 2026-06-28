package com.docscanner.data.pdf

import android.graphics.BitmapFactory
import com.docscanner.common.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Builds a PDF by embedding JPEG pages with DCTDecode — the JPEG bytes from disk are
 * written directly into the PDF stream with no bitmap decode/re-encode step.
 * Result: PDF ≈ sum of JPEG files + ~500 bytes/page overhead.
 */
class PdfGenerator(private val cacheDir: File) {

    companion object {
        private val PAGE_WIDTH_PT  = AppConfig.PDF_PAGE_WIDTH_PT
        private val PAGE_HEIGHT_PT = AppConfig.PDF_PAGE_HEIGHT_PT
        private val MARGIN_PT      = AppConfig.PDF_MARGIN_PT
        private val DRAWABLE_W     = PAGE_WIDTH_PT  - 2 * MARGIN_PT
        private val DRAWABLE_H     = PAGE_HEIGHT_PT - 2 * MARGIN_PT
    }

    private data class JpegPage(val path: String, val width: Int, val height: Int)

    suspend fun generatePdf(documentId: String, pageImagePaths: List<String>): File =
        withContext(Dispatchers.IO) {
            val pages = pageImagePaths.mapNotNull { path ->
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, opts)
                if (opts.outWidth > 0 && opts.outHeight > 0)
                    JpegPage(path, opts.outWidth, opts.outHeight)
                else null
            }
            val exportDir = File(cacheDir, "export").also { it.mkdirs() }
            val outputFile = File(exportDir, "export_${System.currentTimeMillis()}.pdf")
            FileOutputStream(outputFile).use { writePdf(CountingOutputStream(it), pages) }
            outputFile
        }

    // PDF object layout:
    //   1 = Catalog,  2 = Pages
    //   per page i:   (3+i*3) = Page,  (4+i*3) = Content stream,  (5+i*3) = Image XObject
    private fun writePdf(out: CountingOutputStream, pages: List<JpegPage>) {
        val offsets = mutableListOf<Long>()

        out.ascii("%PDF-1.4\n")
        // 4 high bytes signal binary content to transfer tools
        out.write(byteArrayOf(0x25, 0xe2.toByte(), 0xe3.toByte(), 0xcf.toByte(), 0xd3.toByte(), 0x0a))

        // Catalog
        offsets += out.pos
        out.ascii("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        // Pages
        val pageNums = pages.indices.map { 3 + it * 3 }
        offsets += out.pos
        out.ascii("2 0 obj\n<< /Type /Pages /Kids [${pageNums.joinToString(" ") { "$it 0 R" }}] /Count ${pages.size} >>\nendobj\n")

        pages.forEachIndexed { i, page ->
            val pageObj = 3 + i * 3
            val contObj = 4 + i * 3
            val imgObj  = 5 + i * 3
            val imgName = "Im${i + 1}"

            // Scale to fit drawable area while preserving aspect ratio
            val scale = minOf(DRAWABLE_W.toFloat() / page.width, DRAWABLE_H.toFloat() / page.height)
            val imgW  = (page.width  * scale).toInt()
            val imgH  = (page.height * scale).toInt()
            val x     = MARGIN_PT + (DRAWABLE_W - imgW) / 2
            val y     = MARGIN_PT + (DRAWABLE_H - imgH) / 2

            val content = "q $imgW 0 0 $imgH $x $y cm /$imgName Do Q\n"
                .toByteArray(Charsets.US_ASCII)
            val jpegLen = File(page.path).length()

            // Page dictionary
            offsets += out.pos
            out.ascii(
                "$pageObj 0 obj\n" +
                "<< /Type /Page /Parent 2 0 R\n" +
                "   /MediaBox [0 0 $PAGE_WIDTH_PT $PAGE_HEIGHT_PT]\n" +
                "   /Resources << /XObject << /$imgName $imgObj 0 R >> >>\n" +
                "   /Contents $contObj 0 R\n" +
                ">>\nendobj\n"
            )

            // Content stream (page drawing commands)
            offsets += out.pos
            out.ascii("$contObj 0 obj\n<< /Length ${content.size} >>\nstream\n")
            out.write(content)
            out.ascii("\nendstream\nendobj\n")

            // Image XObject — JPEG embedded via DCTDecode, no bitmap decode
            offsets += out.pos
            out.ascii(
                "$imgObj 0 obj\n" +
                "<< /Type /XObject /Subtype /Image\n" +
                "   /Width ${page.width} /Height ${page.height}\n" +
                "   /ColorSpace /DeviceRGB /BitsPerComponent 8\n" +
                "   /Filter /DCTDecode /Length $jpegLen\n" +
                ">>\nstream\n"
            )
            FileInputStream(File(page.path)).use { it.copyTo(out) }
            out.ascii("\nendstream\nendobj\n")
        }

        // Cross-reference table — each entry is exactly 20 bytes per PDF spec
        val xrefPos  = out.pos
        val objCount = 2 + pages.size * 3
        out.ascii("xref\n0 ${objCount + 1}\n")
        out.ascii("0000000000 65535 f\r\n")             // free head entry
        offsets.forEach { off -> out.ascii("%010d 00000 n\r\n".format(off)) }

        out.ascii("trailer\n<< /Size ${objCount + 1} /Root 1 0 R >>\nstartxref\n$xrefPos\n%%EOF\n")
    }

    /** Wraps an OutputStream, tracking byte position for xref offset calculation. */
    private class CountingOutputStream(private val delegate: OutputStream) : OutputStream() {
        var pos: Long = 0

        fun ascii(s: String) = write(s.toByteArray(Charsets.US_ASCII))

        override fun write(b: Int)                       { delegate.write(b);       pos++ }
        override fun write(b: ByteArray)                 { delegate.write(b);       pos += b.size }
        override fun write(b: ByteArray, off: Int, len: Int) { delegate.write(b, off, len); pos += len }
        override fun flush() = delegate.flush()
        override fun close() = delegate.close()
    }
}
