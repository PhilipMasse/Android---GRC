package fr.berrelesalpes.grc

import android.app.Application
import fr.berrelesalpes.grc.data.local.TokenManager
import fr.berrelesalpes.grc.data.network.GrcApiService
import fr.berrelesalpes.grc.data.network.RetrofitClient
import fr.berrelesalpes.grc.data.repository.AuthRepository
import fr.berrelesalpes.grc.data.repository.DemarcheRepository

/**
 * Conteneur de dépendances "à la main", sans framework d'injection (Hilt,
 * Koin...) — volontairement simple pour ce premier lot. Si l'application
 * grossit significativement, migrer vers Hilt sera pertinent.
 */
class GrcApplication : Application() {

    lateinit var tokenManager: TokenManager
        private set
    lateinit var apiService: GrcApiService
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var demarcheRepository: DemarcheRepository
        private set

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        apiService = RetrofitClient.create(tokenManager)
        authRepository = AuthRepository(apiService, tokenManager)
        demarcheRepository = DemarcheRepository(apiService)
    }
}
