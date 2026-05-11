package com.kanade.backend.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.kanade.backend.model.entity.Question;

public class NormalizationUtils {

    public static String normalizeText(String text) {
        if (StrUtil.isBlank(text)) {
            return "";
        }
        return text.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\r\\n|\\r", "\n");
    }

    public static String normalizeOptions(String optionsJson) {
        if (StrUtil.isBlank(optionsJson)) {
            return "";
        }
        try {
            var parsed = JSONUtil.parse(optionsJson);
            return parsed.toString();
        } catch (Exception e) {
            return normalizeText(optionsJson);
        }
    }

    public static String generateCompositeMd5(Question question) {
        String content = normalizeText(question.getContent());
        String type = question.getType() != null ? question.getType().toString() : "";
        String options = normalizeOptions(question.getOptions());
        String answer = normalizeText(question.getAnswer());
        String combined = content + "|" + type + "|" + options + "|" + answer;
        return DigestUtil.md5Hex(combined);
    }
}
