import {copyTextToClipboard} from "@/utils/rdbms";

/**
 * v-copyText 复制文本内容
 * Copyright (c) 2022 ruoyi
 */
export default {
  beforeMount(el, {value, arg}) {
    if (arg === "callback") {
      el.$copyCallback = value;
    } else {
      el.$copyValue = value;
      const handler = () => {
        copyTextToClipboard(el.$copyValue);
        if (el.$copyCallback) {
          el.$copyCallback(el.$copyValue);
        }
      };
      el.addEventListener("click", handler);
      el.$destroyCopy = () => el.removeEventListener("click", handler);
    }
  }
}
