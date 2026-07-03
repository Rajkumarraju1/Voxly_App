package com.voxly.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voxly.app.ui.auth.LoginScreen
import com.voxly.app.ui.home.HomeScreen
import com.voxly.app.ui.onboarding.AvatarScreen
import com.voxly.app.ui.onboarding.GenderScreen
import com.voxly.app.ui.onboarding.LanguageScreen
import com.voxly.app.ui.profile.ProfileSetupScreen
import com.voxly.app.ui.splash.SplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import com.voxly.app.ui.call.CallOverlay
import com.voxly.app.ui.viewmodel.HomeViewModel
import com.voxly.app.ui.viewmodel.CallState

@Composable
fun VoxlyNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    val onboardingViewModel: com.voxly.app.ui.viewmodel.OnboardingViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val homeViewModel: HomeViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    val context = LocalContext.current

    val currentUser by homeViewModel.currentUser.collectAsState()

    // Removed global listener to prevent startup race condition.
    // relying on explicit callbacks from UI for now.

    Surface(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        color = androidx.compose.material3.MaterialTheme.colorScheme.background
    ) {
        // Observe Call Navigation (Caller Side)
        androidx.compose.runtime.LaunchedEffect(homeViewModel) {
            homeViewModel.navigateToActiveCall.collect { call ->
                val intent = android.content.Intent(context, com.voxly.app.ui.call.ActiveCallActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("CALL_ID", call.id)
                    // For Caller mode: The "Caller" displayed is the Speaker.
                    // We swap roles in the intent so ActiveCallActivity displays the "Other Person".
                    putExtra("CALLER_ID", call.speakerId)
                    putExtra("CALLER_NAME", call.speakerName)
                    putExtra("CALLER_AVATAR", call.speakerAvatar)
                    putExtra("RATE", call.rate)
                    putExtra("TYPE", call.type)
                }
                context.startActivity(intent)
            }
        }

        Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                     onNavigateToLogin = {
                         navController.navigate(Screen.Login.route) {
                             popUpTo(Screen.Splash.route) { inclusive = true }
                         }
                     },
                     onNavigateToHome = {
                         navController.navigate(Screen.Home.route) {
                             popUpTo(Screen.Splash.route) { inclusive = true }
                         }
                     }
                )
            }
    
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { isExistingUser ->
                        if (isExistingUser) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.Language.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    }
                )
            }
    
            composable(Screen.Language.route) {
                LanguageScreen(
                    onContinue = { language ->
                        onboardingViewModel.updateLanguage(language)
                        navController.navigate(Screen.Gender.route)
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
    
            composable(Screen.Gender.route) {
                GenderScreen(
                    onContinue = { gender ->
                        onboardingViewModel.updateGender(gender)
                        navController.navigate("${Screen.Avatar.route}/$gender")
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
    
            composable(
                route = "${Screen.Avatar.route}/{gender}",
                arguments = listOf(navArgument("gender") { type = NavType.StringType })
            ) { backStackEntry ->
                val gender = backStackEntry.arguments?.getString("gender") ?: "Male"
                AvatarScreen(
                    gender = gender,
                    onContinue = { avatar ->
                        onboardingViewModel.updateAvatar(avatar)
                        navController.navigate(Screen.ProfileSetup.route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
    
            composable(Screen.ProfileSetup.route) {
                ProfileSetupScreen(
                    onSetupComplete = { name, tags ->
                        onboardingViewModel.updateProfile(name, tags)
                        onboardingViewModel.saveUser {
                             navController.navigate(Screen.Permission.route) {
                                 popUpTo(Screen.Language.route) { inclusive = true } // Clear onboarding stack
                             }
                        }
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }
    
            composable(Screen.Permission.route) {
                com.voxly.app.ui.onboarding.PermissionScreen(
                    onContinue = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Permission.route) { inclusive = true }
                        }
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Home.route) {
                // Shared ViewModel passed provided by VoxlyNavGraph scope
                com.voxly.app.ui.main.MainScreen(
                    viewModel = homeViewModel,
                    onEditAvatar = { gender ->
                        navController.navigate("${Screen.EditAvatar.route}/$gender")
                    },
                    onEditLanguage = { currentLang ->
                        navController.navigate("${Screen.EditLanguage.route}/$currentLang")
                    },
                    onEditTags = { currentAvatar ->
                        val encoded = java.net.URLEncoder.encode(currentAvatar, "UTF-8")
                        navController.navigate("${Screen.EditProfile.route}/$encoded")
                    },
                    onEditPayment = {
                        navController.navigate(Screen.PaymentDetails.route)
                    },
                    onEarningsClick = {
                        navController.navigate(Screen.Earnings.route)
                    },
                    onWalletClick = {
                        navController.navigate(Screen.Wallet.route)
                    },
                    onTransactionsClick = {
                        navController.navigate(Screen.Transactions.route)
                    },
                    onAccountSettingsClick = {
                        navController.navigate(Screen.AccountSettings.route)
                    },
                    onSwitchToListener = {
                        navController.navigate(Screen.BecomeListener.route)
                    },
                    onLogout = {
                        homeViewModel.logout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
    
            composable(Screen.Transactions.route) {
                com.voxly.app.ui.wallet.TransactionsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
    
            composable(
                route = "${Screen.EditAvatar.route}/{gender}",
                arguments = listOf(navArgument("gender") { type = NavType.StringType })
            ) { backStackEntry ->
                val gender = backStackEntry.arguments?.getString("gender") ?: "Male"
                
                AvatarScreen(
                    gender = gender,
                    onContinue = { avatar ->
                        val encodedAvatar = java.net.URLEncoder.encode(avatar, "UTF-8")
                        navController.navigate("${Screen.EditProfile.route}/$encodedAvatar")
                    },
                    onBack = { navController.popBackStack() },
                    isOnboarding = false
                )
            }
    
            composable(
                route = "${Screen.EditProfile.route}/{avatar}",
                arguments = listOf(navArgument("avatar") { type = NavType.StringType })
            ) { backStackEntry ->
                val avatar = backStackEntry.arguments?.getString("avatar") ?: ""
                val user by homeViewModel.currentUser.collectAsState()
    
                user?.let { currentUser ->
                    com.voxly.app.ui.profile.EditProfileScreen(
                        currentName = currentUser.displayName,
                        currentTagline = currentUser.tagline,
                        currentTags = currentUser.tags,
                        currentAvatarUrl = avatar.ifEmpty { currentUser.avatarUrl },
                        gender = currentUser.gender,
                        onUpdateProfile = { name, tagline, tags ->
                             homeViewModel.updateFullProfile(name, tagline, tags, avatar)
                             // Pop back to Main Screen (which is "home")
                             navController.popBackStack(Screen.Home.route, inclusive = false)
                        },
                        onBackClick = { navController.popBackStack() },
                        onEditAvatarClick = { gender ->
                            navController.navigate("${Screen.EditAvatar.route}/$gender")
                        }
                    )
                }
            }

            composable(
                route = "${Screen.EditLanguage.route}/{currentLanguage}",
                arguments = listOf(navArgument("currentLanguage") { type = NavType.StringType })
            ) { backStackEntry ->
                val currentLanguage = backStackEntry.arguments?.getString("currentLanguage") ?: "English"
                
                LanguageScreen(
                    initialSelection = currentLanguage,
                    onContinue = { language ->
                        homeViewModel.updateUserLanguage(language)
                        navController.popBackStack()
                    },
                    onBackClick = { navController.popBackStack() },
                    isOnboarding = false
                )
            }

            composable(Screen.PaymentDetails.route) {
                val user by homeViewModel.currentUser.collectAsState()
                
                user?.let { currentUser ->
                    com.voxly.app.ui.payment.PaymentDetailsScreen(
                        user = currentUser,
                        onBack = { navController.popBackStack() },
                        onSave = { updatedUser ->
                            homeViewModel.updatePaymentDetails(updatedUser)
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable(Screen.Earnings.route) {
                val historyViewModel: com.voxly.app.ui.viewmodel.HistoryViewModel = androidx.hilt.navigation.compose.hiltViewModel() // Added
                val user by homeViewModel.currentUser.collectAsState()
                val transactions by homeViewModel.transactions.collectAsState()
                val isTransactionLoading by homeViewModel.isTransactionLoading.collectAsState()
                val history by historyViewModel.history.collectAsState() // Added
                val isHistoryLoading by historyViewModel.isLoading.collectAsState() // Added
                
                user?.let { currentUser ->
                    com.voxly.app.ui.speaker.EarningsScreen(
                        user = currentUser,
                        history = history, // Pass real history
                        transactions = transactions,
                        isTransactionLoading = isTransactionLoading,
                        onLoadMoreTransactions = { homeViewModel.loadMoreTransactions() },
                        isHistoryLoading = isHistoryLoading, // Pass state
                        onLoadMoreHistory = { historyViewModel.loadMore() }, // Pass callback
                        onWithdraw = { amount, method, details ->
                            homeViewModel.submitWithdrawal(amount, method, details)
                        },
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            composable(Screen.Wallet.route) {
                com.voxly.app.ui.wallet.WalletScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.Transactions.route) {
                com.voxly.app.ui.wallet.TransactionsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.AccountSettings.route) {
                val user by homeViewModel.currentUser.collectAsState()
                
                user?.let { currentUser ->
                    com.voxly.app.ui.profile.AccountSettingsScreen(
                        user = currentUser,
                        onBack = { navController.popBackStack() },
                        onLogout = {
                            homeViewModel.logout()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onDeleteAccount = {
                             homeViewModel.deleteAccount()
                             navController.navigate(Screen.Login.route) {
                                 popUpTo(0) { inclusive = true }
                             }
                        }
                    )
                }
            }

            composable(Screen.BecomeListener.route) {
                val user by homeViewModel.currentUser.collectAsState()
                
                // Default to "none" if user is null (shouldn't happen here usually)
                val status = user?.verificationStatus ?: "none"

                com.voxly.app.ui.profile.BecomeListenerScreen(
                    verificationStatus = status,
                    onBackClick = { navController.popBackStack() },
                    onStartEarningClick = {
                        navController.navigate(Screen.ListenerForm.route)
                    },
                    onGoToDashboard = {
                        // Clear backstack up to Home and reload it to trigger Speaker Mode
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.ListenerForm.route) {
                val user by homeViewModel.currentUser.collectAsState()
                
                user?.let { currentUser ->
                    com.voxly.app.ui.profile.ListenerFormScreen(
                        initialGender = currentUser.gender,
                        initialLanguages = currentUser.languages,
                        onBackClick = { navController.popBackStack() },
                        onSubmit = { gender, phone, languages ->
                            homeViewModel.submitListenerApplication(phone, gender, languages)
                            // Navigate back to BecomeListenerScreen to show the "Pending" status
                            // Pop exclusively up to BecomeListener? No, just pop this form off.
                            // The previous screen on stack should be BecomeListener.
                            navController.popBackStack()
                        }
                    )
                }
            }
            }

            CallOverlay(
                viewModel = homeViewModel,
                onNavigateToIncoming = { state ->
                    val intent = android.content.Intent(context, com.voxly.app.ui.call.IncomingCallActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("CALL_ID", state.callId)
                        putExtra("CALLER_ID", state.callerId)
                        putExtra("CALLER_NAME", state.callerName)
                        putExtra("CALLER_AVATAR", state.callerAvatar)
                        putExtra("RATE", state.rate)
                        putExtra("TYPE", state.type)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}
