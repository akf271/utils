package com.chemao.springboot;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorld {

    @RequestMapping(value="/hello", method = RequestMethod.GET)
    public String index() {
        System.out.println("######");
        System.out.println("####&&&####");
        return "hello world!";
    }
}
