package com.moodiary.app.ui.nav

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moodiary.app.data.DiaryViewModel
import com.moodiary.app.data.ReviewPeriod
import com.moodiary.app.data.newerThan
import com.moodiary.app.data.olderThan
import com.moodiary.app.ui.components.MoodiaryBottomBar
import com.moodiary.app.ui.components.Tab
import com.moodiary.app.ui.screens.CalendarScreen
import com.moodiary.app.ui.screens.ChatScreen
import com.moodiary.app.ui.screens.DetailScreen
import com.moodiary.app.ui.screens.EditorScreen
import com.moodiary.app.ui.screens.InsightsScreen
import com.moodiary.app.ui.screens.MapPickerScreen
import com.moodiary.app.ui.screens.PlacePickerScreen
import com.moodiary.app.ui.screens.ApiKeyDialog
import com.moodiary.app.ui.screens.ProfileScreen
import com.moodiary.app.ui.screens.ReviewScreen
import com.moodiary.app.ui.screens.SearchScreen
import com.moodiary.app.ui.screens.TimelineScreen
import com.moodiary.app.ui.screens.UpdateScreen
import com.moodiary.app.ui.theme.MoodiaryColors
import com.moodiary.app.util.appVersionName

/**
 * Screens that cover the tabs completely. They form a back stack because the editor
 * can open the place picker, which can open the map picker — three deep. The tab
 * screens themselves stay where they are underneath.
 */
private sealed interface Overlay {
    data object Editor : Overlay
    data object Search : Overlay
    data object PlacePicker : Overlay
    data object MapPicker : Overlay
    data object Update : Overlay
    data class Review(val period: ReviewPeriod) : Overlay
    data object Chat : Overlay
    data class Detail(val entryId: String) : Overlay
}

/**
 * The whole app. Four tabs behind the bottom bar, plus the overlay stack above.
 * A NavHost would buy nothing here: there are no deep links and no arguments beyond
 * one entry id.
 */
@Composable
fun MoodiaryApp(vm: DiaryViewModel = viewModel()) {
    val context = LocalContext.current
    val entries by vm.entries.collectAsState()
    val version = remember { context.appVersionName() }

    var tab by remember { mutableStateOf(Tab.TIMELINE) }
    val stack = remember { mutableStateListOf<Overlay>() }
    var sheetOpen by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }
    var editingApiKey by remember { mutableStateOf(false) }
    // Android 13+ shows the listener's notification only with this permission; the
    // listener itself runs either way, so a refusal is not an error.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(Unit) { vm.checkForUpdate(version) }

    fun push(overlay: Overlay) = stack.add(overlay)
    fun pop() = stack.removeLastOrNull()
    fun closeAll() = stack.clear()

    Box(Modifier.fillMaxSize().background(MoodiaryColors.Background)) {
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                Tab.TIMELINE -> TimelineScreen(
                    modifier = Modifier.statusBarsPadding(),
                    entries = entries,
                    onSearch = { push(Overlay.Search) },
                    onOpenEntry = { push(Overlay.Detail(it.id)) },
                )
                Tab.CALENDAR -> CalendarScreen(
                    modifier = Modifier.statusBarsPadding(),
                    entries = entries,
                    month = vm.visibleMonth,
                    selected = vm.selectedDate,
                    onMonthChange = vm::showMonth,
                    onSelectDate = vm::selectDate,
                    onOpenEntry = { push(Overlay.Detail(it.id)) },
                )
                Tab.INSIGHTS -> InsightsScreen(
                    insights = vm.insights,
                    onOpen = { push(Overlay.Review(it)) },
                    onAsk = { push(Overlay.Chat) },
                    modifier = Modifier.statusBarsPadding(),
                )
                Tab.PROFILE -> ProfileScreen(
                    modifier = Modifier.statusBarsPadding(),
                    entries = entries,
                    updateVersion = vm.availableUpdate?.version,
                    hasApiKey = vm.hasApiKey,
                    autoTag = vm.autoTagEnabled,
                    onCheckUpdate = { push(Overlay.Update) },
                    onEditApiKey = { editingApiKey = true },
                    onAutoTagChange = vm::setAutoTag,
                    writingPrompt = vm.writingPromptEnabled,
                    onWritingPromptChange = vm::setWritingPrompt,
                    mcpEnabled = vm.mcpEnabled,
                    onMcpChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        vm.setMcp(enabled)
                    },
                    mcpCommand = vm.mcpCommand(),
                )
            }
        }

        MoodiaryBottomBar(
            current = tab,
            onSelect = { tab = it },
            onCompose = {
                vm.startNewEntry()
                push(Overlay.Editor)
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (editingApiKey) {
            ApiKeyDialog(
                onSave = { vm.saveApiKey(it); editingApiKey = false },
                onDismiss = { editingApiKey = false },
            )
        }

        val top = stack.lastOrNull()
        AnimatedVisibility(
            visible = top != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) {
            Box(Modifier.fillMaxSize().background(MoodiaryColors.Background)) {
                when (top) {
                    Overlay.Editor -> EditorScreen(
                        modifier = Modifier.statusBarsPadding(),
                        vm = vm,
                        onDismiss = {
                            vm.clearDraft()
                            pop()
                        },
                        onPublished = {
                            closeAll()
                            tab = Tab.TIMELINE
                        },
                        onPickPlace = {
                            vm.openPlacePicker()
                            push(Overlay.PlacePicker)
                        },
                    )

                    Overlay.Search -> SearchScreen(
                        modifier = Modifier.statusBarsPadding(),
                        entries = entries,
                        query = vm.searchQuery,
                        onQueryChange = vm::onSearchQueryChange,
                        onDismiss = {
                            vm.clearSearch()
                            pop()
                        },
                        onOpenEntry = { push(Overlay.Detail(it.id)) },
                    )

                    Overlay.PlacePicker -> PlacePickerScreen(
                        modifier = Modifier.statusBarsPadding(),
                        nearby = vm.nearbyPlaces,
                        frequent = vm.frequentPlaces(),
                        selected = vm.pendingPlace,
                        query = vm.placeQuery,
                        onQueryChange = vm::onPlaceQueryChange,
                        onSelect = vm::selectPlace,
                        onOpenMap = {
                            vm.openMapPicker()
                            push(Overlay.MapPicker)
                        },
                        onCancel = { pop() },
                        onDone = {
                            vm.commitPlace()
                            pop()
                        },
                    )

                    Overlay.MapPicker -> MapPickerScreen(
                        modifier = Modifier.statusBarsPadding(),
                        candidates = vm.pinPlaces,
                        selected = vm.pendingPlace,
                        onBack = {
                            vm.cancelMapPick()
                            pop()
                        },
                        onSelect = vm::selectPlace,
                        onCenterSettled = vm::resolvePin,
                        onConfirm = {
                            vm.commitPlace()
                            // Straight back to the editor: the map screen answers the
                            // same question the picker does.
                            pop()
                            pop()
                        },
                    )

                    Overlay.Chat -> ChatScreen(
                        modifier = Modifier.statusBarsPadding(),
                        messages = vm.chatMessages,
                        input = vm.chatInput,
                        busy = vm.chatBusy,
                        entries = entries,
                        onInputChange = vm::onChatInputChange,
                        onSend = { vm.sendChat(it) },
                        onClear = vm::clearChat,
                        onOpenEntry = { push(Overlay.Detail(it.id)) },
                        onBack = { pop() },
                    )
                    is Overlay.Review -> ReviewScreen(
                        modifier = Modifier.statusBarsPadding(),
                        period = top.period,
                        insight = vm.insights[top.period] ?: DiaryViewModel.InsightState.Idle,
                        onRefresh = { force -> vm.refreshInsight(top.period, force) },
                        onBack = { pop() },
                    )
                    Overlay.Update -> UpdateScreen(
                        modifier = Modifier.statusBarsPadding(),
                        currentVersion = version,
                        update = vm.availableUpdate,
                        onBack = { pop() },
                        onUpdate = { pop() },
                    )

                    is Overlay.Detail -> {
                        val entry = entries.firstOrNull { it.id == top.entryId }
                        if (entry == null) {
                            // The entry was deleted from under us.
                            LaunchedEffect(top.entryId) { pop() }
                        } else {
                            DetailScreen(
                                entry = entry,
                                older = entries.olderThan(entry),
                                newer = entries.newerThan(entry),
                                sheetOpen = sheetOpen,
                                confirmingDelete = confirmingDelete,
                                onBack = { pop() },
                                onOpenSheet = { sheetOpen = true },
                                onDismissSheet = { sheetOpen = false },
                                onEdit = {
                                    sheetOpen = false
                                    vm.startEditing(entry)
                                    push(Overlay.Editor)
                                },
                                onAskDelete = {
                                    sheetOpen = false
                                    confirmingDelete = true
                                },
                                onDismissDelete = { confirmingDelete = false },
                                onConfirmDelete = {
                                    confirmingDelete = false
                                    vm.deleteEntry(entry.id)
                                    pop()
                                },
                                onNavigate = { neighbour ->
                                    pop()
                                    push(Overlay.Detail(neighbour.id))
                                },
                            )
                        }
                    }

                    null -> Unit
                }
            }
        }
    }

    BackHandler(enabled = stack.isNotEmpty()) {
        when {
            confirmingDelete -> confirmingDelete = false
            sheetOpen -> sheetOpen = false
            else -> {
                if (stack.lastOrNull() == Overlay.Search) vm.clearSearch()
                if (stack.lastOrNull() == Overlay.Editor) vm.clearDraft()
                if (stack.lastOrNull() == Overlay.MapPicker) vm.cancelMapPick()
                pop()
            }
        }
    }
    BackHandler(enabled = stack.isEmpty() && tab != Tab.TIMELINE) {
        tab = Tab.TIMELINE
    }
}

