package org.wsitm.schemax.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {
    private static final Logger log = LoggerFactory.getLogger(CommonUtil.class);


    public static void renderFile(HttpServletResponse response, File file) throws IOException {
        if (file == null || !file.isFile()) {
            throw new IOException("文件不存在");
        }
        // -- 配置响应头 -------
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode(file.getName(), "UTF-8"));
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Length", String.valueOf(file.length()));

        try (
                InputStream inputStream = FileUtil.getInputStream(file);
                OutputStream outputStream = response.getOutputStream();
        ) {
            IoUtil.copy(inputStream, outputStream);
        } catch (IOException e) {
            log.error("文件传输失败: " + e.getMessage());
            throw e; // 重新抛出异常以便调用方处理
        }
    }

    /**
     * 忽略大小写匹配字符串
     * 此方法用于检查给定的字符串是否与一系列模式字符串中的任何一个匹配，不考虑大小写
     * 模式字符串可以使用通配符 "*" 和 "?"
     *
     * @param str    要匹配的目标字符串
     * @param matchs 一个或多个模式字符串，用于与目标字符串进行匹配
     * @return 如果目标字符串与任何一个模式字符串匹配，则返回true；否则返回false
     */
    public static boolean matchAnyIgnoreCase(final String str, final String... matchs) {
        // 检查目标字符串或模式字符串数组是否为空，如果任一为空，则直接返回false
        if (StrUtil.isEmpty(str) || ArrayUtil.isEmpty(matchs)) {
            return false;
        }

        // 遍历模式字符串数组，逐个与目标字符串进行匹配
        for (String match : matchs) {
            // 判断当前模式字符串是否以"!"开头，如果是，则表示这是一个否定匹配
            boolean flag = !StrUtil.startWith(match, "!");
            // 如果是否定匹配，则去除"!"继续处理
            if (!flag) {
                match = match.substring(1);
            }
            // 如果目标字符串与模式字符串（不考虑大小写）完全相同，则根据是否是否定匹配返回结果
            if (str.equalsIgnoreCase(match)) {
                return flag;
            }
            // 如果模式字符串中包含通配符"*"或"?"，则使用自定义的匹配逻辑进行模糊匹配
            if (StrUtil.containsAny(match, "*", "?")
                    && isMatch(str, match, true)) {
                return flag;
            }
        }
        // 如果没有找到任何匹配，则返回false
        return false;
    }


    /**
     * 通配符匹配
     *
     * @param s 字符串
     * @param p 匹配字符串
     * @return 结果
     */
    public static boolean isMatch(String s, String p) {
        return isMatch(s, p, false);
    }

    /**
     * 通配符匹配
     *
     * @param s          字符串
     * @param p          匹配字符串
     * @param ignoreCase 是否忽略大小写
     * @return 结果
     */
    public static boolean isMatch(String s, String p, boolean ignoreCase) {
        if (!StrUtil.isAllNotEmpty(s, p)) {
            return false;
        }
        if (ignoreCase) {
            s = s.toLowerCase();
            p = p.toLowerCase();
        }
        int n = s.length(), m = p.length();
        boolean[][] judges = new boolean[n + 1][m + 1];

        judges[0][0] = true;  // 空字符串和空模式匹配

        for (int i = 1; i <= m; i++) {
            if (p.charAt(i - 1) == '*') {
                judges[0][i] = judges[0][i - 1];
            }
        }

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                char a = s.charAt(i - 1);
                char b = p.charAt(j - 1);
                if (b == '?' || a == b) {
                    judges[i][j] = judges[i - 1][j - 1];
                } else if (b == '*') {
                    judges[i][j] = judges[i][j - 1] || judges[i - 1][j];
                } else {
                    judges[i][j] = false;
                }
            }
        }
        return judges[n][m];
    }

    /**
     * 替换单引号包含的内容
     *
     * @param input       原字符串
     * @param searchStr   被查找的字符串
     * @param replacement 被替换的字符串
     * @return 替换后的字符串
     */
    public static String replaceInQuotes(String input, String searchStr, String replacement) {
        // 定义正则表达式，匹配单引号内的内容
        Pattern pattern = Pattern.compile("'([^']*)'");
        Matcher matcher = pattern.matcher(input);

        // 使用 StringBuilder 来构建替换后的字符串
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0; // 上一个匹配结束的位置

        while (matcher.find()) {
            // 添加匹配前的内容到 StringBuilder
            sb.append(input, lastEnd, matcher.start());
            // 获取匹配的内容，并替换其中的分号
            String quotedContent = matcher.group(1);
            String replacedContent = quotedContent.replace(searchStr, replacement);
            // 添加替换后的内容到 StringBuilder
            sb.append("'").append(replacedContent).append("'");
            // 更新上一个匹配结束的位置
            lastEnd = matcher.end();
        }

        // 添加最后一个匹配之后的内容（如果有）
        sb.append(input.substring(lastEnd));

        return sb.toString();
    }

    /**
     * 判断字符串中是否包含数字
     * @param str 待检查的字符串
     * @return 是否包含数字
     */
    public static boolean containsDigit(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.matches(".*\\d.*"); // 匹配任意位置的数字
    }

    public static String[] dealStipStrArr(String[] skipStrArr){
        skipStrArr = ArrayUtil.removeEmpty(skipStrArr);
        if (ArrayUtil.isEmpty(skipStrArr)) {
            skipStrArr = new String[]{"*"};
        }
        Arrays.sort(skipStrArr, (s1, s2) -> {
            boolean b1 = StrUtil.startWith(s1, "!");
            boolean b2 = StrUtil.startWith(s2, "!");
            if (b1 && b2) return 0;
            if (b1) return -1;
            if (b2) return 1;
            return 0;
        });
        return skipStrArr;
    }
}
