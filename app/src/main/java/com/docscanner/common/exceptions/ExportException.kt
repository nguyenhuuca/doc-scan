package com.docscanner.common.exceptions

class ExportException(cause: Throwable) :
    RuntimeException("PDF export failed: ${cause.message}", cause)
