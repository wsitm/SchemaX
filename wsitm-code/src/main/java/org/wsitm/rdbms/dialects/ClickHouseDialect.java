package org.wsitm.rdbms.dialects;

import com.github.drinkjava2.jdialects.DDLFeatures;
import com.github.drinkjava2.jdialects.Dialect;
import com.github.drinkjava2.jdialects.Type;

import java.util.Map;

public class ClickHouseDialect extends Dialect {

    public final static Dialect DIALECT = new ClickHouseDialect("ClickHouseDialect");

    public ClickHouseDialect(String name) {
        super(name);
        this.init();
    }

    private void init() {
        DDLFeatures ddlFeatures = DDLFeatures.createDefaultDDLFeatures();
        ddlFeatures.setAddColumnString("ADD COLUMN");
        ddlFeatures.setAddFKeyRefPkeyString(" ADD CONSTRAINT _FKEYNAME FOREIGN KEY (_FK1, _FK2) REFERENCES _REFTABLE (_REF1, _REF2)");
        ddlFeatures.setCloseQuote("`");
        ddlFeatures.setColumnComment(" COMMENT '_COMMENT'");
        ddlFeatures.setCreateTableString("CREATE TABLE IF NOT EXISTS");
        ddlFeatures.setCreateCatalogCommand("CREATE DATABASE _CATALOGNAME");
        ddlFeatures.setCreatePooledSequenceStrings(DDLFeatures.NOT_SUPPORT);
        ddlFeatures.setCreateSchemaCommand(DDLFeatures.NOT_SUPPORT);
        ddlFeatures.setCreateSequenceStrings(DDLFeatures.NOT_SUPPORT);
        ddlFeatures.setDropCatalogCommand("DROP DATABASE _CATALOGNAME");
        ddlFeatures.setDropForeignKeyString(" DROP FOREIGN KEY ");
        ddlFeatures.setDropSchemaCommand(DDLFeatures.NOT_SUPPORT);
        ddlFeatures.setDropSequenceStrings(DDLFeatures.NOT_SUPPORT);
        ddlFeatures.setDropTableString("DROP TABLE IF EXISTS _TABLENAME");
        ddlFeatures.setIdentityColumnString("NOT NULL"); // auto_increment
        ddlFeatures.setIdentityColumnStringBigINT("NOT NULL");// auto_increment
        ddlFeatures.setIdentitySelectString("SELECT last_insert_id()");
        ddlFeatures.setIdentitySelectStringBigINT("SELECT last_insert_id()");
        ddlFeatures.setNeedDropConstraintsBeforeDropTable(true);
        ddlFeatures.setOpenQuote("`");
        ddlFeatures.setSelectSequenceNextValString(DDLFeatures.NOT_SUPPORT);
        ddlFeatures.setSequenceNextValString(DDLFeatures.NOT_SUPPORT);
        ddlFeatures.setSupportsColumnCheck(false);
        ddlFeatures.setSupportsCommentOn(false);
        ddlFeatures.setSupportsIdentityColumns(true);
        ddlFeatures.setSupportsPooledSequences(false);
        ddlFeatures.setSupportsSequences(false);
//        ddlFeatures.setTableTypeString(" engine=InnoDB");
        this.ddlFeatures = ddlFeatures;


        Map<Type, String> m = this.typeMappings;
        m.put(Type.BINARY, "FixedString($l)");
        m.put(Type.BIT, "UInt8");
        m.put(Type.LONGBLOB, "String");
        m.put(Type.BLOB, "String");
        m.put(Type.BOOLEAN, "UInt8");
        m.put(Type.CHAR, "FixedString($l)");
        m.put(Type.CLOB, "String");
        m.put(Type.DATE, "Date");
        m.put(Type.DOUBLE, "Float64");
        m.put(Type.FLOAT, "Float32");
        m.put(Type.INTEGER, "Int32");
        m.put(Type.LONGVARBINARY, "String");
        m.put(Type.LONGVARCHAR, "String");
        m.put(Type.NCLOB, "String");
        m.put(Type.NUMERIC, "Decimal($p,$s)");
        m.put(Type.TIME, "DateTime"); //
        m.put(Type.DATETIME, "DateTime64");
        m.put(Type.VARBINARY, "String");
        m.put(Type.DECIMAL, "Decimal($p,$s)");
        m.put(Type.SMALLINT, "Int16");
        m.put(Type.MEDIUMINT, "Int32");
        m.put(Type.TINYINT, "Int8");
        m.put(Type.BIGINT, "Int64");
        m.put(Type.YEAR, "UInt16");
        m.put(Type.INT, "Int32");
        m.put(Type.TINYBLOB, "String");
        m.put(Type.TINYTEXT, "String");
        m.put(Type.TEXT, "String");
        m.put(Type.MEDIUMBLOB, "String");
        m.put(Type.MEDIUMTEXT, "String");
        m.put(Type.LONGTEXT, "String");
        m.put(Type.JSON, "String");
        m.put(Type.JAVA_OBJECT, "String");
        m.put(Type.TIMESTAMP, "DateTime64");
        m.put(Type.VARCHAR, "String");

    }

}
