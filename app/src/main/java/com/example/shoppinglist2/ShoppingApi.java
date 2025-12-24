package com.example.shoppinglist2;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface ShoppingApi {
    @GET("api/products")
    Call<ProductListResponse> getProducts();

    @POST("api/products")
    Call<ProductResponse> createProduct(@Body Product product);

    @PUT("api/products/{id}")
    Call<ProductResponse> updateProduct(@Path("id") int id, @Body Product product);

    @DELETE("api/products/{id}")
    Call<BasicResponse> deleteProduct(@Path("id") int id);
}

// Класс для ответа со списком товаров
class ProductListResponse {
    public boolean success;
    public int count;
    public Product[] data;
    public String timestamp;
}

// Класс для ответа с одним товаром
class ProductResponse {
    public boolean success;
    public String message;
    public Product data;
}

// Класс для простых ответов
class BasicResponse {
    public boolean success;
    public String message;
}