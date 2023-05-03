package com.poe.poegps.feature.data.di

import com.poe.poegps.feature.data.remote.api.ObjectsApi
import com.poe.poegps.feature.data.repository.ObjectsRepositoryImpl
import com.poe.poegps.feature.domain.repository.ObjectsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton


@Module(includes = [MainModule.BindsModule::class])
@InstallIn(SingletonComponent::class)
object MainModule {

    @Provides
    @Singleton
    fun provideApi(
        retrofit: Retrofit
    ): ObjectsApi {
        return retrofit.create(ObjectsApi::class.java)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    interface BindsModule {
        @Binds
        @Singleton
        fun bindRepository(
            repository: ObjectsRepositoryImpl
        ): ObjectsRepository
    }
}