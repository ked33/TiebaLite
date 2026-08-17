package com.huanchengfly.tieba.post.ui.page.forum

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VerticalAlignTop
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.eygraber.compose.placeholder.PlaceholderHighlight
import com.eygraber.compose.placeholder.material.fade
import com.eygraber.compose.placeholder.material.placeholder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.frsPage.ForumInfo
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.ImmutableHolder
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEventSuspend
import com.huanchengfly.tieba.post.arch.onEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.getBoolean
import com.huanchengfly.tieba.post.getInt
import com.huanchengfly.tieba.post.putBoolean
import com.huanchengfly.tieba.post.models.ForumHistoryExtra
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.toastShort
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumDetailPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ForumSearchPostPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ReplyPageDestination
import com.huanchengfly.tieba.post.ui.page.forum.generaltablist.GeneralTabListPage
import com.huanchengfly.tieba.post.ui.page.forum.generaltablist.GeneralTabListUiEvent
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadListPage
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadListType
import com.huanchengfly.tieba.post.ui.page.forum.threadlist.ForumThreadListUiEvent
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.ClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.FeedCardPlaceholder
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoad
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuScope
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.PagerTabIndicator
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.ScrollableTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TabClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.ui.widgets.compose.picker.ListSinglePicker
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.HistoryUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.requestPinShortcut
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.math.min


private val LoadDistance = 70.dp

private fun hideSpecialThreadsKey(forumName: String): String {
    return "${forumName}_hide_special_threads"
}

fun getSortType(
    context: Context,
    forumName: String,
): Int {
    val defaultSortType = context.appPreferences.defaultSortType?.toIntOrNull() ?: 0
    return context.dataStore.getInt("${forumName}_sort_type", defaultSortType)
}

private fun getHideSpecialThreads(
    context: Context,
    forumName: String,
): Boolean {
    return context.dataStore.getBoolean(hideSpecialThreadsKey(forumName), false)
}

suspend fun setSortType(
    context: Context,
    forumName: String,
    sortType: Int,
) {
    context.dataStore.edit {
        it[intPreferencesKey("${forumName}_sort_type")] = sortType
    }
}

private fun setHideSpecialThreads(
    context: Context,
    forumName: String,
    hide: Boolean,
) {
    context.dataStore.putBoolean(hideSpecialThreadsKey(forumName), hide)
}

@Composable
private fun ForumToolbarTitle(
    forumName: String,
    forumInfoImmutableHolder: ImmutableHolder<ForumInfo>?,
    onOpenForumInfo: () -> Unit,
    accountAction: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val forum = forumInfoImmutableHolder?.get()
    val titleModifier = if (forum != null) {
        modifier
            .fillMaxWidth()
            .debounceClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenForumInfo,
            )
    } else {
        modifier.fillMaxWidth()
    }

    Row(
        modifier = titleModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.title_forum, forumName),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (forum?.is_like == 1) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(
                        id = R.string.tip_forum_header_liked,
                        forum.user_level.toString(),
                        forum.level_name,
                    ),
                    style = MaterialTheme.typography.caption,
                    color = ExtendedTheme.colors.onTopBarSecondary,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                LinearProgressIndicator(
                    progress = max(
                        0F,
                        min(
                            1F,
                            forum.cur_score * 1.0F / max(1.0F, forum.levelup_score * 1.0F),
                        ),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(100)),
                    color = ExtendedTheme.colors.primary,
                    backgroundColor = ExtendedTheme.colors.onTopBar.copy(alpha = 0.16f),
                )
            )
        }
        accountAction()
    }
}

@Composable
private fun ForumToolbarAccountAction(
    forumInfoImmutableHolder: ImmutableHolder<ForumInfo>?,
    onBtnClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val forum = forumInfoImmutableHolder?.get() ?: return
    if (LocalAccount.current == null) return

    val btnEnabled =
        (forum.is_like != 1) || (forum.sign_in_info?.user_info?.is_sign_in != 1)
    Button(
        onClick = onBtnClick,
        modifier = modifier
            .height(36.dp),
        elevation = null,
        shape = RoundedCornerShape(100),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = ExtendedTheme.colors.primary,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = ExtendedTheme.colors.onTopBarSecondary,
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
        enabled = btnEnabled,
    ) {
        val text = when {
            forum.is_like != 1 -> stringResource(id = R.string.button_follow)
            forum.sign_in_info?.user_info?.is_sign_in == 1 -> stringResource(
                id = R.string.button_signed_in,
                forum.sign_in_info.user_info.cont_sign_num,
            )

            else -> stringResource(id = R.string.button_sign_in)
        }
        Text(text = text, fontSize = 11.sp, maxLines = 1)
    }
}

private fun shareForum(context: Context, forumName: String) {
    TiebaUtil.shareText(
        context,
        "https://tieba.baidu.com/f?kw=$forumName",
        context.getString(R.string.title_forum, forumName)
    )
}

private suspend fun sendToDesktop(
    context: Context,
    forum: ForumInfo,
    onSuccess: () -> Unit = {},
    onFailure: (String) -> Unit = {}
) {
    requestPinShortcut(
        context,
        "forum_${forum.id}",
        forum.avatar,
        context.getString(R.string.title_forum, forum.name),
        Intent(Intent.ACTION_VIEW).setData(Uri.parse("tblite://forum/${forum.name}")),
        onSuccess = onSuccess,
        onFailure = onFailure
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Destination(
    deepLinks = [
        DeepLink(uriPattern = "tblite://forum/{forumName}")
    ]
)
@Composable
fun ForumPage(
    forumName: String,
    viewModel: ForumViewModel = pageViewModel(),
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    LazyLoad(loaded = viewModel.initialized) {
        viewModel.send(ForumUiIntent.Load(forumName, getSortType(context, forumName)))
        viewModel.initialized = true
    }

    val scaffoldState = rememberScaffoldState()
    val snackbarHostState = scaffoldState.snackbarHostState
    viewModel.onEvent<ForumUiEvent.SignIn.Success> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_sign_success,
                "${it.signBonusPoint}",
                "${it.userSignRank}"
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.SignIn.Failure> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_sign_failed,
                it.errorMsg
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.Like.Success> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_like_success,
                it.memberSum,
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.Like.Failure> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_like_failed,
                it.errorMsg
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.Unlike.Success> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_unlike_success
            )
        )
    }
    viewModel.onEvent<ForumUiEvent.Unlike.Failure> {
        snackbarHostState.showSnackbar(
            message = context.getString(
                R.string.toast_unlike_failed,
                it.errorMsg
            )
        )
    }

    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = ForumUiState::isLoading,
        initial = false
    )
    val isError by viewModel.uiState.collectPartialAsState(
        prop1 = ForumUiState::isError,
        initial = false
    )
    val forumInfo by viewModel.uiState.collectPartialAsState(
        prop1 = ForumUiState::forum,
        initial = null
    )
    val tbs by viewModel.uiState.collectPartialAsState(prop1 = ForumUiState::tbs, initial = null)
    val navTabInfo by viewModel.uiState.collectPartialAsState(
        prop1 = ForumUiState::navTabInfo,
        initial = null
    )
    val generalTabs by remember {
        derivedStateOf {
            navTabInfo?.tab?.filter { it.isGeneralTab == 1 }?.filter { it.tabType == 15 } ?: emptyList()
        }
    }

    val account = LocalAccount.current
    val pagerState = rememberPagerState(pageCount = { 3 + generalTabs.size })
    val latestListState = rememberLazyListState()
    val hotListState = rememberLazyListState()
    val goodListState = rememberLazyListState()

    val currentPage by remember {
        derivedStateOf {
            pagerState.currentPage
        }
    }

    val currentListType by remember {
        derivedStateOf {
            when (currentPage) {
                1 -> ForumThreadListType.Hot
                2 -> ForumThreadListType.Good
                else -> ForumThreadListType.Latest
            }
        }
    }

    val currentListState = when (currentListType) {
        ForumThreadListType.Latest -> latestListState
        ForumThreadListType.Hot -> hotListState
        ForumThreadListType.Good -> goodListState
    }

    val coroutineScope = rememberCoroutineScope()
    val hideSpecialThreadsPrefKey = remember(forumName) {
        booleanPreferencesKey(hideSpecialThreadsKey(forumName))
    }
    val hideSpecialThreadsInitial = remember(forumName) {
        getHideSpecialThreads(context, forumName)
    }
    val hideSpecialThreads by context.dataStore.collectPreferenceAsState(
        key = hideSpecialThreadsPrefKey,
        defaultValue = hideSpecialThreadsInitial
    )

    val isListAtTop by remember {
        derivedStateOf {
            currentListState.firstVisibleItemIndex == 0 && currentListState.firstVisibleItemScrollOffset == 0
        }
    }

    val enablePullToRefresh by remember {
        derivedStateOf { currentPage < 3 && isListAtTop }
    }

    val unlikeDialogState = rememberDialogState()

    LaunchedEffect(forumInfo) {
        if (forumInfo != null && !context.appPreferences.incognitoMode) {
            val (forum) = forumInfo as ImmutableHolder<ForumInfo>
            HistoryUtil.saveHistory(
                History(
                    title = context.getString(R.string.title_forum, forum.name),
                    timestamp = System.currentTimeMillis(),
                    avatar = forum.avatar,
                    type = HistoryUtil.TYPE_FORUM,
                    data = forum.name,
                    extras = Json.encodeToString(ForumHistoryExtra(forum.id))
                )
            )
        }
    }

    if (account != null && forumInfo != null) {
        ConfirmDialog(
            dialogState = unlikeDialogState,
            onConfirm = {
                viewModel.send(
                    ForumUiIntent.Unlike(forumInfo!!.get { id }, forumName, tbs ?: account.tbs)
                )
            },
            title = {
                Text(
                    text = stringResource(
                        id = R.string.title_dialog_unfollow_forum,
                        forumName
                    )
                )
            }
        )
    }

    onGlobalEvent<GlobalEvent.AddThreadSuccess>() {
        coroutineScope.launch {
            if (currentPage >= 3) {
                emitGlobalEvent(GeneralTabListUiEvent.BackToTop)
                emitGlobalEvent(GeneralTabListUiEvent.Refresh())
            } else {
                emitGlobalEventSuspend(
                    ForumThreadListUiEvent.BackToTop(
                        currentListType
                    )
                )
                emitGlobalEventSuspend(
                    ForumThreadListUiEvent.Refresh(
                        currentListType,
                        getSortType(
                            context,
                            forumName
                        )
                    )
                )
            }
        }
    }

    onGlobalEvent<ForumThreadListUiEvent.AddThread>(
        filter = { it.forumName == forumName },
    ) {
        if (account == null) {
            context.toastShort(R.string.title_not_logged_in)
        } else if (forumInfo != null) {
            navigator.navigate(
                ReplyPageDestination(
                    forumId = forumInfo!!.get { id },
                    forumName = forumName,
                    threadId = 0L,
                )
            )
        } else context.toastShort(R.string.toast_add_thread_failed)
    }

    ProvideNavigator(navigator = navigator) {
        StateScreen(
            modifier = Modifier.fillMaxSize(),
            isEmpty = forumInfo == null,
            isError = isError,
            isLoading = isLoading,
            onReload = {
                viewModel.send(
                    ForumUiIntent.Load(
                        forumName,
                        getSortType(context, forumName)
                    )
                )
            },
            loadingScreen = {
                LoadingPlaceholder(forumName)
            }
        ) {
            MyScaffold(
                scaffoldState = scaffoldState,
                backgroundColor = Color.Transparent,
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    ForumToolbar(
                        forumName = forumName,
                        forumInfoImmutableHolder = forumInfo,
                        onOpenForumInfo = {
                            forumInfo?.let { holder ->
                                navigator.navigate(
                                    ForumDetailPageDestination(
                                        forumId = holder.get { id }
                                    )
                                )
                            }
                        },
                        onBtnClick = onBtnClick@{
                            forumInfo?.let { holder ->
                                val (forum) = holder
                                val accountTbs = tbs ?: account?.tbs ?: return@onBtnClick
                                when {
                                    forum.is_like != 1 -> viewModel.send(
                                        ForumUiIntent.Like(
                                            forum.id,
                                            forum.name,
                                            accountTbs
                                        )
                                    )

                                    forum.sign_in_info?.user_info?.is_sign_in != 1 -> {
                                        viewModel.send(
                                            ForumUiIntent.SignIn(
                                                forum.id,
                                                forum.name,
                                                accountTbs
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        menuContent = {
                            DropdownMenuItem(
                                onClick = {
                                    shareForum(context, forumName)
                                    dismiss()
                                }
                            ) {
                                Text(text = stringResource(id = R.string.title_share))
                            }
                            DropdownMenuItem(
                                onClick = {
                                    if (forumInfo != null) {
                                        val (forum) = forumInfo!!
                                        coroutineScope.launch {
                                            sendToDesktop(
                                                context,
                                                forum,
                                                onSuccess = {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = context.getString(
                                                                R.string.toast_send_to_desktop_success
                                                            )
                                                        )
                                                    }
                                                },
                                                onFailure = {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            message = context.getString(
                                                                R.string.toast_send_to_desktop_failed,
                                                                it
                                                            )
                                                        )
                                                    }
                                                }
                                            )
                                        }
                                    }
                                    dismiss()
                                }
                            ) {
                                Text(text = stringResource(id = R.string.title_send_to_desktop))
                            }
                            DropdownMenuItem(
                                onClick = {
                                    unlikeDialogState.show()
                                    dismiss()
                                }
                            ) {
                                Text(text = stringResource(id = R.string.title_unfollow))
                            }
                        },
                        forumId = forumInfo?.get { id }
                    )
                },
                floatingActionButton = {
                    if (context.appPreferences.forumFabFunction != "hide" || (context.appPreferences.hideReply == true && context.appPreferences.forumFabFunction == "post")) {
                        FloatingActionButton(
                            onClick = {
                                when (context.appPreferences.forumFabFunction) {
                                    "refresh" -> {
                                        coroutineScope.launch {
                                            if (currentPage >= 3) {
                                                emitGlobalEvent(GeneralTabListUiEvent.BackToTop)
                                                emitGlobalEvent(GeneralTabListUiEvent.Refresh())
                                            } else {
                                                emitGlobalEventSuspend(
                                                    ForumThreadListUiEvent.BackToTop(
                                                        currentListType
                                                    )
                                                )
                                                emitGlobalEventSuspend(
                                                    ForumThreadListUiEvent.Refresh(
                                                        currentListType,
                                                        getSortType(
                                                            context,
                                                            forumName
                                                        )
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    "back_to_top" -> {
                                        coroutineScope.launch {
                                            if (currentPage >= 3) {
                                                emitGlobalEvent(GeneralTabListUiEvent.BackToTop)
                                            } else {
                                                emitGlobalEvent(
                                                    ForumThreadListUiEvent.BackToTop(currentListType)
                                                )
                                            }
                                        }
                                    }

                                    else -> {
                                        coroutineScope.launch {
                                            emitGlobalEvent(
                                                ForumThreadListUiEvent.AddThread(forumName)
                                            )
                                        }
                                    }
                                }
                            },
                            backgroundColor = ExtendedTheme.colors.windowBackground,
                            contentColor = ExtendedTheme.colors.primary,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            Icon(
                                imageVector = when (context.appPreferences.forumFabFunction) {
                                    "refresh" -> Icons.Rounded.Refresh
                                    "back_to_top" -> Icons.Rounded.VerticalAlignTop
                                    else -> Icons.Rounded.Add
                                },
                                contentDescription = null
                            )
                        }
                    }
                }
            ) { contentPadding ->
                var isFakeLoading by remember { mutableStateOf(false) }
                LaunchedEffect(isFakeLoading) {
                    if (isFakeLoading) {
                        delay(1000)
                        isFakeLoading = false
                    }
                }

                PullToRefreshLayout(
                    refreshing = isFakeLoading,
                    enabled = enablePullToRefresh,
                    onRefresh = {
                        coroutineScope.emitGlobalEvent(
                            ForumThreadListUiEvent.Refresh(
                                currentListType,
                                getSortType(
                                    context,
                                    forumName
                                )
                            )
                        )
                        isFakeLoading = true
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .fillMaxSize()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            val tabTextStyle = MaterialTheme.typography.button.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 0.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ScrollableTabRow(
                                    selectedTabIndex = currentPage,
                                    indicator = { tabPositions ->
                                        PagerTabIndicator(
                                            pagerState = pagerState,
                                            tabPositions = tabPositions
                                        )
                                    },
                                    divider = {},
                                    backgroundColor = Color.Transparent,
                                    contentColor = ExtendedTheme.colors.primary,
                                    edgePadding = 0.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    var currentSortType by remember {
                                        mutableIntStateOf(
                                            getSortType(
                                                context,
                                                forumName
                                            )
                                        )
                                    }
                                    TabClickMenu(
                                        selected = currentPage == 0,
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(0)
                                            }
                                        },
                                        text = {
                                            Text(
                                                text = stringResource(id = R.string.tab_forum_latest),
                                                style = tabTextStyle
                                            )
                                        },
                                        menuContent = {
                                            ListSinglePicker(
                                                itemTitles = persistentListOf(
                                                    stringResource(id = R.string.title_sort_by_reply),
                                                    stringResource(id = R.string.title_sort_by_send)
                                                ),
                                                itemValues = persistentListOf(0, 1),
                                                selectedPosition = currentSortType,
                                                onItemSelected = { _, _, value, changed ->
                                                    if (changed) {
                                                        currentSortType = value
                                                        coroutineScope.launch {
                                                            setSortType(context, forumName, value)
                                                            emitGlobalEvent(
                                                                ForumThreadListUiEvent.Refresh(
                                                                    ForumThreadListType.Latest,
                                                                    value
                                                                )
                                                            )
                                                        }
                                                    }
                                                    dismiss()
                                                }
                                            )
                                        },
                                        selectedContentColor = ExtendedTheme.colors.primary,
                                        unselectedContentColor = ExtendedTheme.colors.textSecondary
                                    )
                                    Tab(
                                        selected = currentPage == 1,
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(1)
                                            }
                                        },
                                        selectedContentColor = ExtendedTheme.colors.primary,
                                        unselectedContentColor = ExtendedTheme.colors.textSecondary
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .height(48.dp)
                                                .padding(horizontal = 16.dp)
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.tab_forum_hot),
                                                style = tabTextStyle
                                            )
                                        }
                                    }
                                    Tab(
                                        selected = currentPage == 2,
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(2)
                                            }
                                        },
                                        selectedContentColor = ExtendedTheme.colors.primary,
                                        unselectedContentColor = ExtendedTheme.colors.textSecondary
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .height(48.dp)
                                                .padding(horizontal = 16.dp)
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.tab_forum_good),
                                                style = tabTextStyle
                                            )
                                        }
                                    }
                                    generalTabs.forEachIndexed { index, tab ->
                                        val tabIndex = 3 + index
                                        var currentSortIndex by remember(tab.tabId) {
                                            mutableIntStateOf(0)
                                        }
                                        if (tab.sort_menu.isNotEmpty()) {
                                            TabClickMenu(
                                                selected = currentPage == tabIndex,
                                                onClick = {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(tabIndex)
                                                    }
                                                },
                                                text = {
                                                    Text(
                                                        text = tab.tabName,
                                                        style = tabTextStyle
                                                    )
                                                },
                                                menuContent = {
                                                    ListSinglePicker(
                                                        itemTitles = tab.sort_menu.map { it.text }.toImmutableList(),
                                                        itemValues = tab.sort_menu.map { it.source_id }.toImmutableList(),
                                                        selectedPosition = currentSortIndex,
                                                        onItemSelected = { position, _, value, changed ->
                                                            if (changed) {
                                                                currentSortIndex = position
                                                                coroutineScope.launch {
                                                                    emitGlobalEvent(GeneralTabListUiEvent.Refresh(sortType = value))
                                                                }
                                                            }
                                                            dismiss()
                                                        }
                                                    )
                                                },
                                                selectedContentColor = ExtendedTheme.colors.primary,
                                                unselectedContentColor = ExtendedTheme.colors.textSecondary
                                            )
                                        } else {
                                            Tab(
                                                selected = currentPage == tabIndex,
                                                onClick = {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(tabIndex)
                                                    }
                                                },
                                                selectedContentColor = ExtendedTheme.colors.primary,
                                                unselectedContentColor = ExtendedTheme.colors.textSecondary
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .height(48.dp)
                                                        .padding(horizontal = 16.dp)
                                                ) {
                                                    Text(
                                                        text = tab.tabName,
                                                        style = tabTextStyle
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (currentPage < 3) {
                                    IconButton(
                                        onClick = {
                                            val nextValue = !hideSpecialThreads
                                            setHideSpecialThreads(context, forumName, nextValue)
                                        },
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (hideSpecialThreads) {
                                                Icons.Rounded.Visibility
                                            } else {
                                                Icons.Rounded.VisibilityOff
                                            },
                                            contentDescription = stringResource(
                                                id = if (hideSpecialThreads) {
                                                    R.string.desc_show_forum_special_posts
                                                } else {
                                                    R.string.desc_hide_forum_special_posts
                                                }
                                            ),
                                            tint = ExtendedTheme.colors.textSecondary
                                        )
                                    }
                                }
                            }

                            if (forumInfo != null) {
                                LazyLoadHorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    key = { it },
                                    verticalAlignment = Alignment.Top,
                                    userScrollEnabled = true,
                                ) { pageIndex ->
                                    when (pageIndex) {
                                        0, 1, 2 -> ForumThreadListPage(
                                            forumId = forumInfo!!.get { id },
                                            forumName = forumInfo!!.get { name },
                                            type = when (pageIndex) {
                                                1 -> ForumThreadListType.Hot
                                                2 -> ForumThreadListType.Good
                                                else -> ForumThreadListType.Latest
                                            },
                                            hideSpecialThreads = hideSpecialThreads,
                                            lazyListState = when (pageIndex) {
                                                1 -> hotListState
                                                2 -> goodListState
                                                else -> latestListState
                                            }
                                        )
                                        else -> {
                                            val tabIndex = pageIndex - 3
                                            if (tabIndex < generalTabs.size) {
                                                GeneralTabListPage(
                                                    forumId = forumInfo!!.get { id },
                                                    forumName = forumInfo!!.get { name },
                                                    navTabInfo = generalTabs[tabIndex],
                                                    viewModel = pageViewModel(key = "general_tab_$tabIndex"),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingPlaceholder(
    forumName: String
) {
    val context = LocalContext.current

    MyScaffold(
        backgroundColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            ForumToolbar(
                forumName = forumName,
                menuContent = {
                    DropdownMenuItem(
                        onClick = {
                            shareForum(context, forumName)
                            dismiss()
                        }
                    ) {
                        Text(text = stringResource(id = R.string.title_share))
                    }
                }
            )
        }
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            Row(modifier = Modifier.height(48.dp)) {
                persistentListOf(
                    stringResource(id = R.string.tab_forum_latest),
                    stringResource(id = R.string.tab_forum_hot),
                    stringResource(id = R.string.tab_forum_good),
                ).fastForEach {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = it,
                            modifier = Modifier.placeholder(
                                visible = true,
                                color = MaterialTheme.colors.surface,
                                highlight = PlaceholderHighlight.fade(),
                            ),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp,
                            style = MaterialTheme.typography.button,
                        )
                    }
                }
            }
            repeat(4) {
                FeedCardPlaceholder()
            }
        }
    }
}

@Composable
private fun ForumToolbar(
    forumName: String,
    forumInfoImmutableHolder: ImmutableHolder<ForumInfo>? = null,
    onOpenForumInfo: () -> Unit = {},
    onBtnClick: () -> Unit = {},
    menuContent: @Composable (MenuScope.() -> Unit)? = null,
    forumId: Long? = null,
) {
    val navigator = LocalNavigator.current
    Toolbar(
        title = {
            ForumToolbarTitle(
                forumName = forumName,
                forumInfoImmutableHolder = forumInfoImmutableHolder,
                onOpenForumInfo = onOpenForumInfo,
                accountAction = {
                    ForumToolbarAccountAction(
                        forumInfoImmutableHolder = forumInfoImmutableHolder,
                        onBtnClick = onBtnClick,
                        modifier = Modifier.padding(end = 52.dp),
                    )
                },
            )
        },
        navigationIcon = {
            BackNavigationIcon(onBackPressed = {
                val navigateUp =
                    navigator.navigateUp()
            })
        },
        actions = {
            if (forumId != null) {
                var lastClickTime by remember { mutableLongStateOf(0L) }
                IconButton(
                    onClick = {
                        val currentTime = System.currentTimeMillis()
                        val delayMillis = 500L
                        if (currentTime - lastClickTime >= delayMillis) {
                            navigator.navigate(ForumSearchPostPageDestination(forumName, forumId))
                        }
                        lastClickTime = currentTime
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(id = R.string.btn_search_in_forum)
                    )
                }
            }
            Box {
                if (menuContent != null) {
                    val menuState = rememberMenuState()
                    ClickMenu(
                        menuContent = menuContent,
                        menuState = menuState,
                        triggerShape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(id = R.string.btn_more)
                            )
                        }
                    }
                }
            }
        },
    )
}
