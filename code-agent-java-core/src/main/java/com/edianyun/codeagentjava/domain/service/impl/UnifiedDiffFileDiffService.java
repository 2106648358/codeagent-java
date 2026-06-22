package com.edianyun.codeagentjava.domain.service.impl;

import com.edianyun.codeagentjava.domain.service.FileDiffService;

import java.util.Arrays;
import java.util.List;

/**
 * Unified diff 格式的文件差异计算服务实现。
 * 使用简单的行级别比较算法生成类似 git diff 的输出格式。
 */
public class UnifiedDiffFileDiffService implements FileDiffService {

    @Override
    public String diff(String oldContent, String newContent) {
        String oldStr = oldContent == null ? "" : oldContent;
        String newStr = newContent == null ? "" : newContent;
        if (oldStr.equals(newStr)) {
            return "No changes.";
        }
        List<String> oldLines = Arrays.asList(oldStr.split("\\r?\\n", -1));
        List<String> newLines = Arrays.asList(newStr.split("\\r?\\n", -1));
        StringBuilder sb = new StringBuilder();
        sb.append("--- old\n");
        sb.append("+++ new\n");
        int oldSize = oldLines.size();
        int newSize = newLines.size();
        int i = 0, j = 0;
        while (i < oldSize || j < newSize) {
            if (i < oldSize && j < newSize && oldLines.get(i).equals(newLines.get(j))) {
                sb.append(" ").append(oldLines.get(i)).append("\n");
                i++;
                j++;
            } else if (i < oldSize && (j >= newSize || !containsFrom(newLines, j, oldLines.get(i)))) {
                sb.append("-").append(oldLines.get(i)).append("\n");
                i++;
            } else if (j < newSize) {
                sb.append("+").append(newLines.get(j)).append("\n");
                j++;
            }
        }
        return sb.toString();
    }

    private boolean containsFrom(List<String> lines, int from, String line) {
        for (int k = from; k < lines.size(); k++) {
            if (lines.get(k).equals(line)) {
                return true;
            }
        }
        return false;
    }
}
