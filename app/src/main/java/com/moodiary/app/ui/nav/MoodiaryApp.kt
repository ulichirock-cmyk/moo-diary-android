package com.moodiary.app.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.compose.BackHandler
import com.moodiary.app.data.DiaryViewModel
import com.moodiary.app.data.toMarkdown
import com.moodiary.app.ui.components.MoodiaryBottomBar
import com.moodiary.app.ui.components.Tab
import com.moodiary.app.ui.screens.CalendarScreen
import com.moodiary.app.ui.screens.EditorScreen
import com.moodiary.app.ui.screens.InsightsScreen
import com.moodiary.app.ui.screens.ProfileScreen
import com.moodiary.app.ui.screens.SearchScreen
import com.moodiary.app.ui.screens.TimelineScreen
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.util.shareMarkdown

/**
 * The whole app. Four tabs live behind the translucent bottom bar; the editor and
 * search are full-screen overlays that cover it, which is how the design draws them
 * (neither screen 02 nor 04 has a bottom bar).
 */
@Composable
fun MoodiaryApp(vm: DiaryViewModel = viewModel()) {
    val context = LocalContext.current
    val entries by vm.entries.collectAsState()
    var tab by remember { mutableStateOf(Tab.TIMELINE) }
    var overlay by remember { mutableStateOf<Overlay?>(null) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    Box(
        Modifier
            .fillMaxSize()
            .background(MoodiaryColors.Background),
    ) {
        Box(Modifier.fillMaxSize().padding(top = statusBarPadding.calculateTopPadding())) {
            when (tab) {
                Tab.TIMELINE -> TimelineScreen(
                    entries = entries,
                    onSearch = { overlay = Overlay.SEARCH },
                )
                Tab.CALENDAR -> CalendarScreen(
                    entries = entries,
                    month = vm.visibleMonth,
                    selected = vm.selectedDate,
                    onMonthChange = vm::showMonth,
                    onSelectDate = vm::selectDate,
                )
                Tab.INSIGHTS -> InsightsScreen(entries = entries)
                Tab.PROFILE -> ProfileScreen(
                    entries = entries,
                    onExport = { context.shareMarkdown(entries.toMarkdown()) },
                )
            }
        }

        MoodiaryBottomBar(
            current = tab,
            onSelect = { tab = it },
            onCompose = { overlay = Overlay.EDITOR },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        AnimatedVisibility(
            visible = overlay != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MoodiaryColors.Background)
                    .padding(top = statusBarPadding.calculateTopPadding()),
            ) {
                when (overlay) {
                    Overlay.EDITOR -> EditorScreen(
                        vm = vm,
                        onDismiss = { overlay = null },
                        onPublished = {
                            overlay = null
                            tab = Tab.TIMELINE
                        },
                    )
                    Overlay.SEARCH -> SearchScreen(
                        entries = entries,
                        query = vm.searchQuery,
                        onQueryChange = vm::onSearchQueryChange,
                        onDismiss = {
                            vm.clearSearch()
                            overlay = null
                        },
                    )
                    null -> Unit
                }
            }
        }
    }

    BackHandler(enabled = overlay != null) {
        if (overlay == Overlay.SEARCH) vm.clearSearch()
        overlay = null
    }
    BackHandler(enabled = overlay == null && tab != Tab.TIMELINE) {
        tab = Tab.TIMELINE
    }
}

private enum class Overlay { EDITOR, SEARCH }
