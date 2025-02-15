import {DEFAULT_WORKBOOK_DATA} from "@/views/rdbms/components/UniverSheet/sheet-data";

export const tableKeys = ["tableName", "comment"];
export const titleKeys = ["序号", "字段", "类型", "长度", "为空", "自增", "主键", "默认", "注释"];
export const columnKeys = ["order", "name", "typeName", "size", "isNullable", "autoIncrement", "isPk", "columnDef", "comment"];

/**
 * 表格元信息数据转换excel数据
 * @param list  表格元信息数据
 * @returns {{}}
 */
export function tableInfoToWorkbookData(list) {
  let mergeData = [];
  let cellData = {};
  let bd = {t: {s: 1}, r: {s: 1}, b: {s: 1}, l: {s: 1}};
  let rowIndex = 1;
  for (let i = 0; i < list.length; i++) {
    const table = list[i];
    cellData[String(rowIndex)] = {
      "0": {
        v: (i + 1) + ". " + (tableKeys.map(key => table[key]).join(", ")),
        s: {
          bl: 1,
          bd: bd
        }
      }
    };
    mergeData.push({
      rangeType: 0,
      startRow: rowIndex,
      endRow: rowIndex,
      startColumn: 0,
      endColumn: titleKeys.length - 1
    });

    rowIndex++;
    cellData[String(rowIndex)] = {};
    for (let j = 0; j < titleKeys.length; j++) {
      cellData[String(rowIndex)][String(j)] = {
        v: titleKeys[j] || "",
        s: {
          bg: {rgb: "rgb(204,255,204)"},
          bl: 1,
          bd: bd
        }
      };
    }
    rowIndex++;
    const columns = table.columnList;
    if (columns && columns.length) {
      for (let k = 0; k < columns.length; k++) {
        const column = columns[k];
        cellData[String(rowIndex)] = {};
        for (let j = 0; j < columnKeys.length; j++) {
          cellData[String(rowIndex)][String(j)] = {
            v: j === 0 ? (k + 1) : column[columnKeys[j]] || "",
            s: {
              bd: bd
            }
          };
        }
        rowIndex++;
      }
    }
    rowIndex += 2;
  }

  const data = {...DEFAULT_WORKBOOK_DATA};
  data.sheets['sheet-01']['mergeData'] = mergeData;
  data.sheets['sheet-01']['cellData'] = cellData;
  data.sheets['sheet-01']['rowCount'] = rowIndex > 1 ? rowIndex : 100;
  return data;
}

/**
 * TODO excel数据转换表格元信息数据，待定
 * @param workbookData excel数据
 * @returns {*[]}
 */

export function workbookDataToTableInfo(workbookData) {
  const firstSheet = workbookData.sheets[Object.keys(workbookData.sheets)[0]];
  const cellData = firstSheet.cellData;

  const isRowEmpty = function (row) {
    const colKeys = Object.keys(row);
    for (let i = 0; i < colKeys.length; i++) {
      if (row[colKeys[i]]['v']) {
        return true;
      }
    }
    return true;
  }


  let result = {};
  let curKey = null;
  let columns = [];

  const rowKeys = Object.keys(cellData);
  for (let i = 0; i < rowKeys.length; i++) {
    const row = cellData[rowKeys[i]];
    if (isRowEmpty(row)) {
      curKey = null;

    } else {
      const colKeys = Object.keys(row);


    }
  }

  return [];
}
