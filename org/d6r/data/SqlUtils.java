package org.d6r.data;

import com.google.common.base.Supplier;                                           import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import java.io.File;
import java.util.*;
import org.d6r.*;
import org.tmatesoft.sqljet.core.SqlJetException;
import org.tmatesoft.sqljet.core.SqlJetTransactionMode;
import org.tmatesoft.sqljet.core.internal.lang.ISqlJetConnection;
import org.tmatesoft.sqljet.core.internal.lang.SqlJetConnection;
import org.tmatesoft.sqljet.core.internal.lang.SqlJetPreparedStatement;
import org.tmatesoft.sqljet.core.schema.ISqlJetColumnDef;
import org.tmatesoft.sqljet.core.schema.ISqlJetTableDef;
import org.tmatesoft.sqljet.core.schema.ISqlJetTypeDef;
import org.tmatesoft.sqljet.core.schema.SqlJetTypeAffinity;
import org.tmatesoft.sqljet.core.table.ISqlJetTable;
import org.tmatesoft.sqljet.core.table.SqlJetDb;
import org.tmatesoft.sqljet.core.SqlJetValueType;
import static org.tmatesoft.sqljet.core.SqlJetValueType.NULL;

public class SqlUtils {
  
  public static <C, V> Supplier<? extends Map<C, V>> newRowSupplier(final int size)
  {
    return new Supplier<Map<C, V>>() {
      @Override
      public Map<C, V> get() {
        if (size >= 0) return new RealArrayMap<C, V>(size);
        else return Collections.<C, V>emptyMap();
      }
    };
  }
  
  
  public static SqlJetDb open(String dbFilePath, boolean beginTransaction) {
    SqlJetConnection connection = null;
    SqlJetDb db = null;
    try {
      connection = SqlJetConnection.open(dbFilePath);
      db = connection.getDb();
      if (beginTransaction) db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
      return db;
    } catch (SqlJetException ex) {
      try {
        if (beginTransaction && db != null) try { 
          db.rollback();
        } catch (Throwable ex1) { ex.addSuppressed(ex1); }
        if (db != null) try {
          db.close();
        } catch (Throwable ex2) { ex.addSuppressed(ex2); }
        if (connection != null) try { 
          connection.close();
        } catch (Throwable ex3) { ex.addSuppressed(ex3); }
      } catch (Throwable ex4) { ex.addSuppressed(ex4); }
      throw Reflector.Util.sneakyThrow(ex);
    }
  }
  
  
  public static SqlJetDb open(String dbFilePath) {
    return open(dbFilePath, true);
  }
  
  
  public static Table<Integer, String, Object> query(String dbFilePath, String sql)
    throws SqlJetException
  {
    try (SqlJetConnection connection = SqlJetConnection.open(dbFilePath);
         SqlJetDb db = connection.getDb())
    {
      db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
      try (final SqlJetPreparedStatement st = connection.prepare(sql)) {
        
        return collect(st);
        
      } finally {
        db.rollback();
      }
    }
  }
  
  
  public static SqlJetPreparedStatement prepare(SqlJetDb db, String sql)
    throws SqlJetException
  {
    ISqlJetConnection connection = db.getConnection();
    if (db.isInTransaction()) db.rollback();
    db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
    SqlJetPreparedStatement st = connection.prepare(sql);
    return st;
  }
  
  
  
  public static Table<Integer, String, Object> collect(SqlJetPreparedStatement st,
  int limit)
  {
    try {
      if (! st.step()) return emptyTable();
            
      final int numColumns = st.getColumnsCount();
      final ISqlJetTable table = Reflect.getfldval(st, "table");
      final ISqlJetTableDef tableDef = table.getDefinition();
      final ISqlJetColumnDef[] columns
        = tableDef.getColumns().toArray(new ISqlJetColumnDef[0]);
      final SqlJetTypeAffinity[] types = new SqlJetTypeAffinity[numColumns];
      final String[] names = new String[numColumns];
      for (int i=0; i<numColumns; ++i) {
        types[i] = columns[i].getTypeAffinity();
        names[i] = columns[i].getName();
      }
      
      final Supplier<? extends Map<String, Object>> rowSupplier 
        = newRowSupplier(numColumns);
      final Table<Integer, String, Object> resultTable
        = Tables.newCustomTable(new TreeMap<>(), rowSupplier);
      int rowIndex = -1;
      do {
        final Integer rowKey = Integer.valueOf(++rowIndex);
        for (int c=0; c<numColumns; c++) {
          final String name = names[c];
          final SqlJetTypeAffinity type = types[c];
          Object val = null;
          switch (type) {
            case INTEGER: val = st.getInteger(c); break;
            case NONE: val = st.getBlobAsArray(c); break;
            case NUMERIC: st.getInteger(c); break;
            case REAL: val = st.getFloat(c); break;
            case TEXT: val = st.getText(c); break;
            default:
              throw new RuntimeException(String.format(
                "Unhandled type affinity: '%s' in column[%d] (`%s`): %s",
                type, c, names[c], Debug.ToString(columns[c].getType())
              ));
          }
          resultTable.put(
            rowKey,
            name != null? name: String.format("column_%03d", c),
            val != null? val: NULL
          );
        }
      } while ((limit == -1 || rowIndex < limit) && st.step());
      return resultTable;
    } catch (SqlJetException ex) { 
      throw Reflector.Util.sneakyThrow(ex);
    }
  }
  
  
  public static Table<Integer, String, Object> collect(SqlJetPreparedStatement st) {
    return collect(st, -1);
  }
  
  
  
  
  private static Table<?,?,?> EMPTY_TABLE;
  protected static final <R, C, V> Table<R, C, V> emptyTable() {
    if (EMPTY_TABLE == null) {
      EMPTY_TABLE = (Table<?,?,?>)
        Tables.newCustomTable(Collections.emptyMap(), newRowSupplier(-1));
    }
    return (Table<R, C, V>) (Table<?,?,?>) EMPTY_TABLE;
  }
  
  
  /*
  public static Map<String, ISqlJetSchemaDef> schema(String dbFilePath) {
    try (SqlJetConnection connection = SqlJetConnection.open(dbFilePath);
         SqlJetDb db = connection.getDb()) {
      db.beginTransaction(SqlJetTransactionMode.READ_ONLY);
      ISqlJetSchema = db.getSchema();
      try (final SqlJetPreparedStatement st = connection.prepare(sql)) {
      } finally {
        db.rollback();
      }
    }
  }
  */
  
}


