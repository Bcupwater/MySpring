package com.ll.springFramework;

public class BeanDefinition {

    /**
     * bean的类型
     */
    private Class type;

    /**
     * bean的作用域
     */
    private String scope;

    /**
     * 是否懒加载
     */
    private boolean isLazy;

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isLazy() {
        return isLazy;
    }

    public void setLazy(boolean lazy) {
        isLazy = lazy;
    }
}
