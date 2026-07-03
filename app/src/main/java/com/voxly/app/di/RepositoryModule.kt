package com.voxly.app.di

import com.voxly.app.data.repository.CallRepository
import com.voxly.app.data.repository.CallRepositoryImpl
import com.voxly.app.data.repository.UserRepository
import com.voxly.app.data.repository.UserRepositoryImpl
import com.voxly.app.data.repository.WalletRepository
import com.voxly.app.data.repository.WalletRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindCallRepository(
        callRepositoryImpl: CallRepositoryImpl
    ): CallRepository

    @Binds
    @Singleton
    abstract fun bindWalletRepository(
        walletRepositoryImpl: WalletRepositoryImpl
    ): WalletRepository

    @Binds
    @Singleton
    abstract fun bindPlayBillingRepository(
        playBillingRepositoryImpl: com.voxly.app.data.repository.PlayBillingRepositoryImpl
    ): com.voxly.app.data.repository.PlayBillingRepository

    @Binds
    @Singleton
    abstract fun bindCallRoutingManager(
        callRoutingManagerImpl: com.voxly.app.data.routing.CallRoutingManagerImpl
    ): com.voxly.app.data.routing.CallRoutingManager

    @Binds
    @Singleton
    abstract fun bindCandidateSelector(
        candidateSelectorImpl: com.voxly.app.data.routing.CandidateSelectorImpl
    ): com.voxly.app.data.routing.CandidateSelector

    @Binds
    @Singleton
    abstract fun bindRoutingLogger(
        routingLoggerImpl: com.voxly.app.data.routing.RoutingLoggerImpl
    ): com.voxly.app.data.routing.RoutingLogger
}
