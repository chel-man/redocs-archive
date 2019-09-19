package com.redocs.archive.ui.events

import com.redocs.archive.framework.EventBus

class PartitionNodeSelectedEvent(id: Long) : EventBus.Event<Long>(id)
class SelectPartitionNodeRequestEvent(id: Int) : EventBus.Event<Int>(id)
class ActivateDocumentListEvent() : EventBus.Event<Unit>(Unit)