package com.redocs.archive.ui.events

import com.redocs.archive.framework.EventBus

class PartitionNodeSelectedEvent(id: Int) : EventBus.Event<Int>(id)
class SelectPartitionNodeRequestEvent(id: Int) : EventBus.Event<Int>(id)