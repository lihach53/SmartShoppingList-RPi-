package com.example.shoppinglist2;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items ORDER BY id DESC")
    List<ShoppingItem> getAllItems();

    @Insert
    void insert(ShoppingItem item);

    @Update
    void update(ShoppingItem item);

    @Delete
    void delete(ShoppingItem item);
}