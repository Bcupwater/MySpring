package com.ll;

import com.ll.config.AppConfig;
import com.ll.service.OrderService;
import com.ll.springFramework.MyApplicationContext;

public class MyApplication {
    public static void main(String[] args) {

        // 根据配置类，创建applicationContext容器
        MyApplicationContext applicationContext = new MyApplicationContext(AppConfig.class);
        OrderService orderService = (OrderService) applicationContext.getBean("orderService");
        orderService.test();
    }
}
