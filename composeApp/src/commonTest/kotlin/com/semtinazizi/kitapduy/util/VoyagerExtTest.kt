package com.semtinazizi.kitapduy.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import kotlin.test.Test
import kotlin.test.assertSame

private class DummyScreen(val id: String) : Screen {
    @Composable
    override fun Content() {}
}

private class DummySaveableStateHolder : SaveableStateHolder {
    @Composable
    override fun SaveableStateProvider(key: Any, content: @Composable () -> Unit) {
        content()
    }
    override fun removeState(key: Any) {}
}

@OptIn(InternalVoyagerApi::class)
class VoyagerExtTest {

    private fun createNavigator(screens: List<Screen>, parent: Navigator? = null): Navigator {
        return Navigator(
            screens = screens,
            key = "key_${screens.firstOrNull()?.hashCode()}",
            stateHolder = DummySaveableStateHolder(),
            disposeBehavior = NavigatorDisposeBehavior(),
            parent = parent
        )
    }

    @Test
    fun rootReturnsSelfWhenNoParent() {
        val nav = createNavigator(listOf(DummyScreen("root")))
        assertSame(nav, nav.root)
    }

    @Test
    fun rootReturnsTopLevelNavigatorThroughNesting() {
        val rootNav = createNavigator(listOf(DummyScreen("root")))
        val childNav = createNavigator(listOf(DummyScreen("child")), parent = rootNav)
        val grandchildNav = createNavigator(listOf(DummyScreen("grandchild")), parent = childNav)

        assertSame(rootNav, childNav.root)
        assertSame(rootNav, grandchildNav.root)
    }
}
