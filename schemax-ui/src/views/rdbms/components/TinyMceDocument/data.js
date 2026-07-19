export const createDefaultTinyWordData = () => ({
  version: 2,
  editor: 'tinymce',
  format: 'html',
  bodyHtml: '',
  page: {
    size: 'A4',
    orientation: 'portrait',
    widthMm: 210,
    heightMm: 297,
    margins: {top: 20, bottom: 20, left: 25, right: 25, header: 10, footer: 10},
    differentFirstPage: false,
    differentOddEven: false,
    fontFamily: 'Microsoft YaHei',
    fontSize: 10.5,
  },
  headers: {},
  footers: {},
})
