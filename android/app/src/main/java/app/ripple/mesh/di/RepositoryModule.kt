package app.ripple.mesh.di

import app.ripple.mesh.data.repository.MeshRepository
import app.ripple.mesh.data.repository.MeshRepositoryImpl
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
    abstract fun bindMeshRepository(
        meshRepositoryImpl: MeshRepositoryImpl
    ): MeshRepository
}
