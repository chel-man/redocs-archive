package com.redocs.archive.ui.events

import com.redocs.archive.domain.document.Document
import com.redocs.archive.framework.EventBus

class PartitionNodeSelectedEvent(id: Long) : EventBus.Event<Long>(id)
class SelectPartitionNodeRequestEvent(id: Long) : EventBus.Event<Long>(id)
class ActivateDocumentListEvent() : EventBus.Event<Unit>(Unit)
class ShowDocumentListRequestEvent(l: List<Document>) : EventBus.Event<List<Document>>(l)
class ShowDocumentEvent(doc: Document) : EventBus.Event<Document>(doc)
