package org.originit.print.type.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;

@Slf4j
public class ParentTypeFilter implements TypeFilter {

    private final Class type;

    public ParentTypeFilter(Class type) {
        assert type != null;
        this.type = type;
    }

    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) throws
            IOException {
        try {
            Class parent = Class.forName(metadataReader.getClassMetadata().getSuperClassName());
            if (this.type.isAssignableFrom(parent)) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            log.info("skip type {} because of error: {}", metadataReader.getClassMetadata().getClassName(), e.getMessage());
        }

        return false;
    }
}
