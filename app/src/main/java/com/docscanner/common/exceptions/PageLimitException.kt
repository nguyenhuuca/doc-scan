package com.docscanner.common.exceptions

import com.docscanner.common.AppConfig

class PageLimitException(documentId: String) :
    IllegalStateException("Document $documentId has reached the maximum of ${AppConfig.MAX_PAGES} pages")
