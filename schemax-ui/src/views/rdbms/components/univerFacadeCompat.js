import {FUniver} from '@univerjs/core/facade'
import '@univerjs/sheets-ui/lib/facade'

const PATCH_MARK = Symbol.for('schemax.univer.sheets-ui-observer-patched')

export function applyUniverFacadeCompatibilityPatch() {
  const prototype = FUniver.prototype
  if (prototype[PATCH_MARK] || typeof prototype._initObserverListener !== 'function') return

  // Univer 0.25.1 会让 Sheets 门面监听已销毁实例的生命周期，导致 Excel/Word 切换时报依赖缺失。
  Object.defineProperty(prototype, '_initObserverListener', {
    configurable: true,
    writable: true,
    value() {},
  })
  Object.defineProperty(prototype, PATCH_MARK, {value: true})
}
