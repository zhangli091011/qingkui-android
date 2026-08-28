package cn.qingkui.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.qingkui.app.R
import cn.qingkui.app.ui.components.AppTopBar
import cn.qingkui.app.ui.components.QkSvgAsset
import cn.qingkui.app.ui.components.SessionDrawer
import cn.qingkui.app.ui.model.AppDestination
import cn.qingkui.app.ui.screens.AccountScreen
import cn.qingkui.app.ui.screens.ChatScreen
import cn.qingkui.app.ui.screens.KnowledgeGraphScreen
import cn.qingkui.app.ui.screens.LearningScreen
import kotlinx.coroutines.launch

@Composable
fun QingkuiApp(viewModel: AppViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.drawerOpen) {
        if (uiState.drawerOpen) drawerState.open() else drawerState.close()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = uiState.drawerOpen,
        drawerContent = {
            SessionDrawer(
                onClose = { viewModel.setDrawerOpen(false) },
                onOpenChat = {
                    viewModel.selectDestination(AppDestination.Chat)
                    viewModel.setDrawerOpen(false)
                },
            )
        },
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(androidx.compose.foundation.layout.WindowInsetsSides.Vertical)),
        ) {
            val compact = maxWidth < 600.dp
            QkSvgAsset(
                resourceId = if (compact) R.raw.qk_background_portrait else R.raw.qk_background_landscape,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            Column(Modifier.fillMaxSize()) {
                AppTopBar(
                    selected = uiState.destination,
                    credits = uiState.credits,
                    compact = compact,
                    onMenuClick = { viewModel.setDrawerOpen(true) },
                    onSelect = viewModel::selectDestination,
                )
                when (uiState.destination) {
                    AppDestination.Chat -> ChatScreen(
                        compact = compact,
                        draft = uiState.draft,
                        messages = uiState.messages,
                        credits = uiState.credits,
                        onDraftChange = viewModel::updateDraft,
                        onSend = viewModel::sendMessage,
                        onAttach = {
                            scope.launch {
                                snackbarHostState.showSnackbar("图片提问将在 V1.1 接入，文字问答可正常使用")
                            }
                        },
                    )
                    AppDestination.Graph -> KnowledgeGraphScreen(
                        compact = compact,
                        nodes = viewModel.graphNodes,
                        selectedNodeId = uiState.selectedNodeId,
                        onSelectNode = viewModel::selectNode,
                        onAskNode = viewModel::askAboutNode,
                    )
                    AppDestination.Learning -> LearningScreen(
                        compact = compact,
                        items = viewModel.learningItems,
                        onOpenItem = { nodeId ->
                            viewModel.selectNode(nodeId)
                            viewModel.selectDestination(AppDestination.Graph)
                        },
                    )
                    AppDestination.Account -> AccountScreen(
                        compact = compact,
                        credits = uiState.credits,
                    )
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
            )
        }
    }
}
