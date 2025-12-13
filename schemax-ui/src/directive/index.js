import dialogDrag from './dialog/drag'
import dialogDragWidth from './dialog/dragWidth'
import dialogDragHeight from './dialog/dragHeight'
import copyText from './common/copyText'

export default function directive(app) {
  app.directive('copyText', copyText)
  app.directive('dialogDrag', dialogDrag)
  app.directive('dialogDragWidth', dialogDragWidth)
  app.directive('dialogDragHeight', dialogDragHeight)
}
