package com.edge.smartboard.worker;

import android.content.Context;
import androidx.work.WorkerParameters;
import dagger.internal.DaggerGenerated;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class UploadWorker_Factory {
  public UploadWorker_Factory() {
  }

  public UploadWorker get(Context context, WorkerParameters workerParams) {
    return newInstance(context, workerParams);
  }

  public static UploadWorker_Factory create() {
    return new UploadWorker_Factory();
  }

  public static UploadWorker newInstance(Context context, WorkerParameters workerParams) {
    return new UploadWorker(context, workerParams);
  }
}
