package fr.berrelesalpes.grc.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.berrelesalpes.grc.GrcApplication
import fr.berrelesalpes.grc.data.local.TokenManager
import fr.berrelesalpes.grc.ui.auth.ForgotPasswordScreen
import fr.berrelesalpes.grc.ui.auth.ForgotPasswordViewModel
import fr.berrelesalpes.grc.ui.auth.LoginScreen
import fr.berrelesalpes.grc.ui.auth.LoginViewModel
import fr.berrelesalpes.grc.ui.auth.PendingTwoFactor
import fr.berrelesalpes.grc.ui.auth.RegisterScreen
import fr.berrelesalpes.grc.ui.auth.RegisterViewModel
import fr.berrelesalpes.grc.ui.auth.ResetPasswordScreen
import fr.berrelesalpes.grc.ui.auth.ResetPasswordViewModel
import fr.berrelesalpes.grc.ui.auth.TwoFactorScreen
import fr.berrelesalpes.grc.ui.auth.TwoFactorViewModel
import fr.berrelesalpes.grc.ui.common.SimpleViewModelFactory
import fr.berrelesalpes.grc.ui.home.HomeScreen
import fr.berrelesalpes.grc.ui.home.HomeViewModel
import java.net.URLDecoder
import java.net.URLEncoder

/** Noms des destinations de navigation, centralisés pour éviter les fautes de frappe. */
object GrcDestinations {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val RESET_PASSWORD = "reset_password/{token}"
    const val TWO_FACTOR = "two_factor/{pendingToken}/{method}"
    const val HOME = "home"

    fun resetPassword(token: String) = "reset_password/${URLEncoder.encode(token, "UTF-8")}"
    fun twoFactor(pendingToken: String, method: String) =
        "two_factor/${URLEncoder.encode(pendingToken, "UTF-8")}/${URLEncoder.encode(method, "UTF-8")}"
}

@Composable
fun GrcNavGraph(application: GrcApplication, navController: NavHostController = rememberNavController()) {
    val repository = application.authRepository
    val tokenManager: TokenManager = application.tokenManager

    val startDestination = if (tokenManager.isLoggedIn.value) GrcDestinations.HOME else GrcDestinations.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {

        composable(GrcDestinations.LOGIN) {
            val vm: LoginViewModel = viewModel(factory = SimpleViewModelFactory { LoginViewModel(repository) })
            LoginScreen(
                viewModel = vm,
                onLoginSuccess = {
                    navController.navigate(GrcDestinations.HOME) {
                        popUpTo(GrcDestinations.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(GrcDestinations.REGISTER) },
                onNavigateToForgotPassword = { navController.navigate(GrcDestinations.FORGOT_PASSWORD) },
                onRequiresTwoFactor = { pending: PendingTwoFactor ->
                    navController.navigate(GrcDestinations.twoFactor(pending.pendingToken, pending.method))
                },
            )
        }

        composable(GrcDestinations.REGISTER) {
            val vm: RegisterViewModel = viewModel(factory = SimpleViewModelFactory { RegisterViewModel(repository) })
            RegisterScreen(
                viewModel = vm,
                onRegistered = {
                    navController.navigate(GrcDestinations.HOME) {
                        popUpTo(GrcDestinations.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(GrcDestinations.FORGOT_PASSWORD) {
            val vm: ForgotPasswordViewModel = viewModel(factory = SimpleViewModelFactory { ForgotPasswordViewModel(repository) })
            ForgotPasswordScreen(
                viewModel = vm,
                onBackToLogin = { navController.popBackStack() },
            )
        }

        composable(
            route = GrcDestinations.RESET_PASSWORD,
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedToken = backStackEntry.arguments?.getString("token") ?: ""
            val token = URLDecoder.decode(encodedToken, "UTF-8")
            val vm: ResetPasswordViewModel = viewModel(factory = SimpleViewModelFactory { ResetPasswordViewModel(repository, token) })
            ResetPasswordScreen(
                viewModel = vm,
                onDone = {
                    navController.navigate(GrcDestinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = GrcDestinations.TWO_FACTOR,
            arguments = listOf(
                navArgument("pendingToken") { type = NavType.StringType },
                navArgument("method") { type = NavType.StringType },
            )
        ) { backStackEntry ->
            val pendingToken = URLDecoder.decode(backStackEntry.arguments?.getString("pendingToken") ?: "", "UTF-8")
            val method = URLDecoder.decode(backStackEntry.arguments?.getString("method") ?: "email", "UTF-8")
            val vm: TwoFactorViewModel = viewModel(factory = SimpleViewModelFactory { TwoFactorViewModel(repository, pendingToken, method) })
            TwoFactorScreen(
                viewModel = vm,
                onVerified = {
                    navController.navigate(GrcDestinations.HOME) {
                        popUpTo(GrcDestinations.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(GrcDestinations.HOME) {
            val vm: HomeViewModel = viewModel(factory = SimpleViewModelFactory { HomeViewModel(repository) })
            HomeScreen(
                viewModel = vm,
                onLoggedOut = {
                    navController.navigate(GrcDestinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
