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
    // ЗАМЕНИТЕ НА РЕАЛЬНЫЙ IP АДРЕС ВАШЕГО RASPBERRY PI!
    // Пример: "http://192.168.1.100:5000/"
    // Для эмулятора можно использовать "http://10.0.2.2:5000/"
    private static final String BASE_URL = "http://10.0.2.2:5000/";

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

        // Инициализация Retrofit с правильным URL
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        api = retrofit.create(ShoppingApi.class);

        // Загрузка данных с сервера
        loadProductsFromServer();

        // Обработчик добавления
        btnAdd.setOnClickListener(v -> {
            String name = etItemName.getText().toString().trim();
            String note = etItemNote.getText().toString().trim();

            if (!name.isEmpty()) {
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
        api.getProducts().enqueue(new Callback<ProductListResponse>() {
            @Override
            public void onResponse(Call<ProductListResponse> call, Response<ProductListResponse> response) {
                Log.d("API", "Response code: " + response.code());

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    ProductListResponse apiResponse = response.body();
                    Product[] products = apiResponse.data;

                    if (products != null) {
                        List<ShoppingItem> items = new ArrayList<>();
                        for (Product p : products) {
                            ShoppingItem item = new ShoppingItem();
                            item.id = p.id;
                            item.name = p.name;
                            item.note = p.notes;
                            item.isBought = p.purchased;

                            // Передаем даты с сервера
                            if (p.created_at != null) {
                                item.createdAt = p.created_at;
                            } else {
                                item.createdAt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                        .format(new java.util.Date());
                            }

                            // Используем updated_at если есть, иначе created_at
                            if (p.updated_at != null) {
                                item.updatedAt = p.updated_at;
                            } else if (p.created_at != null) {
                                item.updatedAt = p.created_at;
                            } else {
                                item.updatedAt = item.createdAt;
                            }

                            items.add(item);
                        }
                        adapter.setItems(items);
                        Toast.makeText(MainActivity.this, "Загружено " + items.size() + " товаров", Toast.LENGTH_SHORT).show();
                        Log.d("API", "Successfully loaded " + items.size() + " items");
                    } else {
                        Toast.makeText(MainActivity.this, "Нет товаров", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                        Log.e("API", "Ошибка загрузки: " + response.code() + " | " + errorBody);
                        Toast.makeText(MainActivity.this, "Ошибка загрузки: " + response.code(), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ProductListResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Нет связи с сервером: " + t.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("API", "Сетевая ошибка: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    // === ДОБАВЛЕНИЕ ТОВАРА ===
    private void createProductOnServer(Product product) {
        api.createProduct(product).enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                Log.d("API", "Create response code: " + response.code());

                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Product createdProduct = response.body().data;
                    etItemName.setText("");
                    etItemNote.setText("");
                    Toast.makeText(MainActivity.this, "Товар добавлен: " + createdProduct.name, Toast.LENGTH_SHORT).show();
                    loadProductsFromServer(); // Обновить список
                } else {
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "null";
                        Log.e("API", "Ошибка добавления: " + response.code() + " | " + errorBody);
                        Toast.makeText(MainActivity.this, "Ошибка: " + response.code(), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Сетевая ошибка при добавлении", Toast.LENGTH_SHORT).show();
                Log.e("API", "Ошибка сети: " + t.getMessage());
                t.printStackTrace();
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

        api.updateProduct(id, product).enqueue(new Callback<ProductResponse>() {
            @Override
            public void onResponse(Call<ProductResponse> call, Response<ProductResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Log.d("API", "Товар обновлен: " + id);
                } else {
                    Log.e("API", "Ошибка обновления: " + response.code());
                }
                loadProductsFromServer(); // Синхронизировать состояние
            }

            @Override
            public void onFailure(Call<ProductResponse> call, Throwable t) {
                Log.e("API", "Сетевая ошибка при обновлении: " + t.getMessage());
                t.printStackTrace();
                loadProductsFromServer(); // Вернуть исходное состояние
            }
        });
    }

    // === УДАЛЕНИЕ ТОВАРА ===
    private void deleteProductOnServer(int id) {
        api.deleteProduct(id).enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().success) {
                    Toast.makeText(MainActivity.this, "Товар удален", Toast.LENGTH_SHORT).show();
                    loadProductsFromServer();
                } else {
                    Log.e("API", "Ошибка удаления: " + response.code());
                    loadProductsFromServer();
                }
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable t) {
                Log.e("API", "Сетевая ошибка при удалении: " + t.getMessage());
                t.printStackTrace();
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