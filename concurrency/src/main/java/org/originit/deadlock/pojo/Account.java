package org.originit.deadlock.pojo;

import java.math.BigDecimal;

public class Account implements Cloneable {

    private Integer id;

    private volatile BigDecimal surplus;

    public Account(Integer id, BigDecimal surplus) {
        this.id = id;
        this.surplus = surplus;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getSurplus() {
        return surplus;
    }

    public void setSurplus(BigDecimal surplus) {
        this.surplus = surplus;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "Account{" +
                "id=" + id +
                ", surplus=" + surplus +
                '}';
    }
}
