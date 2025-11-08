package org.wsitm.schemax.dialects;

import com.github.drinkjava2.jdialects.DDLFeatures;
import com.github.drinkjava2.jdialects.Dialect;
import com.github.drinkjava2.jdialects.Type;

import java.util.Map;

// TODO
public class Hive2Dialect extends Dialect {

    public final static Dialect DIALECT = new Hive2Dialect("Hive2Dialect");

    public Hive2Dialect(String name) {
        super(name);
        this.init();
    }

    private void init() {
        DDLFeatures ddlFeatures = DDLFeatures.createDefaultDDLFeatures();
        // TODO hive2 语句和MySql类似，临时套用
        ddlFeatures.setAddColumnString("ADD COLUMN");
        ddlFeatures.setAddFKeyRefPkeyString(" ADD CONSTRAINT _FKEYNAME FOREIGN KEY (_FK1, _FK2) REFERENCES _REFTABLE (_REF1, _REF2)");
        ddlFeatures.setCloseQuote("`");
        ddlFeatures.setColumnComment(" COMMENT '_COMMENT'");
        ddlFeatures.setCreateTableString("CREATE TABLE IF NOT EXISTS");
        ddlFeatures.setCreateMultisetTableString("CREATE TABLE IF NOT EXISTS");
        ddlFeatures.setCreateCatalogCommand("CREATE DATABASE _CATALOGNAME");
        ddlFeatures.setCreatePooledSequenceStrings(DDLFeatures.NOT_SUPPORT);
        ddlFeatures.setCreateSchemaCommand(DDLFeatures.NOT_SUPPORT);
        ddlFeatures.setCreateSequenceStrings(DDLFeatures.NOT_SUPPORT);
        ddlFeatures.setDropCatalogCommand("DROP DATABASE _CATALOGNAME");
        ddlFeatures.setDropForeignKeyString(" DROP FOREIGN KEY ");
        ddlFeatures.setDropSchemaCommand(DDLFeatures.NOT_SUPPORT);
        ddlFeatures.setDropSequenceStrings(DDLFeatures.NOT_SUPPORT);
        ddlFeatures.setDropTableString("DROP TABLE IF EXISTS _TABLENAME");
        ddlFeatures.setIdentityColumnString("");
        ddlFeatures.setIdentityColumnStringBigINT("");
        ddlFeatures.setNullColumnString("");
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
        m.put(Type.BINARY, "BINARY");
        m.put(Type.BIT, "BOOLEAN");
        m.put(Type.LONGBLOB, "BINARY");
        m.put(Type.BLOB, "BINARY");
        m.put(Type.BOOLEAN, "BOOLEAN");
        m.put(Type.CHAR, "STRING");
        m.put(Type.CLOB, "STRING");
        m.put(Type.DATE, "DATE");
        m.put(Type.DOUBLE, "DOUBLE");
        m.put(Type.FLOAT, "FLOAT");
        m.put(Type.INTEGER, "INT");
        m.put(Type.LONGVARBINARY, "BINARY");
        m.put(Type.LONGVARCHAR, "STRING");
        m.put(Type.NCLOB, "STRING");
        m.put(Type.NUMERIC, "DECIMAL");
        m.put(Type.TIME, "TIMESTAMP");  // hive2 不支持，使用时间代替
        m.put(Type.DATETIME, "TIMESTAMP");
        m.put(Type.VARBINARY, "BINARY");
        m.put(Type.DECIMAL, "DECIMAL");
        m.put(Type.SMALLINT, "SMALLINT");
        m.put(Type.MEDIUMINT, "INT");
        m.put(Type.TINYINT, "TINYINT");
        m.put(Type.BIGINT, "BIGINT");
        m.put(Type.YEAR, "INT");
        m.put(Type.INT, "INT");
        m.put(Type.TINYBLOB, "BINARY");
        m.put(Type.TINYTEXT, "STRING");
        m.put(Type.TEXT, "STRING");
        m.put(Type.MEDIUMBLOB, "BINARYBINARY");
        m.put(Type.MEDIUMTEXT, "STRING");
        m.put(Type.LONGTEXT, "STRING");
        m.put(Type.JSON, "STRING");
        m.put(Type.JAVA_OBJECT, "STRING");
        m.put(Type.TIMESTAMP, "TIMESTAMP");
        m.put(Type.VARCHAR, "STRING");

    }

}
