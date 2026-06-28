package com.docscanner.common.exceptions

class PageLimitException(documentId: String) :
    IllegalStateException("Document $documentId has reached the maximum of 50 pages")
