package com.docscanner.common.exceptions

import com.docscanner.common.AppConfig

class StorageFullException(availableBytes: Long) :
    IllegalStateException("Insufficient storage: ${availableBytes / (1024 * 1024)} MB available. At least ${AppConfig.MIN_STORAGE_BYTES / (1024 * 1024)} MB required.")
