package com.redocs.archive.ui.events

import com.redocs.archive.domain.document.Document
import com.redocs.archive.framework.EventBus
import com.redocs.archive.ui.ContextActionSource

class PartitionNodeSelectedEvent(id: Long) : EventBus.Event<Long>(id)
class SelectPartitionNodeRequestEvent(id: Long) : EventBus.Event<Long>(id)
class ActivateDocumentListEvent() : EventBus.Event<Unit>(Unit)
class ShowDocumentListRequestEvent(l: List<Document>) : EventBus.Event<List<Document>>(l)
class DocumentSelectedEvent(doc: Document?) : EventBus.Event<Document?>(doc)
class ShowDocumentEvent() : EventBus.Event<Unit>(Unit)
class ContextActionRequestEvent(source: ContextActionSource) : EventBus.Event<ContextActionSource>(source)
class ContextActionStoppedEvent(source: ContextActionSource) : EventBus.Event<ContextActionSource>(source)
