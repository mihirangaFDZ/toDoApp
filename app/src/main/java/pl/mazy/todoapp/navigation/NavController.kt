package pl.mazy.todoapp.navigation

import androidx.compose.runtime.mutableStateOf

class NavController<DestinationBase>(beginning: DestinationBase) {
    private val backStack = mutableListOf(beginning)
    fun pop() {
        if (backStack.size > 1) backStack.removeLast()
        currentBackStackEntry.value = backStack.last()
    }

    var currentBackStackEntry = mutableStateOf(beginning)


    fun navigate(destination: DestinationBase) {
        backStack.add(destination)
        currentBackStackEntry.value = destination
    }
    fun isLast() = backStack.size == 1
}