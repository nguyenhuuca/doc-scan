package com.docscanner.common.exceptions

class DocumentLimitException :
    IllegalStateException("Maximum of 100 documents reached. Delete existing documents to create new ones.")
