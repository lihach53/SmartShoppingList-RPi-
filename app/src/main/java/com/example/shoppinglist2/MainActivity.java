package com.example.shoppinglist2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    // === RETROFIT ===
    private ShoppingApi api;

    // === UI ===
    private ShoppingItemAdapter adapter;
    private EditText etItemName;
    private EditText etItemNote;
    private Button btnAdd;
    private RecyclerView recyclerView;

    // === IP RASPBERRY PI ===
    private static final String BASE_URL = "http://192.168.1.105:5000/"; // ← ЗАМЕНИ НА СВОЙ IP!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация UI
        etItemName = findViewById(R.id.etItemName);
        etItemNote = findViewById(R.id.etItemNote);
        btnAdd = findViewById(R.id.btnAdd);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShoppingItemAdapter();
        recyclerView.setAdapter(adapter);

        // Инициализация Retrofit
        Gson gson = new GsonBuilder().setLenient().create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        api = retrofit.create(ShoppingApi.class);

        // Загрузка данных с сервера
        loadProductsFromServer();

        // Обработчик добавления
        btnAdd.setOnClickListener(v -> {
            String name = etItemName.getText().toString().trim();
            String note = etItemNote.getText().toString().trim();

            if (!name.isEmpty()) {
                if (!name.matches("[\\p{L}\\p{N}\\s\\-.,]+")) {
                    Toast.makeText(this, "Используйте только буквы, цифры и пробелы", Toast.LENGTH_SHORT).show();
                    return;
                }

                Product product = new Product();
                product.name = name;
                product.notes = note;
                product.purchased = false;

                createProductOnServer(product);
            } else {
                Toast.makeText(this, "Введите название товара", Toast.LENGTH_SHORT).show();
            }
        });

        // Обработчики кликов от адаптера
        adapter.setOnItemClickListener(new ShoppingItemAdapter.OnItemClickListener() {
            @Override
            public void onItemBoughtToggled(ShoppingItem item) {
                // Найдём соответствующий Product по id
                updateProductOnServer(item.id, item.name, item.note, item.isBought);
            }

            @Override
            public void onItemDeleted(ShoppingItem item) {
                deleteProductOnServer(item.id);
            }

            @Override
            public void onItemClicked(ShoppingItem item) {
                showNoteDialog(item);
            }
        });
    }

    // === ЗАГРУЗКА СПИСКА ===
    private void loadProductsFromServer() {
        api.getProducts().enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ShoppingItem> items = new ArrayList<>();
                    for (Product p : response.body()) {
                        ShoppingItem item = new ShoppingItem();
                        item.id = p.id;
                        item.name = p.name;
                        item.note = p.notes;
                        item.isBought = p.purchased;
                        items.add(item);
                    }
                    adapter.setItems(items);
                } else {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e("API", "Ошибка: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Нет связи с сервером", Toast.LENGTH_SHORT).show();
                Log.e("API", "Сетевая ошибка: " + t.getMessage());
            }
        });
    }

    // === ДОБАВЛЕНИЕ ТОВАРА ===
    private void createProductOnServer(Product product) {
        api.createProduct(product).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (response.isSuccessful() && response.body() != null) {
                    etItemName.setText("");
                    etItemNote.setText("");
                    loadProductsFromServer(); // Обновить список
                } else {
                    Toast.makeText(MainActivity.this, "Не удалось добавить товар", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Ошибка сети при добавлении", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // === ОБНОВЛЕНИЕ ТОВАРА ===
    private void updateProductOnServer(int id, String name, String notes, boolean purchased) {
        Product product = new Product();
        product.id = id;
        product.name = name;
        product.notes = notes;
        product.purchased = purchased;

        api.updateProduct(id, product).enqueue(new Callback<Product>() {
            @Override
            public void onResponse(Call<Product> call, Response<Product> response) {
                if (!response.isSuccessful()) {
                    Log.e("API", "Ошибка обновления: " + response.code());
                    loadProductsFromServer(); // Синхронизировать состояние
                }
            }

            @Override
            public void onFailure(Call<Product> call, Throwable t) {
                Log.e("API", "Сетевая ошибка при обновлении: " + t.getMessage());
                loadProductsFromServer(); // Вернуть исходное состояние
            }
        });
    }

    // === УДАЛЕНИЕ ТОВАРА ===
    private void deleteProductOnServer(int id) {
        api.deleteProduct(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Log.e("API", "Ошибка удаления: " + response.code());
                    loadProductsFromServer();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("API", "Сетевая ошибка при удалении: " + t.getMessage());
                loadProductsFromServer();
            }
        });
    }

    // === ДИАЛОГ РЕДАКТИРОВАНИЯ ЗАМЕТКИ ===
    private void showNoteDialog(ShoppingItem item) {
        EditText editText = new EditText(this);
        editText.setText(item.note);
        editText.setMinLines(1);
        editText.setMaxLines(3);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        new AlertDialog.Builder(this)
                .setTitle("Редактировать заметку для: " + item.name)
                .setView(editText)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String newNote = editText.getText().toString().trim();
                    updateProductOnServer(item.id, item.name, newNote, item.isBought);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}