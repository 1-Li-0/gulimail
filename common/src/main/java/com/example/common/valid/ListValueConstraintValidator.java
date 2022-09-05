package com.example.common.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

/**
 * 自定义校验器
 * ConstraintValidator<A extends Annotation, T> A指定注解，T指定校验的数据类型
 */
public class ListValueConstraintValidator implements ConstraintValidator<ListValue, Integer> {
    //收集数据
    private Set<Integer> set = new HashSet<>();

    //初始化方法
    @Override
    public void initialize(ListValue constraintAnnotation) {
        int[] values = constraintAnnotation.values();
        for (Integer i : values) {
            set.add(i);
        }
    }

    //判断需要进行校验的数据 (Integer i) 的数据类型，false表示校验失败
    @Override
    public boolean isValid(Integer i, ConstraintValidatorContext constraintValidatorContext) {
        if (set.contains(i)) {
            return true;
        } else return false;
    }
}
