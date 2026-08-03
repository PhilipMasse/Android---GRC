package fr.berrelesalpes.grc.data.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import fr.berrelesalpes.grc.BuildConfig
import fr.berrelesalpes.grc.data.local.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Point de construction unique du client réseau. Instancié une seule fois
 * (voir GrcApplication) et réutilisé pour tous les repositories.
 */
object RetrofitClient {

    fun create(tokenManager: TokenManager): GrcApiService {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Ne journalise le corps des requêtes qu'en debug — jamais en
            // production, pour ne pas exposer de données personnelles dans
            // les logs de l'appareil.
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .authenticator(TokenAuthenticator(tokenManager, BuildConfig.API_BASE_URL))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        return retrofit.create(GrcApiService::class.java)
    }
}
