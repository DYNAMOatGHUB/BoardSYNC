package com.edge.smartboard.repository;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import com.edge.smartboard.api.EdgeApiService;
import com.edge.smartboard.database.SessionDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class EdgeRepository_Factory implements Factory<EdgeRepository> {
  private final Provider<EdgeApiService> apiProvider;

  private final Provider<SessionDao> sessionDaoProvider;

  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public EdgeRepository_Factory(Provider<EdgeApiService> apiProvider,
      Provider<SessionDao> sessionDaoProvider, Provider<DataStore<Preferences>> dataStoreProvider) {
    this.apiProvider = apiProvider;
    this.sessionDaoProvider = sessionDaoProvider;
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public EdgeRepository get() {
    return newInstance(apiProvider.get(), sessionDaoProvider.get(), dataStoreProvider.get());
  }

  public static EdgeRepository_Factory create(Provider<EdgeApiService> apiProvider,
      Provider<SessionDao> sessionDaoProvider, Provider<DataStore<Preferences>> dataStoreProvider) {
    return new EdgeRepository_Factory(apiProvider, sessionDaoProvider, dataStoreProvider);
  }

  public static EdgeRepository newInstance(EdgeApiService api, SessionDao sessionDao,
      DataStore<Preferences> dataStore) {
    return new EdgeRepository(api, sessionDao, dataStore);
  }
}
