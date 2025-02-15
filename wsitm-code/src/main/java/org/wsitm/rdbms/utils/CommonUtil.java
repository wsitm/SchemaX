package org.wsitm.rdbms.utils;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {


    public static void renderFile(HttpServletResponse response, File file)
            throws IOException {
        if (file == null || !file.isFile()) {
            throw new IOException("文件不存在");
        }
        // ---------
        response.setHeader("Accept-Ranges", "bytes");
        String fileName = URLEncoder.encode(file.getName(), "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        response.setContentType("application/octet-stream");

        response.setHeader("Content-Length", String.valueOf(file.length()));

        try (
                FileInputStream fis = new FileInputStream(file);
                InputStream inputStream = new BufferedInputStream(fis);
                OutputStream outputStream = response.getOutputStream();
        ) {
            byte[] buffer = new byte[2048];
            for (int len = -1; (len = inputStream.read(buffer)) != -1; ) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
        } catch (IOException e) {
            // ClientAbortException、EofException 直接或间接继承自 IOException
            String name = e.getClass().getSimpleName();
            if (!"ClientAbortException".equals(name) && !"EofException".equals(name)) {
                throw new IOException(e);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static boolean matchAnyIgnoreCase(final String str, final String... matchs) {
        if (StrUtil.isEmpty(str) || ArrayUtil.isEmpty(matchs)) {
            return false;
        }

        for (String match : matchs) {
            boolean flag = !StrUtil.startWith(match, "!");
            if (!flag) {
                match = match.substring(1);
            }
            if (str.equalsIgnoreCase(match)) {
                return flag;
            }
            if (StrUtil.containsAny(match, "*", "?")
                    && isMatch(str, match, true)) {
                return flag;
            }
        }
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
}
