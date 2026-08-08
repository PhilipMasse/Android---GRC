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
import fr.berrelesalpes.grc.ui.demarches.DemarcheDetailScreen
import fr.berrelesalpes.grc.ui.demarches.DemarcheDetailViewModel
import fr.berrelesalpes.grc.ui.demarches.DemarcheFormScreen
import fr.berrelesalpes.grc.ui.demarches.DemarcheFormViewModel
import fr.berrelesalpes.grc.ui.demarches.DemarcheListScreen
import fr.berrelesalpes.grc.ui.demarches.DemarcheListViewModel
import fr.berrelesalpes.grc.ui.demarches.DemarcheTypeSelectScreen
import fr.berrelesalpes.grc.ui.demarches.DemarcheTypeSelectViewModel
import fr.berrelesalpes.grc.ui.demandes.DemandeDetailScreen
import fr.berrelesalpes.grc.ui.demandes.DemandeDetailViewModel
import fr.berrelesalpes.grc.ui.demandes.DemandeFormScreen
import fr.berrelesalpes.grc.ui.demandes.DemandeFormViewModel
import fr.berrelesalpes.grc.ui.demandes.DemandeListScreen
import fr.berrelesalpes.grc.ui.demandes.DemandeListViewModel
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

    const val DEMARCHE_LIST = "demarche_list"
    const val DEMARCHE_TYPE_SELECT = "demarche_type_select"
    const val DEMARCHE_FORM = "demarche_form/{typeSlug}"
    const val DEMARCHE_DETAIL = "demarche_detail/{id}"

    const val DEMANDE_LIST = "demande_list"
    const val DEMANDE_FORM = "demande_form"
    const val DEMANDE_DETAIL = "demande_detail/{id}"

    fun resetPassword(token: String) = "reset_password/${URLEncoder.encode(token, "UTF-8")}"
    fun twoFactor(pendingToken: String, method: String) =
        "two_factor/${URLEncoder.encode(pendingToken, "UTF-8")}/${URLEncoder.encode(method, "UTF-8")}"
    fun demarcheForm(typeSlug: String) = "demarche_form/${URLEncoder.encode(typeSlug, "UTF-8")}"
    fun demarcheDetail(id: Int) = "demarche_detail/$id"
    fun demandeDetail(id: Int) = "demande_detail/$id"
}

@Composable
fun GrcNavGraph(application: GrcApplication, navController: NavHostController = rememberNavController()) {
    val repository = application.authRepository
    val demarcheRepository = application.demarcheRepository
    val demandeRepository = application.demandeRepository
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
                onOpenDemarches = { navController.navigate(GrcDestinations.DEMARCHE_LIST) },
                onOpenDemandes = { navController.navigate(GrcDestinations.DEMANDE_LIST) },
            )
        }

        composable(GrcDestinations.DEMANDE_LIST) {
            val vm: DemandeListViewModel = viewModel(factory = SimpleViewModelFactory { DemandeListViewModel(demandeRepository) })
            DemandeListScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenDemande = { id -> navController.navigate(GrcDestinations.demandeDetail(id)) },
                onNewDemande = { navController.navigate(GrcDestinations.DEMANDE_FORM) },
            )
        }

        composable(GrcDestinations.DEMANDE_FORM) {
            val vm: DemandeFormViewModel = viewModel(factory = SimpleViewModelFactory { savedStateHandle -> DemandeFormViewModel(demandeRepository, savedStateHandle) })
            DemandeFormScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSubmitted = {
                    navController.navigate(GrcDestinations.DEMANDE_LIST) {
                        popUpTo(GrcDestinations.DEMANDE_LIST) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = GrcDestinations.DEMANDE_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val vm: DemandeDetailViewModel = viewModel(factory = SimpleViewModelFactory { DemandeDetailViewModel(demandeRepository, id) })
            DemandeDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }

        composable(GrcDestinations.DEMARCHE_LIST) {
            val vm: DemarcheListViewModel = viewModel(factory = SimpleViewModelFactory { DemarcheListViewModel(demarcheRepository) })
            DemarcheListScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenDemarche = { id -> navController.navigate(GrcDestinations.demarcheDetail(id)) },
                onNewDemarche = { navController.navigate(GrcDestinations.DEMARCHE_TYPE_SELECT) },
            )
        }

        composable(GrcDestinations.DEMARCHE_TYPE_SELECT) {
            val vm: DemarcheTypeSelectViewModel = viewModel(factory = SimpleViewModelFactory { DemarcheTypeSelectViewModel(demarcheRepository) })
            DemarcheTypeSelectScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onTypeSelected = { slug -> navController.navigate(GrcDestinations.demarcheForm(slug)) },
            )
        }

        composable(
            route = GrcDestinations.DEMARCHE_FORM,
            arguments = listOf(navArgument("typeSlug") { type = NavType.StringType })
        ) { backStackEntry ->
            val typeSlug = URLDecoder.decode(backStackEntry.arguments?.getString("typeSlug") ?: "", "UTF-8")
            val vm: DemarcheFormViewModel = viewModel(factory = SimpleViewModelFactory { DemarcheFormViewModel(demarcheRepository, typeSlug) })
            DemarcheFormScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSubmitted = {
                    navController.navigate(GrcDestinations.DEMARCHE_LIST) {
                        popUpTo(GrcDestinations.DEMARCHE_LIST) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = GrcDestinations.DEMARCHE_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val vm: DemarcheDetailViewModel = viewModel(factory = SimpleViewModelFactory { DemarcheDetailViewModel(demarcheRepository, id) })
            DemarcheDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
