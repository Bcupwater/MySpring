package com.ll.service;

import com.ll.springFramework.anotation.Autowired;
import com.ll.springFramework.anotation.Component;
import com.ll.springFramework.anotation.Transactional;

@Component("orderService")
@Transactional
public class OrderService {

    @Autowired
    private UserService userService;

    public void test() {
        System.out.println("依赖注入的数据：" + userService);
    }
}
