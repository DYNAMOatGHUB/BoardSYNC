package com.edge.smartboard.di;

import com.edge.smartboard.api.EdgeApiService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import retrofit2.Retrofit;

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
public final class AppModule_ProvideApiServiceFactory implements Factory<EdgeApiService> {
  private final Provider<Retrofit> retrofitProvider;

  public AppModule_ProvideApiServiceFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public EdgeApiService get() {
    return provideApiService(retrofitProvider.get());
  }

  public static AppModule_ProvideApiServiceFactory create(Provider<Retrofit> retrofitProvider) {
    return new AppModule_ProvideApiServiceFactory(retrofitProvider);
  }

  public static EdgeApiService provideApiService(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideApiService(retrofit));
  }
}
