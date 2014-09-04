package com.joshtttt.library.network;

import android.text.TextUtils;

public class HtmlUtil {
	public static String htmlDecode(String content) {
		if (content == null)
			return "";
		String html = content;
		if (!TextUtils.isEmpty(content)) {
			html = html.replace("'", "&apos;");
			html = html.replace("\"", "&quot;");
			html = html.replace("\t", "&nbsp;&nbsp;");// 替换跳格
			html = html.replace("<", "&lt;");
			html = html.replace(">", "&gt;");
			html = html.replace("\r\n", "");
			html = html.replace("\n", "");
			html = html.replace("\\\\", "&#92;&#92;");
			html = html.replace("\\", "&#92;");
		}
		return html;
	}
	
	/**
     * 
     * @param content
     * @return
     */
    public static String htmlCode(String content) {
        if (content == null)
            return "";
        String html = content;
		if (!TextUtils.isEmpty(content)) {
			html = html.replace("&apos;", "'");
			html = html.replace("&quot;", "\"");
			html = html.replace("&nbsp; &nbsp;", "\t");// 替换跳格
			html = html.replace("&lt;", "<");
			html = html.replace("&gt;", ">");

			html = html.replace("&#92;&#92;", "\\\\");
			html = html.replace("&#92;", "\\");
		}
		return html;
    }
}
