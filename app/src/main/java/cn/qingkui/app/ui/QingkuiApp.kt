package cn.qingkui.app.ui

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
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
import cn.qingkui.app.ui.screens.AuthScreen
import cn.qingkui.app.ui.screens.ChatScreen
import cn.qingkui.app.ui.screens.KnowledgeGraphScreen
import cn.qingkui.app.ui.screens.LearningScreen
import cn.qingkui.app.ui.screens.MistakeCaptureScreen

@Composable
fun QingkuiApp() {
    val context = LocalContext.current
    val viewModel: AppViewModel = viewModel(factory = remember(context) { AppViewModel.factory(context) })
    val uiState by viewModel.uiState.collectAsState()
    val darkTheme = isSystemInDarkTheme()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(
        enabled = uiState.authScreenOpen || uiState.drawerOpen || uiState.destination != AppDestination.Chat,
    ) {
        when {
            uiState.authScreenOpen -> viewModel.closeAuth()
            uiState.drawerOpen -> viewModel.setDrawerOpen(false)
            else -> viewModel.selectDestination(AppDestination.Chat)
        }
    }

    if (uiState.authChecking) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (!uiState.authenticated && uiState.authScreenOpen) {
        AuthScreen(
            mode = uiState.authMode,
            username = uiState.username,
            password = uiState.password,
            nickname = uiState.nickname,
            email = uiState.email,
            loading = uiState.authLoading,
            errorMessage = uiState.errorMessage,
            onModeChange = viewModel::setAuthMode,
            onUsernameChange = viewModel::updateUsername,
            onPasswordChange = viewModel::updatePassword,
            onNicknameChange = viewModel::updateNickname,
            onEmailChange = viewModel::updateEmail,
            onRequestPasswordReset = viewModel::requestPasswordReset,
            onConfirmPasswordReset = viewModel::confirmPasswordReset,
            onSubmit = viewModel::submitAuth,
            onBack = viewModel::closeAuth,
        )
        return
    }

    if (uiState.mistakeCaptureOpen) {
        MistakeCaptureScreen(
            onClose = viewModel::closeMistakeCapture,
            onSave = viewModel::saveMistakeDraft,
        )
        return
    }

    LaunchedEffect(uiState.drawerOpen) {
        if (uiState.drawerOpen) drawerState.open() else drawerState.close()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = uiState.drawerOpen,
        drawerContent = {
            SessionDrawer(
                authenticated = uiState.authenticated,
                sessions = uiState.sessions,
                searchQuery = uiState.sessionSearchQuery,
                onClose = { viewModel.setDrawerOpen(false) },
                onOpenChat = {
                    viewModel.selectDestination(AppDestination.Chat)
                    viewModel.setDrawerOpen(false)
                },
                onLogout = viewModel::logout,
                onLogin = viewModel::openAuth,
                onRestoreSession = viewModel::restoreSession,
                onDeleteSession = viewModel::deleteSession,
                onSearchQueryChange = viewModel::updateSessionSearch,
                onSearch = viewModel::searchSessions,
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
            if (!darkTheme) {
                QkSvgAsset(
                    resourceId = if (compact) R.raw.qk_background_portrait else R.raw.qk_background_landscape,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
            }
            Column(Modifier.fillMaxSize()) {
                AppTopBar(
                    selected = uiState.destination,
                    credits = uiState.credits,
                    compact = compact,
                    onMenuClick = { viewModel.setDrawerOpen(true) },
                    onSelect = viewModel::selectDestination,
                )
                if (uiState.contentLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                when (uiState.destination) {
                    AppDestination.Chat -> ChatScreen(
                        compact = compact,
                        draft = uiState.draft,
                        messages = uiState.messages,
                        credits = uiState.credits,
                        sending = uiState.sending,
                        aiAvailable = uiState.aiAvailable,
                        aiUnavailableMessage = uiState.aiUnavailableMessage,
                        authenticated = uiState.authenticated,
                        subject = uiState.currentSubject,
                        helpLevel = uiState.helpLevel,
                        qaMode = uiState.qaMode,
                        clarification = uiState.qaClarification,
                        onDraftChange = viewModel::updateDraft,
                        onHelpLevelChange = viewModel::selectHelpLevel,
                        onQaModeChange = viewModel::selectQaMode,
                        onSend = viewModel::sendMessage,
                        onStop = viewModel::stopGenerating,
                        onClarification = viewModel::selectQaClarification,
                        onDismissClarification = viewModel::dismissQaClarification,
                        onFeedback = viewModel::submitAnswerFeedback,
                        onContentError = viewModel::submitContentErrorFeedback,
                        onRetry = viewModel::retryAnswer,
                        onAttach = viewModel::openMistakeCapture,
                    )
                    AppDestination.Graph -> KnowledgeGraphScreen(
                        compact = compact,
                        nodes = uiState.graphNodes,
                        relations = uiState.graphRelations,
                        selectedNodeId = uiState.selectedNodeId,
                        onSelectNode = viewModel::selectNode,
                        onAskNode = viewModel::askAboutNode,
                        onMarkStatus = viewModel::markNodeStatus,
                        onToggleFavorite = viewModel::toggleFavorite,
                        understandingCheck = uiState.understandingCheck,
                        understandingCheckSubmitting = uiState.understandingCheckSubmitting,
                        onStartUnderstandingCheck = viewModel::startUnderstandingCheck,
                        onSubmitUnderstandingCheck = viewModel::submitUnderstandingCheck,
                        onDismissUnderstandingCheck = viewModel::dismissUnderstandingCheck,
                        note = uiState.noteDraft,
                        onNoteChange = viewModel::updateNote,
                        onSearch = viewModel::searchGraph,
                        subject = uiState.currentSubject,
                        catalog = uiState.knowledgeCatalog,
                        selectedScope = uiState.selectedKnowledgeScope,
                        chapters = uiState.knowledgeChapters,
                        onSelectScope = viewModel::selectKnowledgeScope,
                    )
                    AppDestination.Learning -> LearningScreen(
                        compact = compact,
                        items = uiState.learningItems,
                        selectedFilter = uiState.learningFilter,
                        mistakeDrafts = uiState.mistakeDrafts,
                        mistakes = uiState.mistakes,
                        showMistakes = uiState.learningShowsMistakes,
                        mistakeLoading = uiState.mistakeLoading,
                        aiAvailable = uiState.aiAvailable,
                        weeklyReview = uiState.mistakeWeeklyReview,
                        onOpenItem = { nodeId ->
                            viewModel.selectNode(nodeId)
                            viewModel.selectDestination(AppDestination.Graph)
                        },
                        onShowLearning = viewModel::showLearningRecords,
                        onFilterChange = viewModel::selectLearningFilter,
                        onShowMistakes = viewModel::showMistakeBook,
                        onCaptureMistake = viewModel::openMistakeCapture,
                        onRefreshMistakes = viewModel::refreshMistakes,
                        onRetryDraft = viewModel::retryMistakeDraft,
                        onDeleteDraft = viewModel::deleteMistakeDraft,
                        onCancelOcr = viewModel::cancelMistakeOcr,
                        onRetryOcr = viewModel::retryMistakeOcr,
                        onDeleteMistake = viewModel::deleteMistake,
                        onConfirmOcr = viewModel::confirmMistakeOcr,
                        onAnalyzeMistake = viewModel::analyzeMistake,
                        onGeneratePractice = viewModel::generateMistakePractice,
                        onSubmitPractice = viewModel::submitMistakePractice,
                        onAskMistake = viewModel::askAboutMistake,
                    )
                    AppDestination.Account -> AccountScreen(
                        compact = compact,
                        credits = uiState.credits,
                        userName = uiState.currentUserName,
                        authenticated = uiState.authenticated,
                        onLogin = viewModel::openAuth,
                        onLogout = viewModel::logout,
                        onChangePassword = viewModel::changePassword,
                        onDeleteAccount = viewModel::deleteAccount,
                        ledger = uiState.ledger,
                        deviceSessions = uiState.deviceSessions,
                        feedbackItems = uiState.feedbackItems,
                        onRefreshAccount = viewModel::refreshAccount,
                        onRevokeDevice = viewModel::revokeDeviceSession,
                        onSubmitFeedback = viewModel::submitFeedback,
                        organizationsAvailable = uiState.organizationsAvailable,
                        schoolMemberships = uiState.schoolMemberships,
                        schoolClasses = uiState.schoolClasses,
                        classOverview = uiState.classOverview,
                        creditCampaignsAvailable = uiState.creditCampaignsAvailable,
                        creditCampaigns = uiState.creditCampaigns,
                        creditRedemptions = uiState.creditRedemptions,
                        contributionsAvailable = uiState.contributionsAvailable,
                        contributions = uiState.contributions,
                        onRefreshCommunity = viewModel::refreshCommunity,
                        onRedeemInvite = viewModel::redeemOrganizationInvite,
                        onLeaveSchool = viewModel::leaveSchool,
                        onLoadClassOverview = viewModel::loadClassOverview,
                        onRedeemCreditCode = viewModel::redeemCreditCode,
                        onSubmitContribution = viewModel::submitContribution,
                        onDeleteContribution = viewModel::deleteContribution,
                    )
                }
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
            )
        }
    }
    if (uiState.authenticated && uiState.privacyConsentRequired) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("隐私说明已更新") },
            text = {
                Text(
                    "继续使用前，请确认当前版本（${cn.qingkui.app.BuildConfig.PRIVACY_NOTICE_VERSION}）。青葵只处理完成账户、问答、错题与复习所需的数据；请勿提交真实身份、联系方式或与学习无关的信息。AI 结论可能出错，重要内容应结合教材或老师核验。本确认不替代学校或监护人的校内试点授权。"
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::acceptPrivacyConsent, enabled = !uiState.authLoading) {
                    Text(if (uiState.authLoading) "提交中" else "同意并继续")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::logout, enabled = !uiState.authLoading) { Text("退出登录") }
            },
        )
    }
}
