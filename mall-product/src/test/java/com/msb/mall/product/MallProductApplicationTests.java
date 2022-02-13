package com.msb.mall.product;


import com.msb.mall.product.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;


@SpringBootTest
public class MallProductApplicationTests {

    @Autowired
    private CategoryService categoryService;

    @Test
    public void test1(){
        Long[] catelogPath = categoryService.findCatelogPath(762l);
        for (Long aLong : catelogPath) {
            System.out.println(aLong);
        }
    }

}
