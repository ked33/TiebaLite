package com.huanchengfly.tieba.post.ui.page.main.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.eygraber.compose.placeholder.material.placeholder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.pullRefreshIndicator
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import com.huanchengfly.tieba.post.ui.widgets.compose.Chip
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.ErrorScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.LongClickMenu
import com.huanchengfly.tieba.post.ui.widgets.compose.MenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyVerticalGrid
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.TextButton
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import com.huanchengfly.tieba.post.ui.widgets.compose.debounceClickable
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberMenuState
import com.huanchengfly.tieba.post.ui.widgets.compose.states.StateScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.ImageUtil
import com.huanchengfly.tieba.post.models.database.History
import com.huanchengfly.tieba.post.utils.StringUtil.getShortNumString
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import kotlinx.collections.immutable.persistentListOf

private enum class ForumListLayout(
    val columns: Int,
    val preferenceValue: Int,
) {
    // Keep the existing persisted values stable; Triple was added after Quad.
    Single(columns = 1, preferenceValue = 0),
    Double(columns = 2, preferenceValue = 1),
    Triple(columns = 3, preferenceValue = 3),
    Quad(columns = 4, preferenceValue = 2),
    ;

    val isCompact: Boolean
        get() = columns >= Triple.columns

    fun next(): ForumListLayout = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromPreference(value: Int, legacyListSingle: Boolean): ForumListLayout =
            entries.firstOrNull { it.preferenceValue == value }
                ?: if (legacyListSingle) Single else Double
    }
}

private fun getGridCells(layout: ForumListLayout): GridCells = GridCells.Fixed(layout.columns)

@Preview("SearchBoxPreview")
@Composable
fun SearchBoxPreview() {
    SearchBox(
        backgroundColor = Color(0xFFF8F8F8),
        contentColor = Color(0xFFBFBFBF),
        onClick = {}
    )
}

@Composable
fun SearchBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = ExtendedTheme.colors.topBarSurface,
    contentColor: Color = ExtendedTheme.colors.onTopBarSurface,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(ExtendedTheme.colors.topBar)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            color = backgroundColor,
            contentColor = contentColor,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .debounceClickable(onClick = onClick)
        ) {
            Row(
                verticalAlignment = CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier
                        .align(CenterVertically)
                        .size(24.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(id = R.string.hint_search),
                    modifier = Modifier.align(CenterVertically),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun Header(
    text: String,
    modifier: Modifier = Modifier,
    invert: Boolean = false
) {
    Chip(
        text = text,
        modifier = Modifier
            .padding(start = 16.dp)
            .then(modifier),
        invertColor = invert
    )
}

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    itemCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotate by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        label = "sectionIndicatorRotation"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            color = ExtendedTheme.colors.text,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(id = R.string.text_forum_count, itemCount),
            style = MaterialTheme.typography.caption,
            color = ExtendedTheme.colors.textSecondary,
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = stringResource(
                id = if (expanded) {
                    R.string.desc_collapse_section
                } else {
                    R.string.desc_expand_section
                }
            ),
            tint = ExtendedTheme.colors.textSecondary,
            modifier = Modifier
                .size(24.dp)
                .rotate(rotate),
        )
    }
}

@Composable
private fun ForumLabel(
    title: String,
    modifier: Modifier = Modifier,
    avatar: String? = null,
    showAvatar: Boolean = false,
    largeCompact: Boolean = false,
) {
    val style = if (largeCompact) {
        MaterialTheme.typography.body2.copy(fontSize = 15.sp)
    } else {
        MaterialTheme.typography.body2
    }
    val avatarSize = if (largeCompact) {
        14.dp
    } else {
        with(LocalDensity.current) { style.fontSize.toDp() }
    }
    Row(
        modifier = modifier
            .height(36.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(color = ExtendedTheme.colors.chip)
            .padding(horizontal = 6.dp),
        verticalAlignment = CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        AnimatedVisibility(visible = showAvatar && avatar != null) {
            Row(verticalAlignment = CenterVertically) {
                Avatar(
                    data = avatar,
                    size = avatarSize,
                    contentDescription = stringResource(R.string.forum_portrait),
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
        Text(
            text = title,
            style = style,
            fontWeight = if (largeCompact) FontWeight.Bold else FontWeight.Medium,
            color = ExtendedTheme.colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CompactForumItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatar: String? = null,
    showAvatar: Boolean = false,
    largeCompact: Boolean = false,
) {
    ForumLabel(
        title = title,
        avatar = avatar,
        showAvatar = showAvatar,
        largeCompact = largeCompact,
        modifier = modifier.debounceClickable(onClick = onClick),
    )
}

@Composable
private fun <T> CompactForumRow(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            itemContent(item, Modifier.weight(1f))
        }
        repeat(columns - items.size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun HistoryForumItem(
    history: History,
    onClick: () -> Unit,
    onTogglePinned: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    showAvatar: Boolean = false,
    largeCompact: Boolean = false,
) {
    val menuState = rememberMenuState()
    LongClickMenu(
        menuContent = {
            DropdownMenuItem(
                onClick = {
                    onTogglePinned()
                    menuState.expanded = false
                }
            ) {
                Text(
                    text = stringResource(
                        if (history.isPinned) R.string.menu_top_del else R.string.menu_top
                    )
                )
            }
            DropdownMenuItem(
                onClick = {
                    onRemove()
                    menuState.expanded = false
                }
            ) {
                Text(text = stringResource(id = R.string.menu_remove))
            }
        },
        modifier = modifier,
        menuState = menuState,
        shape = RoundedCornerShape(6.dp),
        onClick = onClick,
    ) {
        ForumLabel(
            title = history.title.removeSuffix("吧"),
            avatar = history.avatar,
            showAvatar = showAvatar,
            largeCompact = largeCompact,
        )
    }
}

@Composable
private fun ForumItemPlaceholder(
    showAvatar: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (showAvatar) {
            Image(
                painter = rememberDrawablePainter(
                    drawable = ImageUtil.getPlaceHolder(
                        LocalContext.current,
                        0
                    )
                ),
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(40.dp)
                    .align(CenterVertically)
                    .placeholder(visible = true, color = ExtendedTheme.colors.chip),
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(
            color = ExtendedTheme.colors.text,
            text = "",
            modifier = Modifier
                .weight(1f)
                .align(CenterVertically)
                .placeholder(visible = true, color = ExtendedTheme.colors.chip),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(54.dp)
                .background(
                    color = ExtendedTheme.colors.chip,
                    shape = RoundedCornerShape(3.dp)
                )
                .padding(vertical = 4.dp)
                .align(CenterVertically)
                .placeholder(visible = true, color = ExtendedTheme.colors.chip)
        ) {
            Text(
                text = "0",
                color = ExtendedTheme.colors.onChip,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Center)
            )
        }
    }
}

@Composable
private fun ForumItemMenuContent(
    menuState: MenuState,
    isTopForum: Boolean,
    onDeleteTopForum: () -> Unit,
    onAddTopForum: () -> Unit,
    onCopyName: () -> Unit,
    onUnfollow: () -> Unit,
) {
    DropdownMenuItem(
        onClick = {
            if (isTopForum) {
                onDeleteTopForum()
            } else {
                onAddTopForum()
            }
            menuState.expanded = false
        }
    ) {
        if (isTopForum) {
            Text(text = stringResource(id = R.string.menu_top_del))
        } else {
            Text(text = stringResource(id = R.string.menu_top))
        }
    }
    DropdownMenuItem(
        onClick = {
            onCopyName()
            menuState.expanded = false
        }
    ) {
        Text(text = stringResource(id = R.string.title_copy_forum_name))
    }
    DropdownMenuItem(
        onClick = {
            onUnfollow()
            menuState.expanded = false
        }
    ) {
        Text(text = stringResource(id = R.string.button_unfollow))
    }
}

@Composable
private fun ForumItemContent(
    item: HomeUiState.Forum,
    showAvatar: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = CenterVertically
    ) {
        // 头像在左
        AnimatedVisibility(visible = showAvatar) {
            Row {
                Avatar(
                    data = item.avatar,
                    size = 40.dp,
                    contentDescription = stringResource(R.string.forum_portrait)
                )
                Spacer(modifier = Modifier.width(14.dp))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            // 第二列：贴吧名称
            Text(
                color = ExtendedTheme.colors.text,
                text = item.forumName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            AnimatedVisibility(visible = showAvatar) {
                Spacer(modifier = Modifier.height(2.dp))
                // 热度值显示
                Text(
                    text = stringResource(
                        R.string.hot_num,
                        item.hotNum.getShortNumString()
                    ),
                    color = ExtendedTheme.colors.onChip,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
        // 第三列：等级
        Column(
            modifier = Modifier
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            // 等级显示
            Box(
                modifier = Modifier
                    .width(54.dp)
                    .background(
                        color = ExtendedTheme.colors.chip,
                        shape = RoundedCornerShape(3.dp)
                    )
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.align(Center),
                ) {
                    Text(
                        text = "Lv.${item.levelId}",
                        color = ExtendedTheme.colors.onChip,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(CenterVertically)
                    )
                    if (item.isSign) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(id = R.string.tip_signed),
                            modifier = Modifier
                                .size(12.dp)
                                .align(CenterVertically),
                            tint = ExtendedTheme.colors.onChip
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForumItem(
    item: HomeUiState.Forum,
    showAvatar: Boolean,
    onClick: (HomeUiState.Forum) -> Unit,
    onUnfollow: (HomeUiState.Forum) -> Unit,
    onAddTopForum: (HomeUiState.Forum) -> Unit,
    onDeleteTopForum: (HomeUiState.Forum) -> Unit,
    isTopForum: Boolean = false,
) {
    val context = LocalContext.current
    val menuState = rememberMenuState()
    LongClickMenu(
        menuContent = {
            ForumItemMenuContent(
                menuState = menuState,
                isTopForum = isTopForum,
                onDeleteTopForum = { onDeleteTopForum(item) },
                onAddTopForum = { onAddTopForum(item) },
                onCopyName = {
                    TiebaUtil.copyText(context, item.forumName)
                },
                onUnfollow = { onUnfollow(item) }
            )
        },
        menuState = menuState,
        onClick = {
            onClick(item)
        }
    ) {
        ForumItemContent(item = item, showAvatar = showAvatar)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomePage(
    viewModel: HomeViewModel = pageViewModel(),
    canOpenExplore: Boolean = false,
    forumGridState: LazyGridState,
    onOpenExplore: () -> Unit = {},
) {
    val account = LocalAccount.current
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val isLoading by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::isLoading,
        initial = false
    )
    val forums by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::forums,
        initial = persistentListOf()
    )
    val topForums by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::topForums,
        initial = persistentListOf()
    )
    val historyForums by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::historyForums,
        initial = persistentListOf()
    )
    val expandHistoryForum by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::expandHistoryForum,
        initial = true
    )
    val error by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::error,
        initial = null
    )
    val hasLoaded by viewModel.uiState.collectPartialAsState(
        prop1 = HomeUiState::hasLoaded,
        initial = false
    )
    val isLoggedIn = remember(account) { account != null }
    val isEmpty by remember { derivedStateOf { forums.isEmpty() } }
    val showEmptyState by remember {
        derivedStateOf { isEmpty && (!isLoggedIn || hasLoaded) }
    }
    val hasTopForum by remember { derivedStateOf { topForums.isNotEmpty() } }
    val recentHistoryForums by remember {
        derivedStateOf {
            val followedForumNames = forums.mapTo(mutableSetOf()) { it.forumName }
            historyForums
                .asSequence()
                .filterNot { it.data in followedForumNames }
                .sortedWith(
                    compareByDescending<History> { it.isPinned }
                        .thenByDescending { it.timestamp }
                        .thenByDescending { it.count }
                )
                .toList()
        }
    }
    val showHistoryForum by remember {
        derivedStateOf {
            context.appPreferences.homePageShowHistoryForum && recentHistoryForums.isNotEmpty()
        }
    }
    var forumListLayout by remember {
        mutableStateOf(
            ForumListLayout.fromPreference(
                context.appPreferences.homeForumListLayout,
                context.appPreferences.listSingle,
            )
        )
    }
    var expandFollowedForums by rememberSaveable { mutableStateOf(true) }
    val isError by remember { derivedStateOf { error != null } }
    val gridCells by remember { derivedStateOf { getGridCells(forumListLayout) } }

    onGlobalEvent<GlobalEvent.Refresh>(
        filter = { it.key == "home" }
    ) {
        if (isLoggedIn) viewModel.send(HomeUiIntent.Refresh)
    }

    var unfollowForum by remember { mutableStateOf<HomeUiState.Forum?>(null) }
    val confirmUnfollowDialog = rememberDialogState()
    ConfirmDialog(
        dialogState = confirmUnfollowDialog,
        onConfirm = {
            unfollowForum?.let {
                viewModel.send(HomeUiIntent.Unfollow(it.forumId, it.forumName))
            }
        },
    ) {
        Text(
            text = stringResource(
                id = R.string.title_dialog_unfollow_forum,
                unfollowForum?.forumName.orEmpty()
            )
        )
    }

    LaunchedEffect(Unit) {
        if (isLoggedIn && !viewModel.initialized) {
            viewModel.send(HomeUiIntent.RefreshHistory)
            viewModel.send(HomeUiIntent.Refresh)
        }
    }
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            Toolbar(
                title = stringResource(id = R.string.title_main),
                navigationIcon = accountNavIconIfCompact(),
                actions = {
                    ActionItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_oksign),
                        contentDescription = stringResource(id = R.string.title_oksign)
                    ) {
                        TiebaUtil.startSign(context)
                    }
                    ActionItem(
                        icon = Icons.Outlined.ViewAgenda,
                        contentDescription = stringResource(
                            id = when (forumListLayout.next()) {
                                ForumListLayout.Single -> R.string.title_switch_list_single
                                ForumListLayout.Double -> R.string.title_switch_list_double
                                ForumListLayout.Triple -> R.string.title_switch_list_triple
                                ForumListLayout.Quad -> R.string.title_switch_list_quad
                            }
                        )
                    ) {
                        val nextLayout = forumListLayout.next()
                        context.appPreferences.homeForumListLayout = nextLayout.preferenceValue
                        context.appPreferences.listSingle = nextLayout == ForumListLayout.Single
                        forumListLayout = nextLayout
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { contentPaddings ->
        val pullRefreshState = rememberPullRefreshState(
            refreshing = isLoading,
            onRefresh = { if (isLoggedIn) viewModel.send(HomeUiIntent.Refresh) }
        )
        Box(
            modifier = Modifier
                .pullRefresh(pullRefreshState)
                .padding(contentPaddings)
        ) {
            Column {
                SearchBox(modifier = Modifier.padding(bottom = 4.dp)) {
                    navigator.navigate(SearchPageDestination)
                }
                StateScreen(
                    isEmpty = showEmptyState,
                    isError = isError,
                    isLoading = isLoading,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    onReload = {
                        if (isLoggedIn) viewModel.send(HomeUiIntent.Refresh)
                    },
                    emptyScreen = {
                        EmptyScreen(
                            loggedIn = isLoggedIn,
                            canOpenExplore = canOpenExplore,
                            onOpenExplore = onOpenExplore
                        )
                    },
                    loadingScreen = {
                        HomePageSkeletonScreen(
                            forumListLayout = forumListLayout,
                            gridCells = gridCells,
                        )
                    },
                    errorScreen = {
                        error?.let { ErrorScreen(error = it) }
                    }
                ) {
                    MyLazyVerticalGrid(
                        columns = gridCells,
                        state = forumGridState,
                        contentPadding = PaddingValues(bottom = 12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (showHistoryForum) {
                            item(key = "HistoryForums", span = { GridItemSpan(maxLineSpan) }) {
                                Column {
                                    CollapsibleSectionHeader(
                                        title = stringResource(id = R.string.title_history_forum),
                                        itemCount = recentHistoryForums.size,
                                        expanded = expandHistoryForum,
                                        onToggle = {
                                            viewModel.send(
                                                HomeUiIntent.ToggleHistory(expandHistoryForum)
                                            )
                                        }
                                    )
                                    AnimatedVisibility(visible = expandHistoryForum) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                                .padding(bottom = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            val historyColumns = if (forumListLayout.isCompact) {
                                                forumListLayout.columns
                                            } else {
                                                ForumListLayout.Quad.columns
                                            }
                                            recentHistoryForums.chunked(historyColumns).forEach { forumRow ->
                                                CompactForumRow(
                                                    items = forumRow,
                                                    columns = historyColumns,
                                                ) { forum, itemModifier ->
                                                    HistoryForumItem(
                                                        history = forum,
                                                        modifier = itemModifier,
                                                        showAvatar = forumListLayout.isCompact,
                                                        largeCompact = forumListLayout == ForumListLayout.Triple,
                                                        onClick = {
                                                            navigator.navigate(
                                                                ForumPageDestination(forum.data)
                                                            )
                                                        },
                                                        onTogglePinned = {
                                                            viewModel.send(
                                                                HomeUiIntent.SetHistoryPinned(
                                                                    historyId = forum.id,
                                                                    isPinned = !forum.isPinned,
                                                                )
                                                            )
                                                        },
                                                        onRemove = {
                                                            viewModel.send(
                                                                HomeUiIntent.DeleteHistory(forum.id)
                                                            )
                                                        },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (hasTopForum) {
                            item(key = "TopForumHeader", span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    Header(
                                        text = stringResource(id = R.string.title_top_forum),
                                        invert = true
                                    )
                                }
                            }
                            items(
                                items = topForums,
                                key = { "Top${it.forumId}" }
                            ) { item ->
                                if (forumListLayout.isCompact) {
                                    CompactForumItem(
                                        title = item.forumName.removeSuffix("吧"),
                                        modifier = Modifier.padding(4.dp),
                                        onClick = {
                                            navigator.navigate(ForumPageDestination(item.forumName))
                                        },
                                    )
                                } else {
                                    ForumItem(
                                        item,
                                        true,
                                        onClick = {
                                            navigator.navigate(ForumPageDestination(it.forumName))
                                        },
                                        onUnfollow = {
                                            unfollowForum = it
                                            confirmUnfollowDialog.show()
                                        },
                                        onAddTopForum = {
                                            viewModel.send(HomeUiIntent.TopForums.Add(it))
                                        },
                                        onDeleteTopForum = {
                                            viewModel.send(HomeUiIntent.TopForums.Delete(it.forumId))
                                        },
                                        isTopForum = true
                                    )
                                }
                            }
                        }
                        item(key = "ForumHeader", span = { GridItemSpan(maxLineSpan) }) {
                            CollapsibleSectionHeader(
                                title = stringResource(id = R.string.forum_list_title),
                                itemCount = forums.size,
                                expanded = expandFollowedForums,
                                onToggle = {
                                    expandFollowedForums = !expandFollowedForums
                                }
                            )
                        }
                        if (expandFollowedForums) {
                            if (forumListLayout.isCompact) {
                                val forumRows = forums.chunked(forumListLayout.columns)
                                itemsIndexed(
                                    items = forumRows,
                                    key = { _, forumRow -> "ForumRow${forumRow.first().forumId}" },
                                    span = { _, _ -> GridItemSpan(maxLineSpan) },
                                ) { rowIndex, forumRow ->
                                    CompactForumRow(
                                        items = forumRow,
                                        columns = forumListLayout.columns,
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .padding(
                                                top = if (rowIndex == 0) 0.dp else 4.dp,
                                                bottom = if (rowIndex == forumRows.lastIndex) {
                                                    12.dp
                                                } else {
                                                    4.dp
                                                },
                                            ),
                                    ) { item, itemModifier ->
                                        CompactForumItem(
                                            title = item.forumName.removeSuffix("吧"),
                                            avatar = item.avatar,
                                            showAvatar = true,
                                            largeCompact = forumListLayout == ForumListLayout.Triple,
                                            modifier = itemModifier,
                                            onClick = {
                                                navigator.navigate(
                                                    ForumPageDestination(item.forumName)
                                                )
                                            },
                                        )
                                    }
                                }
                            } else {
                                items(
                                    items = forums,
                                    key = { it.forumId }
                                ) { item ->
                                    ForumItem(
                                        item,
                                        true,
                                        onClick = {
                                            navigator.navigate(ForumPageDestination(it.forumName))
                                        },
                                        onUnfollow = {
                                            unfollowForum = it
                                            confirmUnfollowDialog.show()
                                        },
                                        onAddTopForum = {
                                            viewModel.send(HomeUiIntent.TopForums.Add(it))
                                        },
                                        onDeleteTopForum = {
                                            viewModel.send(HomeUiIntent.TopForums.Delete(it.forumId))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = ExtendedTheme.colors.pullRefreshIndicator,
                contentColor = ExtendedTheme.colors.primary,
            )
        }
    }
}

@Composable
private fun HomePageSkeletonScreen(
    forumListLayout: ForumListLayout,
    gridCells: GridCells
) {
    MyLazyVerticalGrid(
        columns = gridCells,
        contentPadding = PaddingValues(bottom = 12.dp),
        modifier = Modifier
            .fillMaxSize(),
    ) {
        item(key = "TopForumHeaderPlaceholder", span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Header(
                    text = stringResource(id = R.string.title_top_forum),
                    modifier = Modifier.placeholder(
                        visible = true,
                        color = ExtendedTheme.colors.chip
                    ),
                    invert = true
                )
            }
        }
        items(6, key = { "TopPlaceholder$it" }) {
            if (forumListLayout.isCompact) {
                ForumLabel(title = "", modifier = Modifier.padding(4.dp))
            } else {
                ForumItemPlaceholder(showAvatar = true)
            }
        }
        item(
            key = "Spacer",
            span = { GridItemSpan(maxLineSpan) }) {
            Spacer(
                modifier = Modifier.height(
                    16.dp
                )
            )
        }
        item(key = "ForumHeaderPlaceholder", span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Header(
                    text = stringResource(id = R.string.forum_list_title),
                    modifier = Modifier.placeholder(
                        visible = true,
                        color = ExtendedTheme.colors.chip
                    ),
                    invert = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        if (forumListLayout.isCompact) {
            val placeholderRows = List(12) { it }.chunked(forumListLayout.columns)
            itemsIndexed(
                items = placeholderRows,
                key = { _, row -> "PlaceholderRow${row.first()}" },
                span = { _, _ -> GridItemSpan(maxLineSpan) },
            ) { rowIndex, row ->
                CompactForumRow(
                    items = row,
                    columns = forumListLayout.columns,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(
                            top = if (rowIndex == 0) 0.dp else 4.dp,
                            bottom = if (rowIndex == placeholderRows.lastIndex) 12.dp else 4.dp,
                        ),
                ) { _, itemModifier ->
                    ForumLabel(title = "", modifier = itemModifier)
                }
            }
        } else {
            items(12, key = { "Placeholder$it" }) {
                ForumItemPlaceholder(showAvatar = true)
            }
        }
    }
}

@Composable
fun EmptyScreen(
    loggedIn: Boolean,
    canOpenExplore: Boolean,
    onOpenExplore: () -> Unit
) {
    val navigator = LocalNavigator.current
    TipScreen(
        title = {
            if (!loggedIn) {
                Text(text = stringResource(id = R.string.title_empty_login))
            } else {
                Text(text = stringResource(id = R.string.title_empty))
            }
        },
        image = {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_astronaut))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )
        },
        message = {
            if (!loggedIn) {
                Text(
                    text = stringResource(id = R.string.home_empty_login),
                    style = MaterialTheme.typography.body1,
                    color = ExtendedTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        actions = {
            if (!loggedIn) {
                Button(
                    onClick = {
                        navigator.navigate(LoginPageDestination)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.button_login))
                }
            }
            if (canOpenExplore) {
                TextButton(
                    onClick = onOpenExplore,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.button_go_to_explore))
                }
            }
        },
    )
}
