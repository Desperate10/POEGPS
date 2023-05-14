package com.poe.poegps.core.network

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.poe.poegps.feature.data.remote.api.ObjectsApi
import com.poe.poegps.feature.data.remote.utils.AuthInterceptor
import com.poe.poegps.feature.data.remote.utils.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "data_store")

    @Singleton
    @Provides
    fun provideTokenManager(@ApplicationContext context: Context) : TokenManager {
        return TokenManager(context)
    }

    @Provides
    @Singleton
    fun provideHttpClient(authInterceptor: AuthInterceptor) : OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            this.level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient().newBuilder()
            .retryOnConnectionFailure(true)
            // .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Singleton
    @Provides
    fun provideAuthInterceptor(tokenManager: TokenManager) : AuthInterceptor {
        return AuthInterceptor(tokenManager)
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient) : Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://task.poe.pl.ua:9090/gis/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}