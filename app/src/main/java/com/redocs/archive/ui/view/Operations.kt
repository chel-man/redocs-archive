package com.redocs.archive.ui.view

sealed class Operation {
    var proccessed = false
}

data class InsertOperation(val pos: Int, val size: Int) : Operation()
data class RemoveOperation(val pos: Int, val size: Int) : Operation()
data class UpdateOperation(val pos: Int) : Operation()
data class Error(val ex: Exception) : Operation()
class Empty : Operation()

class NotFoundException(val data: Int,source: String) : Exception(source)
