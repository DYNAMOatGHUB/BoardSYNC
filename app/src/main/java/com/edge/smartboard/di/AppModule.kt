package com.edge.smartboard.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.work.WorkManager
import com.edge.smartboard.BuildConfig
import com.edge.smartboard.api.EdgeApiService
import com.edge.smartboard.database.EdgeDatabase
import com.edge.smartboard.database.SessionDao
import com.edge.smartboard.repository.EdgeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "edge_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Dynamic base-URL interceptor: reads the saved server URL from DataStore
        // so every request uses the URL the user configured in Settings.
        val dynamicUrlInterceptor = okhttp3.Interceptor { chain ->
            val originalRequest: Request = chain.request()
            val savedUrl: String? = runBlocking {
                context.dataStore.data.firstOrNull()
                    ?.get(EdgeRepository.KEY_SERVER)
            }
            val baseUrl = (savedUrl?.takeIf { it.isNotBlank() }
                ?: BuildConfig.BASE_URL).trimEnd('/') + "/"

            val newUrl = baseUrl.toHttpUrlOrNull()?.let { base ->
                originalRequest.url.newBuilder()
                    .scheme(base.scheme)
                    .host(base.host)
                    .port(base.port)
                    .build()
            } ?: originalRequest.url

            chain.proceed(originalRequest.newBuilder().url(newUrl).build())
        }

        return OkHttpClient.Builder()
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        // Use a placeholder base URL; real URL is injected per-request by the interceptor.
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): EdgeApiService =
        retrofit.create(EdgeApiService::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): EdgeDatabase =
        Room.databaseBuilder(context, EdgeDatabase::class.java, "edge_smartboard_db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideSessionDao(database: EdgeDatabase): SessionDao = database.sessionDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.dataStore

    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
