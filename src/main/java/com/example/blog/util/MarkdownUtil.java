package com.example.blog.util;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

/**
 * Markdown 원문을 HTML로 변환한다.
 * 글 저장 시점에 content_md -> content_html 로 미리 변환해 두면 조회가 빨라진다.
 */
public final class MarkdownUtil {

    private static final Parser PARSER;
    private static final HtmlRenderer RENDERER;

    static {
        MutableDataSet options = new MutableDataSet();
        PARSER = Parser.builder(options).build();
        RENDERER = HtmlRenderer.builder(options).build();
    }

    private MarkdownUtil() {}

    public static String toHtml(String markdown) {
        if (markdown == null) return "";
        return RENDERER.render(PARSER.parse(markdown));
    }
}
