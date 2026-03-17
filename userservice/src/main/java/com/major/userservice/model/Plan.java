package com.major.userservice.model;

public enum Plan {
    FREE(5,0.1),
    PRO(20,1.0),
    PREMIUM(50,2.0);

    private final int capacity;
    private final double refillRate;

    Plan(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
    }

    public int getCapacity() { return capacity; }
    public double getRefillRate() { return refillRate; }
}
