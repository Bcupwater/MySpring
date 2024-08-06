package com.ll.springFramework.aware;

import com.ll.springFramework.MyApplicationContext;

public interface ApplicationContextAware {
    void setApplicationContext(MyApplicationContext applicationContext);
}
