import {BooleanNumber, LocaleType, SheetTypes} from '@univerjs/core';

export const DEFAULT_SHEET_DATA = {
  id: 'sheet-01',
  type: SheetTypes.GRID,
  name: '表结构',
  tabColor: 'red',
  hidden: BooleanNumber.FALSE,
  rowCount: 1000,
  columnCount: 20,
  zoomRatio: 1,
  scrollTop: 200,
  scrollLeft: 100,
  defaultColumnWidth: 100,
  defaultRowHeight: 25,
  status: 1,
  showGridlines: 1,
  hideRow: [],
  hideColumn: [],
  rowHeader: {
    width: 45,
    hidden: BooleanNumber.FALSE,
  },
  columnHeader: {
    height: 20,
    hidden: BooleanNumber.FALSE,
  },
  // selections: ['A2'],
  rightToLeft: BooleanNumber.FALSE,
  pluginMeta: {},
  // cellData: {
  //   '0': {
  //     '0': {
  //       v: 'Hello World',
  //     },
  //   },
  // },
  columnData: {
    0: {
      w: 65,
      hd: 0
    },
    1: {
      w: 150,
      hd: 0
    },
    2: {
      w: 100,
      hd: 0
    },
    3: {
      w: 80,
      hd: 0
    },
    4: {
      w: 80,
      hd: 0
    },
    5: {
      w: 80,
      hd: 0
    },
    6: {
      w: 80,
      hd: 0
    },
    7: {
      w: 80,
      hd: 0
    },
    8: {
      w: 200,
      hd: 0
    }
  },
}

/**
 * Default workbook data
 * @type {IWorkbookData} document see https://univer.work/api/core/interfaces/IWorkbookData.html
 */
export const DEFAULT_WORKBOOK_DATA = {
  id: 'workbook-01',
  locale: LocaleType.ZH_CN,
  name: 'universheet',
  sheetOrder: ['sheet-01'],
  appVersion: '3.0.0-alpha',
  sheets: {
    'sheet-01': DEFAULT_SHEET_DATA
  },
};

