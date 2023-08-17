package org.originit.print;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.originit.analyze.print.anno.Skip;
import org.originit.print.type.filter.ParentTypeFilter;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Entry {

    @ToString
    static class Info {

        private String name;

        private long time;

        public Info(String name, long time) {
            this.name = name;
            this.time = time;
        }
    }

    public static void main(String[] args) {
        String basePackage = "org.originit.print.impl";

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        AbstractNumberPrint.maxNumber = 1000000;
        // 添加过滤器，这里以扫描带有特定注解的类为例
        scanner.addIncludeFilter(new ParentTypeFilter(NumberPrint.class));
        scanner.addExcludeFilter(new AnnotationTypeFilter(Skip.class));

        // 执行扫描
        Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);
        // 处理扫描结果
        candidates.stream().map(beanDefinition -> {
            try {
                return Class.forName(beanDefinition.getBeanClassName());
            } catch (ClassNotFoundException e) {
                log.info("get class error, name is {}", beanDefinition.getBeanClassName());
                return null;
            }
        }).filter(Objects::nonNull).map(aClass -> {
            try {
                return (NumberPrint)aClass.newInstance();
            } catch (Exception e) {
               log.info("create bean error, class name is {}", aClass.getSimpleName());
               return null;
            }
        }).filter(Objects::nonNull)
                .map(o -> {
                    return new Info(o.getClass().getName(), o.run());
                }).collect(Collectors.toList()).stream()
                .sorted(Comparator.comparingLong(o -> o.time)).forEach(info -> {
           log.info("{} used {} ms\r\n", info.name, info.time);
        });
    }
}
