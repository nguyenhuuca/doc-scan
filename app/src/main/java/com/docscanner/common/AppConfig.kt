package com.docscanner.common

object AppConfig {

    // ── Image storage ──────────────────────────────────────────────────────────
    /** Maximum stored image width in pixels — A4 portrait at 300 DPI. */
    const val IMAGE_MAX_WIDTH  = 2480
    /** Maximum stored image height in pixels — A4 portrait at 300 DPI. */
    const val IMAGE_MAX_HEIGHT = 3508
    /**
     * JPEG quality for stored page images (1–100).
     *   85 = print quality  (~850 KB/page, ~2.6 MB for 3 pages)
     *   75 = screen quality (~580 KB/page, ~1.8 MB for 3 pages)
     *   70 = compact        (~450 KB/page, ~1.4 MB for 3 pages)
     */
    const val IMAGE_JPEG_QUALITY = 85

    // ── PDF export ─────────────────────────────────────────────────────────────
    /** PDF page width in points — A4 at 72 pt/inch. */
    const val PDF_PAGE_WIDTH_PT  = 595
    /** PDF page height in points — A4 at 72 pt/inch. */
    const val PDF_PAGE_HEIGHT_PT = 842
    /** Margin around page content in points. */
    const val PDF_MARGIN_PT      = 10

    // ── Edit / undo ────────────────────────────────────────────────────────────
    /** Maximum number of undo states held in memory per editing session. */
    const val EDIT_MAX_UNDO_STACK     = 5
    /** JPEG quality for undo-stack frames — lower saves RAM, not saved to disk. */
    const val EDIT_UNDO_JPEG_QUALITY  = 75
    /** Debounce delay for brightness/contrast sliders in milliseconds. */
    const val EDIT_SLIDER_DEBOUNCE_MS = 300L
    /** Minimum brightness adjustment value for the edit slider. */
    const val EDIT_BRIGHTNESS_MIN = -255f
    /** Maximum brightness adjustment value for the edit slider. */
    const val EDIT_BRIGHTNESS_MAX = 255f
    /** Minimum contrast multiplier for the edit slider. */
    const val EDIT_CONTRAST_MIN = 0f
    /** Maximum contrast multiplier for the edit slider. */
    const val EDIT_CONTRAST_MAX = 2f

    // ── Limits ─────────────────────────────────────────────────────────────────
    /** Maximum number of documents the app will store. */
    const val MAX_DOCUMENTS       = 100
    /** Maximum pages per document. */
    const val MAX_PAGES           = 50
    /** Minimum free storage required before any save operation (50 MB). */
    const val MIN_STORAGE_BYTES   = 50L * 1024 * 1024
    /** Storage warning threshold — warn user when below this level (100 MB). */
    const val MIN_STORAGE_WARNING_BYTES = 100L * 1024 * 1024

    // ── Thumbnail ──────────────────────────────────────────────────────────────
    /** Maximum thumbnail dimension in pixels (square). */
    const val THUMBNAIL_MAX_SIZE    = 256
    /** JPEG quality for thumbnail images — lower than page quality to save space. */
    const val THUMBNAIL_JPEG_QUALITY = 50

    // ── Cache ─────────────────────────────────────────────────────────────────
    /** Subdirectory name inside cacheDir used for PDF and image exports. */
    const val EXPORT_CACHE_DIR = "export"
    /** Minimum document count before showing the search/sort bar on the list screen. */
    const val SEARCH_BAR_MIN_DOCUMENTS = 10
}
