<template>
  <div ref="univerContainer" class="univer-container">
  </div>
</template>

<script>
import '@univerjs/sheets-ui/lib/index.css'
import {LocaleType, Univer, UniverInstanceType} from '@univerjs/core'
import {defaultTheme} from '@univerjs/design'
import {UniverDocsPlugin} from '@univerjs/docs'
import {UniverDocsUIPlugin} from '@univerjs/docs-ui'
import {UniverFormulaEnginePlugin} from '@univerjs/engine-formula'
import {UniverRenderEnginePlugin} from '@univerjs/engine-render'
import {UniverSheetsPlugin} from '@univerjs/sheets'
import {UniverSheetsFormulaPlugin} from '@univerjs/sheets-formula'
import {UniverSheetsUIPlugin} from '@univerjs/sheets-ui'
import {UniverUIPlugin} from '@univerjs/ui'
import {UniverFindReplacePlugin} from '@univerjs/find-replace';
import {UniverSheetsFindReplacePlugin} from '@univerjs/sheets-find-replace';
import {UniverSheetsFilterPlugin} from '@univerjs/sheets-filter';
import {UniverSheetsFilterUIPlugin} from '@univerjs/sheets-filter-ui';
import {FUniver} from '@univerjs/facade'
import {enUS, zhCN} from 'univer:locales'
import XEUtils from "xe-utils";
import {DEFAULT_WORKBOOK_DATA} from "./sheet-data";

export default {
  name: 'UniverSheet',
  props: {
    workbookData: {
      type: Object,
      default: () => {
        return {...DEFAULT_WORKBOOK_DATA}
      }
    }
  },
  watch: {
    workbookData: {
      handler(val) {
        this.destroyUniver();
        this.init();
      },
      immediate: true
    }
  },
  data() {
    return {
      univer: null,
      workbook: null,
      univerAPI: null
    }
  },
  mounted() {
    // this.init();
  },
  methods: {
    init: XEUtils.debounce(function () {
      if (!this.univer) {
        const univer = new Univer({
          theme: defaultTheme,
          locale: LocaleType.ZH_CN,
          locales: {
            [LocaleType.ZH_CN]: zhCN,
            [LocaleType.EN_US]: enUS
          }
        });
        univer.registerPlugin(UniverRenderEnginePlugin);
        univer.registerPlugin(UniverFormulaEnginePlugin);
        univer.registerPlugin(UniverUIPlugin, {
          container: this.$refs.univerContainer
        });
        univer.registerPlugin(UniverDocsPlugin, {
          hasScroll: false
        });
        univer.registerPlugin(UniverDocsUIPlugin);
        univer.registerPlugin(UniverSheetsPlugin);
        univer.registerPlugin(UniverSheetsUIPlugin);
        univer.registerPlugin(UniverSheetsFormulaPlugin);
        univer.registerPlugin(UniverFindReplacePlugin);
        univer.registerPlugin(UniverSheetsFindReplacePlugin);
        univer.registerPlugin(UniverSheetsFilterPlugin);
        univer.registerPlugin(UniverSheetsFilterUIPlugin);
        this.univer = univer;
        this.univerAPI = FUniver.newAPI(this.univer);
      }
      console.log(this.workbookData);
      this.workbook = this.univer.createUnit(UniverInstanceType.UNIVER_SHEET, this.workbookData);
    }, 200),

    destroyUniver() {
      this.univerAPI?.disposeUnit("workbook-01");
      this.workbook?.dispose();
      this.workbook = null;
    },

    getData() {
      if (!this.workbook) {
        throw new Error('未初始化')
      }
      return this.workbook.save()
    },
  },
  destroyed() {
    this.destroyUniver();
    this.univer?.dispose()
    this.univer = null;
    this.univerAPI = null;
  }
}
</script>

<style scoped>
.univer-container {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

:global(.univer-menubar) {
  display: none;
}
</style>

