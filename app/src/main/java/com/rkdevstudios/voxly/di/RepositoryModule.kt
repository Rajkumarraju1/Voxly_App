package com.rkdevstudios.voxly.di

import com.rkdevstudios.voxly.data.repository.CallRepository
import com.rkdevstudios.voxly.data.repository.CallRepositoryImpl
import com.rkdevstudios.voxly.data.repository.UserRepository
import com.rkdevstudios.voxly.data.repository.UserRepositoryImpl
import com.rkdevstudios.voxly.data.repository.WalletRepository
import com.rkdevstudios.voxly.data.repository.WalletRepositoryImpl
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
        playBillingRepositoryImpl: com.rkdevstudios.voxly.data.repository.PlayBillingRepositoryImpl
    ): com.rkdevstudios.voxly.data.repository.PlayBillingRepository

    @Binds
    @Singleton
    abstract fun bindCallRoutingManager(
        callRoutingManagerImpl: com.rkdevstudios.voxly.data.routing.CallRoutingManagerImpl
    ): com.rkdevstudios.voxly.data.routing.CallRoutingManager

    @Binds
    @Singleton
    abstract fun bindCandidateSelector(
        candidateSelectorImpl: com.rkdevstudios.voxly.data.routing.CandidateSelectorImpl
    ): com.rkdevstudios.voxly.data.routing.CandidateSelector

    @Binds
    @Singleton
    abstract fun bindRoutingLogger(
        routingLoggerImpl: com.rkdevstudios.voxly.data.routing.RoutingLoggerImpl
    ): com.rkdevstudios.voxly.data.routing.RoutingLogger

    @Binds
    @Singleton
    abstract fun bindCallSessionManager(
        callSessionManagerImpl: com.rkdevstudios.voxly.data.session.CallSessionManagerImpl
    ): com.rkdevstudios.voxly.data.session.CallSessionManager

    @Binds
    @Singleton
    abstract fun bindBillingManager(
        billingManagerImpl: com.rkdevstudios.voxly.data.session.BillingManagerImpl
    ): com.rkdevstudios.voxly.data.session.BillingManager
}
