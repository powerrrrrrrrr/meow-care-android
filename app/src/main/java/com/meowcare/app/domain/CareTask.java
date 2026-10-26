package com.meowcare.app.domain;

public final class CareTask {
    private final long id;
    private final String title;
    private final String item;
    private final int intervalDays;
    private final boolean remind;
    private final boolean active;

    public CareTask(long id, String title, String item, int intervalDays, boolean remind, boolean active) {
        this.title = requireText(title, "任务名称");
        this.item = requireText(item, "物品名称");
        CareRules.validateInterval(intervalDays);
        this.id = id;
        this.intervalDays = intervalDays;
        this.remind = remind;
        this.active = active;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(field + "不能为空");
        return value.trim();
    }

    public long id() { return id; }
    public String title() { return title; }
    public String item() { return item; }
    public int intervalDays() { return intervalDays; }
    public boolean remind() { return remind; }
    public boolean active() { return active; }
}
