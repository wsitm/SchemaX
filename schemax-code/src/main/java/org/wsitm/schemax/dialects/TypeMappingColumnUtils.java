package org.wsitm.schemax.dialects;

import cn.hutool.core.util.StrUtil;
import com.github.drinkjava2.jdialects.model.ColumnModel;
import org.wsitm.schemax.entity.vo.ColumnVO;
import org.wsitm.schemax.entity.vo.TypeMappingResult;

import java.sql.Types;

public class TypeMappingColumnUtils {
    private TypeMappingColumnUtils() {
    }

    public static void applyCustomDefinition(ColumnModel columnModel, ColumnVO columnVO, TypeMappingResult mappingResult) {
        applyCustomDefinition(columnModel, columnVO, mappingResult, true);
    }

    public static void applyCustomDefinition(ColumnModel columnModel, ColumnVO columnVO,
                                             TypeMappingResult mappingResult, boolean includeColumnConstraints) {
        if (columnModel == null || columnVO == null || mappingResult == null || !mappingResult.isMatched()) {
            return;
        }
        StringBuilder definition = new StringBuilder();
        definition.append(" ").append(mappingResult.getTypeExpression());
        if (includeColumnConstraints && StrUtil.isNotEmpty(columnVO.getColumnDef())) {
            definition.append(" default ").append(formatDefaultValue(columnVO));
        }
        if (includeColumnConstraints && !columnVO.isNullable()) {
            definition.append(" not null");
        }
        columnModel.setColumnDefinition(definition.toString());
    }

    private static String formatDefaultValue(ColumnVO columnVO) {
        String value = columnVO.getColumnDef();
        if (isTextType(columnVO.getType()) && !StrUtil.startWith(value, "'")) {
            return "'" + value + "'";
        }
        return value;
    }

    private static boolean isTextType(Integer type) {
        if (type == null) {
            return false;
        }
        return type == Types.VARCHAR
                || type == Types.CHAR
                || type == Types.LONGVARCHAR
                || type == Types.LONGVARBINARY
                || type == Types.NVARCHAR;
    }
}
