package org.example;

public class Event {
    private Integer row;
    private String concept;
    private int org;
    private long time;
    private String activity;
    private int resource;
    private String lifecycle;
    private Integer id;
    private String caseConcept;
    private String timestamp;

    public Event(){}

    Event(Integer r, String c, int o, long t, String a, int re, String l, Integer i, String ca, String ti) {
        this.row = r;
        this.concept = c;
        this.org = o;
        this.time = t;
        this.activity = a;
        this.resource = re;
        this.lifecycle = l;
        this.id = i;
        this.caseConcept = ca;
        this.timestamp = ti;
    }

    public Integer getRow() {
        return row;
    }

    public String getConcept() {
        return concept;
    }

    public int getOrg() {
        return org;
    }

    public long getTime() {
        return time;
    }

    public String getActivity() {
        return activity;
    }

    public int getResource() {
        return resource;
    }

    public String getLifecycle() {
        return lifecycle;
    }

    public Integer getId() {
        return id;
    }

    public String getCaseConcept() {
        return caseConcept;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String toString() {
        return this.row.toString() + " " + this.concept.toString() + " ID"+this.id.toString();
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public void setOrg(int org) {
        this.org = org;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public void setResource(int resource) {
        this.resource = resource;
    }

    public void setLifecycle(String lifecycle) {
        this.lifecycle = lifecycle;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setCaseConcept(String caseConcept) {
        this.caseConcept = caseConcept;
    }
}
