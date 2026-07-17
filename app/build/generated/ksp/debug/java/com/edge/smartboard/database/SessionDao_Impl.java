package com.edge.smartboard.database;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.edge.smartboard.models.Session;
import com.edge.smartboard.models.SessionStatus;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
import java.lang.IllegalArgumentException;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SessionDao_Impl implements SessionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Session> __insertionAdapterOfSession;

  private final EntityDeletionOrUpdateAdapter<Session> __deletionAdapterOfSession;

  private final EntityDeletionOrUpdateAdapter<Session> __updateAdapterOfSession;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllSessions;

  public SessionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSession = new EntityInsertionAdapter<Session>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sessions` (`sessionId`,`teacherName`,`subject`,`classRoom`,`department`,`date`,`durationSeconds`,`imageCount`,`audioFile`,`storageBytes`,`status`,`score`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Session entity) {
        statement.bindString(1, entity.getSessionId());
        statement.bindString(2, entity.getTeacherName());
        statement.bindString(3, entity.getSubject());
        statement.bindString(4, entity.getClassRoom());
        statement.bindString(5, entity.getDepartment());
        statement.bindLong(6, entity.getDate());
        statement.bindLong(7, entity.getDurationSeconds());
        statement.bindLong(8, entity.getImageCount());
        if (entity.getAudioFile() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getAudioFile());
        }
        statement.bindLong(10, entity.getStorageBytes());
        statement.bindString(11, __SessionStatus_enumToString(entity.getStatus()));
        if (entity.getScore() == null) {
          statement.bindNull(12);
        } else {
          statement.bindDouble(12, entity.getScore());
        }
      }
    };
    this.__deletionAdapterOfSession = new EntityDeletionOrUpdateAdapter<Session>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `sessions` WHERE `sessionId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Session entity) {
        statement.bindString(1, entity.getSessionId());
      }
    };
    this.__updateAdapterOfSession = new EntityDeletionOrUpdateAdapter<Session>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `sessions` SET `sessionId` = ?,`teacherName` = ?,`subject` = ?,`classRoom` = ?,`department` = ?,`date` = ?,`durationSeconds` = ?,`imageCount` = ?,`audioFile` = ?,`storageBytes` = ?,`status` = ?,`score` = ? WHERE `sessionId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Session entity) {
        statement.bindString(1, entity.getSessionId());
        statement.bindString(2, entity.getTeacherName());
        statement.bindString(3, entity.getSubject());
        statement.bindString(4, entity.getClassRoom());
        statement.bindString(5, entity.getDepartment());
        statement.bindLong(6, entity.getDate());
        statement.bindLong(7, entity.getDurationSeconds());
        statement.bindLong(8, entity.getImageCount());
        if (entity.getAudioFile() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getAudioFile());
        }
        statement.bindLong(10, entity.getStorageBytes());
        statement.bindString(11, __SessionStatus_enumToString(entity.getStatus()));
        if (entity.getScore() == null) {
          statement.bindNull(12);
        } else {
          statement.bindDouble(12, entity.getScore());
        }
        statement.bindString(13, entity.getSessionId());
      }
    };
    this.__preparedStmtOfDeleteAllSessions = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sessions";
        return _query;
      }
    };
  }

  @Override
  public Object insertSession(final Session session, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSession.insert(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSession(final Session session, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSession.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSession(final Session session, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSession.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllSessions(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllSessions.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllSessions.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Session>> getAllSessions() {
    final String _sql = "SELECT * FROM sessions ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sessions"}, new Callable<List<Session>>() {
      @Override
      @NonNull
      public List<Session> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfTeacherName = CursorUtil.getColumnIndexOrThrow(_cursor, "teacherName");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfClassRoom = CursorUtil.getColumnIndexOrThrow(_cursor, "classRoom");
          final int _cursorIndexOfDepartment = CursorUtil.getColumnIndexOrThrow(_cursor, "department");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfImageCount = CursorUtil.getColumnIndexOrThrow(_cursor, "imageCount");
          final int _cursorIndexOfAudioFile = CursorUtil.getColumnIndexOrThrow(_cursor, "audioFile");
          final int _cursorIndexOfStorageBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "storageBytes");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final List<Session> _result = new ArrayList<Session>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Session _item;
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpTeacherName;
            _tmpTeacherName = _cursor.getString(_cursorIndexOfTeacherName);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpClassRoom;
            _tmpClassRoom = _cursor.getString(_cursorIndexOfClassRoom);
            final String _tmpDepartment;
            _tmpDepartment = _cursor.getString(_cursorIndexOfDepartment);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final long _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getLong(_cursorIndexOfDurationSeconds);
            final int _tmpImageCount;
            _tmpImageCount = _cursor.getInt(_cursorIndexOfImageCount);
            final String _tmpAudioFile;
            if (_cursor.isNull(_cursorIndexOfAudioFile)) {
              _tmpAudioFile = null;
            } else {
              _tmpAudioFile = _cursor.getString(_cursorIndexOfAudioFile);
            }
            final long _tmpStorageBytes;
            _tmpStorageBytes = _cursor.getLong(_cursorIndexOfStorageBytes);
            final SessionStatus _tmpStatus;
            _tmpStatus = __SessionStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
            final Float _tmpScore;
            if (_cursor.isNull(_cursorIndexOfScore)) {
              _tmpScore = null;
            } else {
              _tmpScore = _cursor.getFloat(_cursorIndexOfScore);
            }
            _item = new Session(_tmpSessionId,_tmpTeacherName,_tmpSubject,_tmpClassRoom,_tmpDepartment,_tmpDate,_tmpDurationSeconds,_tmpImageCount,_tmpAudioFile,_tmpStorageBytes,_tmpStatus,_tmpScore);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getSession(final String id, final Continuation<? super Session> $completion) {
    final String _sql = "SELECT * FROM sessions WHERE sessionId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Session>() {
      @Override
      @Nullable
      public Session call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfTeacherName = CursorUtil.getColumnIndexOrThrow(_cursor, "teacherName");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfClassRoom = CursorUtil.getColumnIndexOrThrow(_cursor, "classRoom");
          final int _cursorIndexOfDepartment = CursorUtil.getColumnIndexOrThrow(_cursor, "department");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfImageCount = CursorUtil.getColumnIndexOrThrow(_cursor, "imageCount");
          final int _cursorIndexOfAudioFile = CursorUtil.getColumnIndexOrThrow(_cursor, "audioFile");
          final int _cursorIndexOfStorageBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "storageBytes");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final Session _result;
          if (_cursor.moveToFirst()) {
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpTeacherName;
            _tmpTeacherName = _cursor.getString(_cursorIndexOfTeacherName);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpClassRoom;
            _tmpClassRoom = _cursor.getString(_cursorIndexOfClassRoom);
            final String _tmpDepartment;
            _tmpDepartment = _cursor.getString(_cursorIndexOfDepartment);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final long _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getLong(_cursorIndexOfDurationSeconds);
            final int _tmpImageCount;
            _tmpImageCount = _cursor.getInt(_cursorIndexOfImageCount);
            final String _tmpAudioFile;
            if (_cursor.isNull(_cursorIndexOfAudioFile)) {
              _tmpAudioFile = null;
            } else {
              _tmpAudioFile = _cursor.getString(_cursorIndexOfAudioFile);
            }
            final long _tmpStorageBytes;
            _tmpStorageBytes = _cursor.getLong(_cursorIndexOfStorageBytes);
            final SessionStatus _tmpStatus;
            _tmpStatus = __SessionStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
            final Float _tmpScore;
            if (_cursor.isNull(_cursorIndexOfScore)) {
              _tmpScore = null;
            } else {
              _tmpScore = _cursor.getFloat(_cursorIndexOfScore);
            }
            _result = new Session(_tmpSessionId,_tmpTeacherName,_tmpSubject,_tmpClassRoom,_tmpDepartment,_tmpDate,_tmpDurationSeconds,_tmpImageCount,_tmpAudioFile,_tmpStorageBytes,_tmpStatus,_tmpScore);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Session>> getSessionsByStatus(final SessionStatus status) {
    final String _sql = "SELECT * FROM sessions WHERE status = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, __SessionStatus_enumToString(status));
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sessions"}, new Callable<List<Session>>() {
      @Override
      @NonNull
      public List<Session> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSessionId = CursorUtil.getColumnIndexOrThrow(_cursor, "sessionId");
          final int _cursorIndexOfTeacherName = CursorUtil.getColumnIndexOrThrow(_cursor, "teacherName");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfClassRoom = CursorUtil.getColumnIndexOrThrow(_cursor, "classRoom");
          final int _cursorIndexOfDepartment = CursorUtil.getColumnIndexOrThrow(_cursor, "department");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfDurationSeconds = CursorUtil.getColumnIndexOrThrow(_cursor, "durationSeconds");
          final int _cursorIndexOfImageCount = CursorUtil.getColumnIndexOrThrow(_cursor, "imageCount");
          final int _cursorIndexOfAudioFile = CursorUtil.getColumnIndexOrThrow(_cursor, "audioFile");
          final int _cursorIndexOfStorageBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "storageBytes");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfScore = CursorUtil.getColumnIndexOrThrow(_cursor, "score");
          final List<Session> _result = new ArrayList<Session>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Session _item;
            final String _tmpSessionId;
            _tmpSessionId = _cursor.getString(_cursorIndexOfSessionId);
            final String _tmpTeacherName;
            _tmpTeacherName = _cursor.getString(_cursorIndexOfTeacherName);
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final String _tmpClassRoom;
            _tmpClassRoom = _cursor.getString(_cursorIndexOfClassRoom);
            final String _tmpDepartment;
            _tmpDepartment = _cursor.getString(_cursorIndexOfDepartment);
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final long _tmpDurationSeconds;
            _tmpDurationSeconds = _cursor.getLong(_cursorIndexOfDurationSeconds);
            final int _tmpImageCount;
            _tmpImageCount = _cursor.getInt(_cursorIndexOfImageCount);
            final String _tmpAudioFile;
            if (_cursor.isNull(_cursorIndexOfAudioFile)) {
              _tmpAudioFile = null;
            } else {
              _tmpAudioFile = _cursor.getString(_cursorIndexOfAudioFile);
            }
            final long _tmpStorageBytes;
            _tmpStorageBytes = _cursor.getLong(_cursorIndexOfStorageBytes);
            final SessionStatus _tmpStatus;
            _tmpStatus = __SessionStatus_stringToEnum(_cursor.getString(_cursorIndexOfStatus));
            final Float _tmpScore;
            if (_cursor.isNull(_cursorIndexOfScore)) {
              _tmpScore = null;
            } else {
              _tmpScore = _cursor.getFloat(_cursorIndexOfScore);
            }
            _item = new Session(_tmpSessionId,_tmpTeacherName,_tmpSubject,_tmpClassRoom,_tmpDepartment,_tmpDate,_tmpDurationSeconds,_tmpImageCount,_tmpAudioFile,_tmpStorageBytes,_tmpStatus,_tmpScore);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getTotalCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM sessions";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getTotalStorageBytes(final Continuation<? super Long> $completion) {
    final String _sql = "SELECT SUM(storageBytes) FROM sessions";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Long>() {
      @Override
      @Nullable
      public Long call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Long _result;
          if (_cursor.moveToFirst()) {
            final Long _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(0);
            }
            _result = _tmp;
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }

  private String __SessionStatus_enumToString(@NonNull final SessionStatus _value) {
    switch (_value) {
      case RECORDING: return "RECORDING";
      case COMPLETED: return "COMPLETED";
      case UPLOADING: return "UPLOADING";
      case PROCESSING: return "PROCESSING";
      case FAILED: return "FAILED";
      case UPLOADED: return "UPLOADED";
      default: throw new IllegalArgumentException("Can't convert enum to string, unknown enum value: " + _value);
    }
  }

  private SessionStatus __SessionStatus_stringToEnum(@NonNull final String _value) {
    switch (_value) {
      case "RECORDING": return SessionStatus.RECORDING;
      case "COMPLETED": return SessionStatus.COMPLETED;
      case "UPLOADING": return SessionStatus.UPLOADING;
      case "PROCESSING": return SessionStatus.PROCESSING;
      case "FAILED": return SessionStatus.FAILED;
      case "UPLOADED": return SessionStatus.UPLOADED;
      default: throw new IllegalArgumentException("Can't convert value to enum, unknown value: " + _value);
    }
  }
}
