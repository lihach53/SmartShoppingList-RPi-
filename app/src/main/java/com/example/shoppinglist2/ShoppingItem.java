package com.example.shoppinglist2;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shopping_items")
public class ShoppingItem {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String note;
    public boolean isBought;

    // ✅ Обязательный конструктор без параметров
    public ShoppingItem() {}

    // Конструктор для удобства
    public ShoppingItem(String name, String note, boolean isBought) {
        this.name = name;
        this.note = note;
        this.isBought = isBought;
    }
}