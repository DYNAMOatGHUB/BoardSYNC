package com.edge.smartboard;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class EdgeSmartboardApp_MembersInjector implements MembersInjector<EdgeSmartboardApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public EdgeSmartboardApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<EdgeSmartboardApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new EdgeSmartboardApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(EdgeSmartboardApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.edge.smartboard.EdgeSmartboardApp.workerFactory")
  public static void injectWorkerFactory(EdgeSmartboardApp instance,
      HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
