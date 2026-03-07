package com.kanade.backend.ai.parser;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.kanade.backend.ai.model.LabelResult;

public class JsonParser {

    // 匹配 ```json 开头 和 ``` 结尾 的正则
    private static final String MARKDOWN_PREFIX = "```json\n";
    private static final String MARKDOWN_SUFFIX = "```";

    /**
     * 解析带 Markdown 代码块的 JSON 字符串 → LabelResult 对象
     * @param rawJson 原始字符串（带```json```）
     * @return 解析后的对象
     */
    public static LabelResult parse(String rawJson) {
        // 1. 清理字符串：移除 Markdown 代码块标记
        String cleanJson = cleanMarkdownCodeBlock(rawJson);

        // 2. Hutool 核心解析：自动映射别名，直接转 Java 对象
        return JSONUtil.toBean(cleanJson, LabelResult.class);
    }

    /**
     * 清理 Markdown 代码块标记（```json ... ```）
     */
    private static String cleanMarkdownCodeBlock(String rawJson) {
        if (StrUtil.isBlank(rawJson)) {
            return StrUtil.EMPTY;
        }

        // 移除前缀和后缀，trim 去除多余空格/换行
        return rawJson
                .replace(MARKDOWN_PREFIX, StrUtil.EMPTY)
                .replace(MARKDOWN_SUFFIX, StrUtil.EMPTY)
                .trim();
    }
}
