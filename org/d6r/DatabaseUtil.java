package org.d6r;

import android.database.AbstractCursor;
import android.database.AbstractWindowedCursor;
import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteConnection;
import android.database.sqlite.SQLiteConnectionPool;
import android.database.sqlite.SQLiteCursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseConfiguration;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDirectCursorDriver;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQuery;
import android.database.sqlite.SQLiteSession;
import android.database.sqlite.SQLiteStatementInfo;
import android.os.CancellationSignal;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import static org.d6r.Reflect.setfldval;
import static org.d6r.Reflect.getfldval;
import static org.d6r.Reflector.invokeOrDefault;
import java.util.*;
import java.lang.reflect.*;
import org.d6r.CollectionUtil.NullKey;

public class DatabaseUtil {
  public static final String TAG = "DatabaseUtil";
  public static final String TAG_DB = "SQLiteDatabase";
  
  public static final int FIELD_TYPE_NULL = 0;
  public static final int FIELD_TYPE_INTEGER = 1;
  public static final int FIELD_TYPE_FLOAT = 2;
  public static final int FIELD_TYPE_STRING = 3;
  public static final int FIELD_TYPE_BLOB = 4;
    
  public static final int FLAG_OPEN_READONLY = 0x1;
  public static final int FLAG_NO_LOCALIZED_COLLATORS = 0x10;
  public static final int FLAG_CREATE_IF_NECESSARY = 0x10000000;
  public static final int FLAG_ENABLE_WRITE_AHEAD_LOGGING = 0x20000000;
  
  public static final int CONNECTION_FLAG_READ_ONLY = 0x1;  
  public static final int CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY = 0x2;
  public static final int CONNECTION_FLAG_INTERACTIVE = 0x4;
  
  public static boolean VERBOSE = false;
  
  public static final WeakHashMap<SQLiteDatabase, Void> sActiveDatabases
    = LazyMember.<Field>of("sActiveDatabases",SQLiteDatabase.class).getValue(null);
  
  public static class DbErrorHandler implements DatabaseErrorHandler {
    protected DatabaseErrorHandler errorHandler;    
    public DbErrorHandler(final DatabaseErrorHandler errorHandler) {
      this.errorHandler = errorHandler;
    }
    public DbErrorHandler() {
      this((DatabaseErrorHandler) null);
    }    
    @Override
    public void onCorruption(SQLiteDatabase dbObj) {
      System.err.printf(
        "[WARN] Database is corrupted: %s\n", Debug.ToString(dbObj)
      );
      if (errorHandler != null) errorHandler.onCorruption(dbObj);
    }
  }  
  
  public static class DbCursor extends SQLiteCursor {
    
    protected boolean d;
    private static String TAG 
      = StringUtils.substringAfterLast(DbCursor.class.getName(), ".");
    
    public DbCursor(final SQLiteDatabase db, final SQLiteCursorDriver driver,
    final String editTable, final SQLiteQuery query)
    {
      super(db, driver, editTable, query);
    }
    
    @Override
    protected void finalize() {
      if (d) Log.d(TAG, "DbCursor.finalize()");
      super.finalize();
    }
    
    @Override
    protected void clearOrCreateWindow(final String name) {
      if (d) Log.d(TAG, "DbCursor.clearOrCreateWindow(name: '%s')\n", name);
      super.clearOrCreateWindow(name);
    }
    
    @Override
    protected void closeWindow() {
      if (d) Log.d(TAG, "DbCursor.closeWindow()");
      super.closeWindow();
    }
    
    @Override
    public CursorWindow getWindow() {
      return super.getWindow();
    }
    
    public boolean isDebug() { return this.d; }
    public DbCursor setDebug(boolean isDebug) { this.d = isDebug; return this; }
  }
  
  public static class DbCursorFactory implements SQLiteDatabase.CursorFactory
  {
    protected String editTable;
    
    public DbCursorFactory() {
      this.editTable = "";
    }
    
    public void setEditTable(String editTable) {
      this.editTable = editTable;
    }
    
    @Override
    public Cursor newCursor(final SQLiteDatabase db,
    final SQLiteCursorDriver driver, final String sql,
    final SQLiteQuery query)
    {
      final CancellationSignal cancellationSignal = new CancellationSignal();
      //final SQLiteStatementInfo stmtInfo = prepare(db, sql);
      final SQLiteCursorDriver cursorDriver = new SQLiteDirectCursorDriver(
        db, sql, editTable, cancellationSignal
      );
      final Cursor cursor = new DbCursor(db, driver, editTable, query);
      return cursor;
    }
  }
  
  public static SQLiteStatementInfo prepare(final SQLiteDatabase db,
  final String sql)
  {
    if (sql == null) throw new IllegalArgumentException(String.format(
      "DatabaseUtil.prepare(db: %s, sql: %s):  sql == null",
      Debug.ToString(db), sql
    ));
    final CancellationSignal cancellationSignal = new CancellationSignal();
    final SQLiteStatementInfo stmtInfo = new SQLiteStatementInfo();
    Reflector.invokeOrDefault(
      Reflector.invokeOrDefault(db, "getThreadSession"),
      "prepare",
      sql,
      Reflector.<Integer>invokeOrDefault(
        db, "getThreadDefaultConnectionFlags", Boolean.TRUE
      ),
      cancellationSignal,
      stmtInfo
    );
    return stmtInfo;
  }
  
  public static SQLiteDatabase open(final String path, final int flags, 
  final DatabaseErrorHandler errorHandler)
  {
    final SQLiteDatabase db = Reflect.newInstance(
      SQLiteDatabase.class,
      path,
      flags,
      new DbCursorFactory(),
      (errorHandler != null) ? errorHandler : new DbErrorHandler()
    );
    if (!SQLiteDatabase_open(db, true)) {
      close(db);
      SQLiteDatabase_open(db, false);
    } else {
      if (! db.isOpen()) SQLiteDatabase_openInner(db, false);
    }
    // if ((flags & FLAG_OPEN_READONLY) == 0) db.beginTransaction();
    return db;
  }
  
  public static SQLiteDatabase open(final String path, final int flags) {
    return open(path, flags, (DatabaseErrorHandler) null);
  }
  
  public static SQLiteDatabase open(final String path) {
    return open(path, FLAG_OPEN_READONLY);
  }
  
  public static String getLabel(final SQLiteDatabase db) {
    return (String) invokeOrDefault(db, "getLabel");
  }
  
  public static boolean SQLiteDatabase_open(final SQLiteDatabase db,
  final boolean testOnly)
    throws SQLiteException
  {
    SQLiteDatabaseCorruptException suppressed = null;
    try {
      try {
        return SQLiteDatabase_openInner(db, testOnly);
      } catch (final SQLiteDatabaseCorruptException ex) {
        suppressed = ex;
        Log.w(TAG,
          String.format("Database '%s' is corrupt: %s", getLabel(db), ex), ex);
        invokeOrDefault(db, "onCorruption");
        return SQLiteDatabase_openInner(db, testOnly);
      }
    } catch (final SQLiteException tr) {
      if (suppressed != null) tr.addSuppressed(suppressed);
      Log.e(TAG_DB,
        String.format("Failed to open database '%s'.", getLabel(db)), tr);
      if (testOnly) {
        tr.printStackTrace();
        return false;
      }
      db.close();
      throw tr;
    }
  }
  
  public static boolean SQLiteDatabase_openInner(final SQLiteDatabase db, 
  final boolean testOnly)
  {
    final Object mLock = getfldval(db, "mLock");
    synchronized (mLock) {
      Object mConnectionPoolLocked = getfldval(db, "mConnectionPoolLocked");
      if (mConnectionPoolLocked == null) {
        // OK
      } else {
        if (testOnly) return false;
        throw new AssertionError(String.format(
          "Assertion '[db].mConnectionPoolLocked == null' failed. Value: %s",
          mConnectionPoolLocked
        ));
      }
    
      final Object mConfigurationLocked = getfldval(db, "mConfigurationLocked");
      mConnectionPoolLocked = invokeOrDefault(
        SQLiteConnectionPool.class, "open", mConfigurationLocked
      );
      setfldval(db, "mConnectionPoolLocked", mConnectionPoolLocked);
    } // monitorexit(o)    
    synchronized (sActiveDatabases) {
      sActiveDatabases.put(db, null);
    }
    return true;
  }
  
  
  public static SQLiteQuery newQuery(SQLiteDatabase db, String sql) {
    final CancellationSignal cancellationSignal = new CancellationSignal();
    final SQLiteStatementInfo stmtInfo = prepare(db, sql);
    return Reflect.newInstance(
      SQLiteQuery.class, db, sql, cancellationSignal
    );
  }
  
  public static SQLiteSession getSession(final SQLiteDatabase db) {
    return Reflector.invokeOrDefault(db, "getThreadSession");
  }
  
  public static SQLiteConnectionPool getConnectionPool(SQLiteSession sess) {
    return Reflect.getfldval(sess, "mConnectionPool", false);
  }
  
  public static SQLiteDatabaseConfiguration getConfiguration(
  final SQLiteConnection conn)
  {
    final SQLiteDatabaseConfiguration conf = getfldval(conn, "mConfiguration");
    return conf;
  }
  
  public static String getPath(final SQLiteConnection conn) {
    final SQLiteDatabaseConfiguration conf = getConfiguration(conn);
    return conf.path;
  }
  
  public static String getPath(final SQLiteDatabase db) {
    return getPath(getConnection(getSession(db)));
  }
  
  static final LazyMember<Field> fld_Session_mConnection = LazyMember.of(
    "mConnection", SQLiteSession.class);
  static final LazyMember<Field> fld_Session_mConnectionPool = LazyMember.of(
    "mConnectionPool", SQLiteSession.class);
  static final LazyMember<Field> fld_ConnPool_availPriConn = LazyMember.of(
    "mAvailablePrimaryConnection", SQLiteConnectionPool.class);
  static final LazyMember<Field> fld_ConnPool_acquiredConns = LazyMember.of(
    "mAcquiredConnections", SQLiteConnectionPool.class);
  
  
  public static List<Throwable> close(SQLiteDatabase db) {
    List<Throwable> errors = new ArrayList<>();
    final SQLiteSession sess = getSession(db);
    final SQLiteConnection conn = getConnection(sess);
    final SQLiteConnectionPool cpool = getConnectionPool(sess);
    try {
      try {
        Reflector.invokeOrDefault(conn, "close");
      } catch (Throwable e) { errors.add(e); }
      try {
        Reflector.invokeOrDefault(sess, "releaseConnection");
      } catch (Throwable e) { errors.add(e); }
      try {    
        db.close();
      } catch (Throwable e) { errors.add(e); }
      
      for (String methodName: new String[]{ 
          "closeAvailableConnectionsAndLogExceptionsLocked",
          "closeAvailableNonPrimaryConnectionsAndLogExceptionsLocked",
          "closeExcessConnectionsAndLogExceptionsLocked",
          "discardAcquiredConnectionsLocked"
      })
      {
        try {
          Reflect.invoke(
            SQLiteConnectionPool.class,
            cpool,
            methodName,
            true,
            new Object[0]
          );
        } catch (Exception ite) {
          Throwable ex = (ite instanceof InvocationTargetException)
            ? ((InvocationTargetException) ite).getTargetException()
            : ite;
          errors.add(ex);
        } catch (Throwable e) {
          errors.add(e);
        }
      }
    } finally {
      if (cpool != null) {
        try {
          cpool.close();
        } catch (Throwable e) { errors.add(e); }
        try {
          Reflector.invokeOrDefault(
            cpool, "dispose", new Object[] { Boolean.TRUE }
          );
        } catch (Throwable e) { errors.add(e); }
      }
    }
    return errors;
  }
  
  public static
    <AcquiredConnectionStatus extends Enum<AcquiredConnectionStatus>>
  SQLiteConnection getConnection(final SQLiteSession sess)
  {
    
    SQLiteConnection conn = Reflect.getfldval(sess, "mConnection", false);
    if (conn != null) return conn;
    
    final SQLiteConnectionPool pool = getConnectionPool(sess);
    
    SQLiteConnection availPriConn = fld_ConnPool_availPriConn.getValue(pool);
    if (availPriConn != null) {
      SQLiteConnection officialConn = Reflector.invokeOrDefault(
        pool, "tryAcquirePrimaryConnectionLocked", // TODO: read/write flags
        new Object[]{ CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY }
      );
      if (conn != null) return conn;
    }
    
    // release existing acquired or abandoned connection
    final Map<SQLiteConnection, AcquiredConnectionStatus> acquired 
        = fld_ConnPool_acquiredConns.getValue(pool);
    
    outer:
    do {
      for (Map.Entry<SQLiteConnection, AcquiredConnectionStatus> entry
          : CollectionFactory.newSet(acquired.entrySet()))
      {
        final SQLiteConnection acqConn = entry.getKey();
        final AcquiredConnectionStatus status = entry.getValue();
        
        if (VERBOSE) {
          final String blurb = String.format(
            "  - Considering conn@%08x: %s[id:%d, pri:%s, status:%s] ...\n",
            System.identityHashCode(acqConn),
            acqConn.getClass().getSimpleName(),
            acqConn.getConnectionId(),
            Boolean.valueOf(acqConn.isPrimaryConnection()),
            status.name()
          );
          System.err.println(blurb);
        }
        
        if (! acqConn.isPrimaryConnection()) continue;
        
        // Found primary connection
        if (fld_ConnPool_availPriConn.getValue(pool) == null) { 
          AcquiredConnectionStatus statusOfRemoved = acquired.remove(acqConn);
          if (statusOfRemoved != null) {
            fld_ConnPool_availPriConn.setValue(pool, acqConn);
            if (VERBOSE) {
              System.err.printf(
                "Acquired primary connection[id:%d, pri:%s, status:%s]: %s\n",
                acqConn.getConnectionId(),
                Boolean.valueOf(acqConn.isPrimaryConnection()),
                statusOfRemoved.name(),
                acqConn
              );
            }
            break outer;
          }
        }
      }// end pf for
      // reach only when loop fails to find primary
      System.err.printf(
        "Failed to find primary connection in acquiredConnections map! \n" +
        "  map(size: %d): %s\n", acquired.size(), acquired
      );
    } while (false);
    return Reflector.invokeOrDefault(
      pool, "tryAcquirePrimaryConnectionLocked",
      new Object[]{ CONNECTION_FLAG_PRIMARY_CONNECTION_AFFINITY }
    );
  }
  
  public static <T> T getValue(AbstractCursor c, int columnIndex) {
    int typeId = c.getType(columnIndex);
    boolean isNull = c.isNull(columnIndex);
    if (isNull) return null;
    switch (typeId) {
      case FIELD_TYPE_NULL:
      case FIELD_TYPE_STRING:
        return (T) (Object) c.getString(columnIndex);
      case FIELD_TYPE_INTEGER:
        return (T) (Object) c.getInt(columnIndex);
      case FIELD_TYPE_FLOAT:
        return (T) (Object) c.getFloat(columnIndex);
      case FIELD_TYPE_BLOB:
        return (T) (Object) c.getBlob(columnIndex);
      default:
        throw new IllegalStateException(String.format(
          "getTypedValue(c: %s, columnIndex: %d): Unexpected typeId returned by " +
          "c.getType(columnIndex: %d): %d",
          c, columnIndex, columnIndex, typeId
        ));
    }
  }
  
  
  public static List<? extends Map<String, Object>> getResult(AbstractCursor c) {
    final int oldPos = c.getPosition();
    final int numCols = c.getColumnCount();
    final String[] colNames = c.getColumnNames();
    final Pair<Integer, String>[] indexedColNames 
                = CollectionUtil.indexed(colNames);
    
    try {
      c.moveToFirst();
      List<Map<String, Object>> rows = new ArrayList<>(c.getCount());
      
      do {
        final Map<String, Object> row = new RealArrayMap<>(numCols);
        for (final Map.Entry<Integer, String> m: indexedColNames) {
          int colIdx = m.getKey().intValue();
          String colName = m.getValue();
          Object obj = getValue(c, colIdx);
          final Object value = (obj != null)
            ? obj
            : new NullKey(colIdx);
          row.put(colName, value);
        }
        rows.add(row);
      } while (c.moveToNext());
      return rows;
    } finally { 
      c.moveToPosition(oldPos);
    }   
  }
  
  public static AbstractCursor openQuery(SQLiteDatabase db, SQLiteQuery query) {
    final CancellationSignal signal
        = CancellationSignal.fromTransport(CancellationSignal.createTransport());
    final SQLiteCursorDriver drv
      = new SQLiteDirectCursorDriver(db, null, null, signal);
    final SQLiteDatabase.CursorFactory _fact = getfldval(db, "mCursorFactory");
    final SQLiteDatabase.CursorFactory cfact = (_fact != null) ? _fact : new DbCursorFactory();
    
    final AbstractCursor c
       = (AbstractCursor) cfact.newCursor(db, drv, null, query);
    try {
      c.getCount();
    } catch (Throwable e) { e.printStackTrace(); }
    return c;
  }
  
  public static WrappedDatabase wrap(String path) {
    return new WrappedDatabase(
      DatabaseUtil.open(path, FLAG_OPEN_READONLY), true
    );
  }
  
  public static class WrappedDatabase {
    protected SQLiteDatabase db;
    protected boolean owner;
    
    public WrappedDatabase(final SQLiteDatabase db, boolean owner) {
      this.db = db;
      this.owner = owner;
    }
    
    public WrappedDatabase(final SQLiteDatabase db) {
      this(db, false);
    }
    
    @Override
    public String toString() {
      return String.format(
        "%s{db: %s, owner: %s}", getClass().getSimpleName(), db, owner
      );
    }
    
    public QueryCursor openQuery(String sql) {
      AbstractCursor c = DatabaseUtil.openQuery(db, newQuery(db, sql));
      return this.new QueryCursor(c);
    }
    
    public class QueryCursor {
      public final AbstractCursor cursor;
      public final SQLiteQuery query;
      
      public QueryCursor(AbstractCursor cursor) {
        this.cursor = cursor;
        this.query = getfldval(cursor, "mQuery");
      }
      
      @Override
      public String toString() {
        int pos = cursor.getPosition();
        return String.format(
          "%s{cursor: [%s @ row %s/%d], query: %s} from %s",
          getClass().getSimpleName(),
          cursor,
          (pos != -1)? Integer.toString(pos, 10): "-",
          cursor.getCount(), query, db
        );
      }
      
      public List<? extends Map<String, Object>> list() {
        return DatabaseUtil.getResult(cursor);
      }
    }
  }
}