export const tableKeys = ["tableName", "comment"];
export const titleKeys = [
  "序号",
  "字段",
  "类型",
  "长度",
  "小数",
  "不为空",
  "自增",
  "主键",
  "默认",
  "注释",
];
export const columnKeys = [
  "order",
  "name",
  "typeName",
  "size",
  "digit",
  "nullable",
  "autoIncrement",
  "pk",
  "columnDef",
  "comment",
];

/**
 * 表格元信息数据转换excel数据
 * @param list  表格元信息数据
 * @returns {{}}
 */
export function tableInfoToWorkbookData(list) {
  let mergeData = [];
  let cellData = {};
  let bd = { t: { s: 1 }, r: { s: 1 }, b: { s: 1 }, l: { s: 1 } };
  let rowIndex = 1;
  for (let i = 0; i < list.length; i++) {
    const table = list[i];
    cellData[String(rowIndex)] = {
      0: {
        v: i + 1 + ". " + tableKeys.map((key) => table[key]).join(", "),
        s: {
          bl: 1,
          bd: bd,
        },
      },
    };
    mergeData.push({
      rangeType: 0,
      startRow: rowIndex,
      endRow: rowIndex,
      startColumn: 0,
      endColumn: titleKeys.length - 1,
    });

    rowIndex++;
    cellData[String(rowIndex)] = {};
    for (let j = 0; j < titleKeys.length; j++) {
      cellData[String(rowIndex)][String(j)] = {
        v: titleKeys[j] || "",
        s: {
          bg: { rgb: "rgb(204,255,204)" },
          bl: 1,
          bd: bd,
        },
      };
    }
    rowIndex++;
    const columns = table.columnList;
    if (columns && columns.length) {
      for (let k = 0; k < columns.length; k++) {
        const column = columns[k];
        cellData[String(rowIndex)] = {};
        for (let j = 0; j < columnKeys.length; j++) {
          let val = k + 1;
          if (j > 0) {
            if (columnKeys[j] === "nullable") {
              val = !column[columnKeys[j]] ? "YES" : "";
            } else if (columnKeys[j] === "autoIncrement") {
              val = column[columnKeys[j]] ? "YES" : "";
            } else if (columnKeys[j] === "pk") {
              val = column[columnKeys[j]] ? "YES" : "";
            } else {
              val = column[columnKeys[j]] || "";
            }
          }
          cellData[String(rowIndex)][String(j)] = { v: val, s: { bd: bd } };
        }
        rowIndex++;
      }
    }
    rowIndex += 2;
  }
  // const data = {...DEFAULT_WORKBOOK_DATA};
  // data.sheets['sheet-01']['mergeData'] = mergeData;
  // data.sheets['sheet-01']['cellData'] = cellData;
  // data.sheets['sheet-01']['rowCount'] = rowIndex > 1 ? rowIndex : 100;
  return {
    mergeData: mergeData,
    cellData: cellData,
    rowCount: rowIndex > 1 ? rowIndex : 100,
  };
}

/**
 * 提取字符串中的单词和特殊字符
 * @param {string} str - 输入的字符串
 * @returns {{tokens: string[], others: string[]}} - 返回一个对象，包含单词数组和特殊字符数组
 */
function extractTokensAndOthers(str) {
  // 定义正则表达式来匹配单词和特殊字符
  const regex = /([a-zA-Z][a-zA-Z0-9_]*)|([^a-zA-Z]+)/g;
  // 初始化单词数组
  const tokens = [];
  // 初始化特殊字符数组
  const others = [];
  // 用于存储正则表达式的匹配结果
  let match;

  // 使用正则表达式循环匹配字符串
  while ((match = regex.exec(str)) !== null) {
    // 如果匹配到单词
    if (match[1]) {
      // 将单词添加到tokens数组中
      tokens.push(match[1]);
    } else if (match[2]) {
      // 将特殊字符添加到others数组中
      others.push(match[2]);
    }
  }
  // 返回包含单词和特殊字符的数组
  return { tokens, others };
}

/**
 * 获取数组中最长的字符串并去除最外层括号
 * @param array
 * @returns {*|string}
 */
function getLongestAndTrim(array) {
  if (!Array.isArray(array) || array.length === 0) {
    return "";
  }

  // 找出第一个最长的字符串
  const longest = array.reduce((a, b) => (a.length >= b.length ? a : b));

  // 移除最外层括号（仅当首尾是括号）
  if (longest.startsWith("(") && longest.endsWith(")")) {
    return longest.substring(1, longest.length - 1);
  }
  if (longest.startsWith("（") && longest.endsWith("）")) {
    return longest.substring(1, longest.length - 1);
  }
  if (longest.startsWith(",") || longest.startsWith("，")) {
    return longest.substring(1, longest.length);
  }

  return longest;
}

// 列名映射
const colTransMap = {
  "序号": "order",
  "顺序": "order",
  "排序": "order",
  "sort": "order",
  "order": "order",
  // ----------------
  "字段": "name",
  "名称": "name",
  "字段名": "name",
  "字段名称": "name",
  "name": "name",
  "field": "name",
  "column": "name",
  // ----------------
  "类型": "typeName",
  "字段类型": "typeName",
  "数据类型": "typeName",
  "type": "typeName",
  "typeName": "typeName",
  // ----------------
  "长度": "size",
  "字段长度": "size",
  "length": "size",
  "size": "size",
  // ----------------
  "小数": "digit",
  "字段小数": "digit",
  "digit": "digit",
  // ----------------
  "为空": "nullable",
  "字段为空": "nullable",
  "是否为空": "nullable",
  "必填": "nullable",
  "字段必填": "nullable",
  "是否必填": "nullable",
  "非空": "nullable",
  "不为空": "nullable",
  "nullable": "nullable",
  // ----------------
  "自增": "autoIncrement",
  "字段自增": "autoIncrement",
  "是否自增": "autoIncrement",
  "autoIncrement": "autoIncrement",
  // ----------------
  "主键": "pk",
  "字段主键": "pk",
  "是否主键": "pk",
  "pk": "pk",
  // ----------------
  "默认": "columnDef",
  "字段默认": "columnDef",
  "默认值": "columnDef",
  "default": "columnDef",
  "columnDef": "columnDef",
  // ----------------
  "注释": "comment",
  "字段注释": "comment",
  "说明": "comment",
  "备注": "comment",
  "字段备注": "comment",
  "描述": "comment",
  "字段描述": "comment",
  "remark": "comment",
  "comment": "comment"
};

/**
 * 将Excel数据转换为表格元信息数据
 * 该函数的目的是解析Excel数据（workbookData），并将其转换为一个结构化的表格元信息数组
 * 每个表格元信息包含表格的名称、注释和列信息
 *
 * @param workbookData Excel数据，应包含sheets属性，每个sheet包含cellData属性
 * @returns {Array} 返回一个结构化的表格元信息数组，每个元素包含tableName、comment和columnList
 */
export function workbookDataToTableInfo(workbookData) {
  // 检查workbookData的有效性，如果无效则返回空数组
  if (
    !workbookData ||
    !workbookData.sheets ||
    Object.keys(workbookData.sheets).length === 0
  ) {
    return [];
  }

  // 获取第一个sheet的数据
  const firstSheet = workbookData.sheets[Object.keys(workbookData.sheets)[0]];
  const cellData = firstSheet.cellData || {};

  /**
   * 判断一行是否为空
   * 该函数用于检查给定行是否为空（即不包含任何有效数据）
   *
   * @param row 行数据，应为一个对象，其中每个属性对应一个单元格
   * @returns {boolean} 如果行为空则返回true，否则返回false
   */
  const isRowEmpty = function (row) {
    if (!row) return true;
    const colKeys = Object.keys(row);
    for (let i = 0; i < colKeys.length; i++) {
      const cellValue = row[colKeys[i]]?.v;
      if (cellValue !== undefined && cellValue !== null && cellValue !== "") {
        return false;
      }
    }
    return true;
  };

  // 初始化变量
  let name = null; // 模块标题，表名
  let header = null; // 字段标题
  let columnList = []; // 字段数据
  let result = []; // 结果

  // 遍历行数据
  const rowKeys = Object.keys(cellData);
  for (let i = 0; i < Math.max(...rowKeys); i++) {
    const row = cellData[i];

    // 如果行为空，表示当前部分结束，将当前部分的元信息添加到结果中
    if (isRowEmpty(row)) {
      if (name && header && columnList.length > 0) {
        result.push({
          name: name,
          columnList: columnList,
        });
      }

      name = null;
      header = null;
      columnList = [];
    } else {
      // 如果行不为空，根据当前状态更新name、header或columnList
      // 强制固定结构，相对首行是 表名
      if (isRowEmpty(name)) {
        name = row;
      }
      // 强制固定结构，次行是 字段标题
      else if (isRowEmpty(header)) {
        header = row;
      }
      // 强制规定结构，再者是 数据行
      else {
        let colKeys = Object.keys(header);
        let item = {};
        for (let j = 0; j < colKeys.length; j++) {
          let headerCell = header[colKeys[j]] || {};
          let key = headerCell.v;
          if (key) {
            let rowCell = row[colKeys[j]] || {};
            item[key] = rowCell.v;
          }
        }
        columnList.push(item);
      }
    }
  }

  // 将最后一个部分的元信息添加到结果中
  if (name && header && columnList.length > 0) {
    result.push({
      name: name,
      columnList: columnList,
    });
  }

  // 处理结果，提取表格名称、注释和列信息
  return result.map((item) => {
    let tableName, tableComment;

    // 验证item的name属性，如果不合法则返回空的列信息
    if (!item.name || typeof item.name !== "object") {
      return { columnList: [] };
    }

    const nameKeys = Object.keys(item.name);
    if (nameKeys.length === 0) {
      return { columnList: [] };
    }

    // 提取表格名称和注释
    for (let i = 0; i < nameKeys.length; i++) {
      const nameKey = nameKeys[i];
      let nameVal = item.name[nameKey];
      const obj = extractTokensAndOthers(nameVal.v || "");
      if (!tableName) {
        tableName = obj.tokens.length > 0 ? obj.tokens[0] : "";
      }
      if (!tableComment) {
        tableComment = getLongestAndTrim(obj.others);
      }
    }

    // 提取列信息
    let columnList =
      item.columnList?.map((column) => {
        let data = {};
        const colKeys = Object.keys(column);
        for (let j = 0; j < colKeys.length; j++) {
          const key = colKeys[j];
          const mappedKey = colTransMap[key];
          if (mappedKey) {
            if (["nullable", "autoIncrement", "pk"].includes(mappedKey)) {
              data[mappedKey] = [
                "YES",
                "yes",
                "Y",
                "y",
                "TRUE",
                "true",
                "是",
              ].includes(column[key]);
              if (
                "nullable" === mappedKey &&
                (key.includes("必填") ||
                  key.includes("非空") ||
                  key.includes("不为空"))
              ) {
                data[mappedKey] = !data[mappedKey];
              }
            } else {
              let colVal = column[key];
              if ("typeName" === mappedKey && colVal && colVal.includes("(")) {
                let size = colVal.substring(
                  colVal.indexOf("(") + 1,
                  colVal.indexOf(")")
                );
                colVal = colVal.substring(0, colVal.indexOf("("));
                if (size.includes(",")) {
                  const sd_arr = size.split(",");
                  size = sd_arr[0];
                  if (sd_arr.length > 1) {
                    data["digit"] = Number(sd_arr[1]);
                  }
                }
                data["size"] = Number(size);
              }
              data[mappedKey] = colVal;
            }
          }
        }
        return data;
      }) || [];

    if (columnList.length > 0) {
      // 按照order字段排序
      columnList.sort((a, b) => a.order - b.order);
    }

    // 返回表格元信息
    return {
      tableName: tableName,
      comment: tableComment,
      columnList: columnList,
    };
  });
}
