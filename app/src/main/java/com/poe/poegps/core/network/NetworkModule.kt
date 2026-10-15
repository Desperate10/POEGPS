package com.poe.poegps.core.network

import com.poe.poegps.BuildConfig
import com.poe.poegps.feature.data.remote.utils.AuthInterceptor
import com.poe.poegps.feature.data.remote.utils.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(authInterceptor: AuthInterceptor) : OkHttpClient {
        val builder = OkHttpClient().newBuilder()
            .retryOnConnectionFailure(true)
            // .addInterceptor(authInterceptor)
        // Bodies and headers carry the password, the JWT and the Authorization token, so release
        // builds log nothing and debug builds only the request line and status.
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
                redactHeader("Authorization")
            })
        }
        return builder.build()
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
            .baseUrl("http://194.44.39.19:9090/gis/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}