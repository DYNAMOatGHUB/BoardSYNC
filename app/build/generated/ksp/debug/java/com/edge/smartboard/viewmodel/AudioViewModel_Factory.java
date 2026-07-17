package com.edge.smartboard.viewmodel;

import android.content.Context;
import com.edge.smartboard.repository.EdgeRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AudioViewModel_Factory implements Factory<AudioViewModel> {
  private final Provider<EdgeRepository> repositoryProvider;

  private final Provider<Context> appContextProvider;

  public AudioViewModel_Factory(Provider<EdgeRepository> repositoryProvider,
      Provider<Context> appContextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.appContextProvider = appContextProvider;
  }

  @Override
  public AudioViewModel get() {
    return newInstance(repositoryProvider.get(), appContextProvider.get());
  }

  public static AudioViewModel_Factory create(Provider<EdgeRepository> repositoryProvider,
      Provider<Context> appContextProvider) {
    return new AudioViewModel_Factory(repositoryProvider, appContextProvider);
  }

  public static AudioViewModel newInstance(EdgeRepository repository, Context appContext) {
    return new AudioViewModel(repository, appContext);
  }
}
