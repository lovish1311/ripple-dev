package app.ripple.mesh.di

import android.content.Context
import app.ripple.mesh.data.MessageDao
import app.ripple.mesh.data.PeerDao
import app.ripple.mesh.data.RelayDao
import app.ripple.mesh.data.RippleDatabase
import app.ripple.mesh.data.SosBeaconDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRippleDatabase(@ApplicationContext context: Context): RippleDatabase {
        return RippleDatabase.get(context)
    }

    @Provides
    fun provideMessageDao(db: RippleDatabase): MessageDao = db.messages()

    @Provides
    fun providePeerDao(db: RippleDatabase): PeerDao = db.peers()

    @Provides
    fun provideRelayDao(db: RippleDatabase): RelayDao = db.relay()

    @Provides
    fun provideSosBeaconDao(db: RippleDatabase): SosBeaconDao = db.sos()
}
