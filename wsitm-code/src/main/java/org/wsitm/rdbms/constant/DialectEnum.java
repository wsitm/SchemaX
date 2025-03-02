package org.wsitm.rdbms.constant;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.github.drinkjava2.jdialects.Dialect;

import java.util.ArrayList;
import java.util.List;

import static cn.hutool.db.dialect.DriverNamePool.*;
import static com.github.drinkjava2.jdialects.Type.*;

public enum DialectEnum {

    MySQL8Dialect(DRIVER_MYSQL_V6, "MySQL8Dialect", Dialect.MySQL8Dialect),
    MariaDBDialect(DRIVER_MARIADB, "MariaDBDialect", Dialect.MariaDBDialect),
    PostgreSQLDialect(DRIVER_POSTGRESQL, "PostgreSQLDialect", Dialect.PostgreSQLDialect),
    Oracle12cDialect(DRIVER_ORACLE, "Oracle12cDialect", Dialect.Oracle12cDialect),
    DamengDialect(DRIVER_DM7, "DamengDialect", Dialect.DamengDialect),
    //    AccessDialect("", "AccessDialect", Dialect.AccessDialect),
    //    CobolDialect("", "CobolDialect", Dialect.CobolDialect),
    //    DbfDialect("", "DbfDialect", Dialect.DbfDialect),
    //    ExcelDialect("", "ExcelDialect", Dialect.ExcelDialect),
    //    ParadoxDialect("", "ParadoxDialect", Dialect.ParadoxDialect),
    SQLiteDialect(DRIVER_SQLLITE3, "SQLiteDialect", Dialect.SQLiteDialect),
    //    MckoiDialect("", "MckoiDialect", Dialect.MckoiDialect),
    //    MimerSQLDialect("", "MimerSQLDialect", Dialect.MimerSQLDialect),
    MySQLDialect(DRIVER_MYSQL, "MySQLDialect", Dialect.MySQLDialect),
    MySQL55Dialect(DRIVER_MYSQL, "MySQL55Dialect", Dialect.MySQL55Dialect),
    MySQL57Dialect(DRIVER_MYSQL, "MySQL57Dialect", Dialect.MySQL57Dialect),
    MySQL57InnoDBDialect(DRIVER_MYSQL, "MySQL57InnoDBDialect", Dialect.MySQL57InnoDBDialect),
    MySQL5Dialect(DRIVER_MYSQL, "MySQL5Dialect", Dialect.MySQL5Dialect),
    MySQL5InnoDBDialect(DRIVER_MYSQL, "MySQL5InnoDBDialect", Dialect.MySQL5InnoDBDialect),
    MySQLInnoDBDialect(DRIVER_MYSQL, "MySQLInnoDBDialect", Dialect.MySQLInnoDBDialect),
    MySQLMyISAMDialect(DRIVER_MYSQL, "MySQLMyISAMDialect", Dialect.MySQLMyISAMDialect),
    Oracle10gDialect(DRIVER_ORACLE, "Oracle10gDialect", Dialect.Oracle10gDialect),
    MariaDB102Dialect(DRIVER_MARIADB, "MariaDB102Dialect", Dialect.MariaDB102Dialect),
    MariaDB103Dialect(DRIVER_MARIADB, "MariaDB103Dialect", Dialect.MariaDB103Dialect),
    MariaDB10Dialect(DRIVER_MARIADB, "MariaDB10Dialect", Dialect.MariaDB10Dialect),
    MariaDB53Dialect(DRIVER_MARIADB, "MariaDB53Dialect", Dialect.MariaDB53Dialect),
    Oracle8iDialect(DRIVER_ORACLE_OLD, "Oracle8iDialect", Dialect.Oracle8iDialect),
    Oracle9iDialect(DRIVER_ORACLE_OLD, "Oracle9iDialect", Dialect.Oracle9iDialect),
    //    PointbaseDialect("", "PointbaseDialect", Dialect.PointbaseDialect),
    PostgresPlusDialect(DRIVER_POSTGRESQL, "PostgresPlusDialect", Dialect.PostgresPlusDialect),
    PostgreSQL81Dialect(DRIVER_POSTGRESQL, "PostgreSQL81Dialect", Dialect.PostgreSQL81Dialect),
    PostgreSQL82Dialect(DRIVER_POSTGRESQL, "PostgreSQL82Dialect", Dialect.PostgreSQL82Dialect),
    PostgreSQL91Dialect(DRIVER_POSTGRESQL, "PostgreSQL91Dialect", Dialect.PostgreSQL91Dialect),
    PostgreSQL92Dialect(DRIVER_POSTGRESQL, "PostgreSQL92Dialect", Dialect.PostgreSQL92Dialect),
    PostgreSQL93Dialect(DRIVER_POSTGRESQL, "PostgreSQL93Dialect", Dialect.PostgreSQL93Dialect),
    PostgreSQL94Dialect(DRIVER_POSTGRESQL, "PostgreSQL94Dialect", Dialect.PostgreSQL94Dialect),
    PostgreSQL95Dialect(DRIVER_POSTGRESQL, "PostgreSQL95Dialect", Dialect.PostgreSQL95Dialect),
    PostgreSQL9Dialect(DRIVER_POSTGRESQL, "PostgreSQL9Dialect", Dialect.PostgreSQL9Dialect),
    //    ProgressDialect("", "ProgressDialect", Dialect.ProgressDialect),
    Hive2Dialect(DRIVER_HIVE2, "Hive2Dialect", org.wsitm.rdbms.dialects.Hive2Dialect.DIALECT),
    ClickHouseDialect(DRIVER_CLICK_HOUSE, "ClickHouseDialect", org.wsitm.rdbms.dialects.ClickHouseDialect.DIALECT),
    GBaseDialect(DRIVER_GBASE, "GBaseDialect", Dialect.GBaseDialect),
    SQLServer2005Dialect(DRIVER_SQLSERVER, "SQLServer2005Dialect", Dialect.SQLServer2005Dialect),
    SQLServer2008Dialect(DRIVER_SQLSERVER, "SQLServer2008Dialect", Dialect.SQLServer2008Dialect),
    SQLServer2012Dialect(DRIVER_SQLSERVER, "SQLServer2012Dialect", Dialect.SQLServer2012Dialect),
    SQLServerDialect(DRIVER_SQLSERVER, "SQLServerDialect", Dialect.SQLServerDialect),
    H2Dialect(DRIVER_H2, "H2Dialect", Dialect.H2Dialect),
    Sybase11Dialect(DRIVER_SYBASE, "Sybase11Dialect", Dialect.Sybase11Dialect),
    SybaseAnywhereDialect(DRIVER_SYBASE, "SybaseAnywhereDialect", Dialect.SybaseAnywhereDialect),
    SybaseASE157Dialect(DRIVER_SYBASE, "SybaseASE157Dialect", Dialect.SybaseASE157Dialect),
    SybaseASE15Dialect(DRIVER_SYBASE, "SybaseASE15Dialect", Dialect.SybaseASE15Dialect),
    //    Teradata14Dialect("", "Teradata14Dialect", Dialect.Teradata14Dialect),
    //    TeradataDialect("", "TeradataDialect", Dialect.TeradataDialect),
    //    TimesTenDialect("", "TimesTenDialect", Dialect.TimesTenDialect),
    //    RDMSOS2200Dialect("", "RDMSOS2200Dialect", Dialect.RDMSOS2200Dialect),
    //    SAPDBDialect("", "SAPDBDialect", Dialect.SAPDBDialect),
    SybaseDialect(DRIVER_SYBASE, "SybaseDialect", Dialect.SybaseDialect),
    //    TextDialect("", "TextDialect", Dialect.TextDialect),
    //    XMLDialect("", "XMLDialect", Dialect.XMLDialect),
    //    Cache71Dialect("", "Cache71Dialect", Dialect.Cache71Dialect),
    //    CUBRIDDialect("", "CUBRIDDialect", Dialect.CUBRIDDialect),
    DataDirectOracle9Dialect(DRIVER_ORACLE_OLD, "DataDirectOracle9Dialect", Dialect.DataDirectOracle9Dialect),
    DB2390Dialect(DRIVER_DB2, "DB2390Dialect", Dialect.DB2390Dialect),
    DB2390V8Dialect(DRIVER_DB2, "DB2390V8Dialect", Dialect.DB2390V8Dialect),
    DB2400Dialect(DRIVER_DB2, "DB2400Dialect", Dialect.DB2400Dialect),
    DB297Dialect(DRIVER_DB2, "DB297Dialect", Dialect.DB297Dialect),
    DB2Dialect(DRIVER_DB2, "DB2Dialect", Dialect.DB2Dialect),
    DerbyTenFiveDialect(DRIVER_DERBY, "DerbyTenFiveDialect", Dialect.DerbyTenFiveDialect),
    DerbyTenSevenDialect(DRIVER_DERBY, "DerbyTenSevenDialect", Dialect.DerbyTenSevenDialect),
    DerbyTenSixDialect(DRIVER_DERBY, "DerbyTenSixDialect", Dialect.DerbyTenSixDialect),
    //    FirebirdDialect("", "FirebirdDialect", Dialect.FirebirdDialect),
    //    FrontBaseDialect("", "FrontBaseDialect", Dialect.FrontBaseDialect),
    //    HANAColumnStoreDialect("", "HANAColumnStoreDialect", Dialect.HANAColumnStoreDialect),
    //    HANARowStoreDialect("", "HANARowStoreDialect", Dialect.HANARowStoreDialect),
    HSQLDialect(DRIVER_HSQLDB, "HSQLDialect", Dialect.HSQLDialect);
    //    Informix10Dialect("", "Informix10Dialect", Dialect.Informix10Dialect),
    //    InformixDialect("", "InformixDialect", Dialect.InformixDialect),
    //    Ingres10Dialect("", "Ingres10Dialect", Dialect.Ingres10Dialect),
    //    Ingres9Dialect("", "Ingres9Dialect", Dialect.Ingres9Dialect),
    //    IngresDialect("", "IngresDialect", Dialect.IngresDialect),
    //    InterbaseDialect("", "InterbaseDialect", Dialect.InterbaseDialect),
    //    JDataStoreDialect("", "JDataStoreDialect", Dialect.JDataStoreDialect),;

    private final String driver;
    private final String database;
    private final Dialect dialect;

    DialectEnum(String driver, String database, Dialect dialect) {
        this.driver = driver;
        this.database = database;
        if (dialect == Dialect.DamengDialect) {
            dialect.ddlFeatures.setSupportsCommentOn(true);
            dialect.typeMappings.put(VARCHAR, "varchar($l)<8188|longvarchar");
            // TODO
            dialect.typeMappings.put(JAVA_OBJECT, "text");
        }
        if (StrUtil.containsIgnoreCase(dialect.getName(), "postgres")) {
            dialect.typeMappings.put(VARCHAR, "varchar($l)<65535|text");
        }
        if (dialect == Dialect.SQLiteDialect) {
            dialect.typeMappings.put(VARCHAR, "varchar($l)<4000|text");
            // TODO
            dialect.typeMappings.put(JAVA_OBJECT, "longvarchar");
        }
        if (StrUtil.containsIgnoreCase(dialect.getName(), "mysql")) {
            dialect.typeMappings.put(REAL, "real");
        }
        if (dialect == Dialect.MariaDBDialect || dialect == Dialect.MariaDB10Dialect) {
            // TODO
            dialect.typeMappings.put(JAVA_OBJECT, "longtext");
        }
        if (dialect == Dialect.GBaseDialect) {
            // TODO
            dialect.typeMappings.put(JAVA_OBJECT, "long");
        }
        if (dialect == Dialect.OracleDialect
                || dialect == Dialect.Oracle10gDialect
                || dialect == Dialect.Oracle12cDialect
                || dialect == Dialect.Oracle8iDialect
                || dialect == Dialect.Oracle9Dialect
                || dialect == Dialect.Oracle9iDialect) {
            // TODO
            dialect.typeMappings.put(JAVA_OBJECT, "long");
        }
        if (dialect == Dialect.PostgreSQLDialect) {
            // TODO
            dialect.typeMappings.put(JAVA_OBJECT, "text");
        }
        this.dialect = dialect;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public static List<Dict> getList() {
        List<Dict> list = new ArrayList<>();
        for (DialectEnum dialectEnum : DialectEnum.values()) {
            list.add(Dict.create()
                    .set("driver", dialectEnum.driver)
                    .set("database", dialectEnum.database));
        }
        return list;
    }

    public static DialectEnum getDialectByDriver(String driver) {
        for (DialectEnum dialectEnum : DialectEnum.values()) {
            if (dialectEnum.driver.equals(driver)) {
                return dialectEnum;
            }
        }
        return null;
    }

    public static DialectEnum getDialectByDatabase(String database) {
        for (DialectEnum dialectEnum : DialectEnum.values()) {
            if (dialectEnum.database.equals(database)) {
                return dialectEnum;
            }
        }
        return MySQL8Dialect;
    }

}
