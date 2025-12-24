package com.example.shoppinglist2;

public class Product {
    public int id;
    public String name;
    public boolean purchased;
    public String notes;
    public String created_at;
    public String updated_at;

    // Конструктор по умолчанию
    public Product() {}

    // Конструктор для удобства
    public Product(String name, boolean purchased, String notes) {
        this.name = name;
        this.purchased = purchased;
        this.notes = notes;
    }
}