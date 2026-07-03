package com.voxly.app.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Language : Screen("language")
    object Gender : Screen("gender")
    object Avatar : Screen("avatar")
    object ProfileSetup : Screen("profile_setup")
    object Home : Screen("home")
    object EditAvatar : Screen("edit_avatar")
    object EditProfile : Screen("edit_profile")
    object EditLanguage : Screen("edit_language")
    object PaymentDetails : Screen("payment_details")
    object Earnings : Screen("earnings")
    object Wallet : Screen("wallet")
    object Transactions : Screen("transactions")
    object AccountSettings : Screen("account_settings")
    object Permission : Screen("permission")
    object BecomeListener : Screen("become_listener")
    object ListenerForm : Screen("listener_form")
}
