package com.docscanner.common.exceptions

import com.docscanner.common.AppConfig

class DocumentLimitException :
    IllegalStateException("Maximum of ${AppConfig.MAX_DOCUMENTS} documents reached. Delete existing documents to create new ones.")
