package org.zefrenchwan.bigdata;

import java.io.Serializable;

public class YearSalaryTuple implements Serializable {

    private static final long serialVersionUID = 1L;

    private int year;
    private float salary;

    public int getYear() {
        return year;
    }
    public void setYear(int year) {
        this.year = year;
    }
    public float getSalary() {
        return salary;
    }
    public void setSalary(float salary) {
        this.salary = salary;
    }
    @Override
    public String toString() {
        return "YearSalaryTuple [year=" + year + ", salary=" + salary + "]";
    }

    
}
