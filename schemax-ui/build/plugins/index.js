import vue from '@vitejs/plugin-vue'
// import inject from '@rollup/plugin-inject'

import createAutoImport from './auto-import'
import createSvgIcon from './svg-icon'
import createCompression from './compression'
import createSetupExtend from './setup-extend'
// import {univerPlugin} from '@univerjs/vite-plugin'


export default function createVitePlugins(viteEnv, isBuild = false) {
  const vitePlugins = [vue()];
  vitePlugins.push(createAutoImport());
  vitePlugins.push(createSetupExtend());
  vitePlugins.push(createSvgIcon(isBuild));
  // vitePlugins.push(univerPlugin())
  isBuild && vitePlugins.push(...createCompression(viteEnv));
  // vitePlugins.push(
  //   inject({
  //     $: "jquery",
  //     jQuery: "jquery",
  //     CodeMirror: "codemirror",
  //   })
  // );
  return vitePlugins;
}
