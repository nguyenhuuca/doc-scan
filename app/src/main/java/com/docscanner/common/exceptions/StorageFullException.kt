package com.docscanner.common.exceptions

class StorageFullException(availableBytes: Long) :
    IllegalStateException("Insufficient storage: ${availableBytes / (1024 * 1024)} MB available. At least 50 MB required.")
