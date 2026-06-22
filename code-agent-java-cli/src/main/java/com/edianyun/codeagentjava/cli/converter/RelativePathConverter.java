package com.edianyun.codeagentjava.cli.converter;

import com.edianyun.codeagentjava.domain.model.workspace.RelativePath;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Spring Shell 参数转换器，将字符串参数转换为 RelativePath 值对象。
 */
@Component
public class RelativePathConverter implements Converter<String, RelativePath> {

    @Override
    public RelativePath convert(String source) {
        return new RelativePath(source);
    }
}
