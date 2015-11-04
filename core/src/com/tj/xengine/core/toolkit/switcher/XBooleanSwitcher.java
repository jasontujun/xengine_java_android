package com.tj.xengine.core.toolkit.switcher;

/**
 * <pre>
 * 用一个boolean值实现的开关类，实现XSwitcher的接口。
 * User: jasontujun
 * Date: 14-8-18
 * Time: 下午8:12
 * </pre>
 */
public final class XBooleanSwitcher implements XSwitcher {

    private volatile boolean on;

    public XBooleanSwitcher(boolean on) {
        this.on = on;
    }

    @Override
    public boolean isOn() {
        return on;
    }

    public synchronized void turnOn() {
        on = true;
    }

    public synchronized void turnOff() {
        on = false;
    }
}
