//package com.example.shoppinglist2;
//
//import android.text.TextUtils;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.CheckBox;
//import android.widget.TextView;
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//import com.google.android.material.button.MaterialButton;
//import java.util.ArrayList;
//import java.util.List;
//
//public class ShoppingItemAdapter extends RecyclerView.Adapter<ShoppingItemAdapter.ViewHolder> {
//
//    private List<ShoppingItem> items = new ArrayList<>();
//    private OnItemClickListener listener;
//
//    public interface OnItemClickListener {
//        void onItemBoughtToggled(ShoppingItem item);
//        void onItemDeleted(ShoppingItem item);
//        void onItemClicked(ShoppingItem item); // для редактирования заметки
//    }
//
//    public void setOnItemClickListener(OnItemClickListener listener) {
//        this.listener = listener;
//    }
//
//    public void setItems(List<ShoppingItem> items) {
//        this.items = items;
//        notifyDataSetChanged();
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_shopping, parent, false);
//        return new ViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        ShoppingItem item = items.get(position);
//        holder.textView.setText(item.name);
//
//        if (!TextUtils.isEmpty(item.note)) {
//            holder.textNote.setText(item.note);
//            holder.textNote.setVisibility(View.VISIBLE);
//        } else {
//            holder.textNote.setVisibility(View.GONE);
//        }
//
//        holder.checkBox.setChecked(item.isBought);
//    }
//
//    @Override
//    public int getItemCount() {
//        return items.size();
//    }
//
//    class ViewHolder extends RecyclerView.ViewHolder {
//        CheckBox checkBox;
//        TextView textView;
//        TextView textNote;
//        MaterialButton btnDelete;
//
//        ViewHolder(View itemView) {
//            super(itemView);
//            checkBox = itemView.findViewById(R.id.checkBox);
//            textView = itemView.findViewById(R.id.textView);
//            textNote = itemView.findViewById(R.id.textNote);
//            btnDelete = itemView.findViewById(R.id.btnDelete);
//
//            // Клик по чекбоксу — только переключение статуса
//            checkBox.setOnClickListener(v -> {
//                if (listener != null) {
//                    ShoppingItem item = items.get(getAdapterPosition());
//                    item.isBought = checkBox.isChecked();
//                    listener.onItemBoughtToggled(item);
//                }
//            });
//
//            // Клик по всей карточке — редактирование заметки
//            itemView.setOnClickListener(v -> {
//                if (listener != null) {
//                    ShoppingItem item = items.get(getAdapterPosition());
//                    listener.onItemClicked(item);
//                }
//            });
//
//            // Клик по кнопке — удаление
//            btnDelete.setOnClickListener(v -> {
//                if (listener != null) {
//                    ShoppingItem item = items.get(getAdapterPosition());
//                    listener.onItemDeleted(item);
//                }
//            });
//        }
//    }
//}
package com.example.shoppinglist2;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShoppingItemAdapter extends RecyclerView.Adapter<ShoppingItemAdapter.ViewHolder> {

    private List<ShoppingItem> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemBoughtToggled(ShoppingItem item);
        void onItemDeleted(ShoppingItem item);
        void onItemClicked(ShoppingItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ShoppingItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shopping, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingItem item = items.get(position);
        holder.textView.setText(item.name);

        // Отображаем заметку
        if (!TextUtils.isEmpty(item.note)) {
            holder.textNote.setText(item.note);
            holder.textNote.setVisibility(View.VISIBLE);
        } else {
            holder.textNote.setVisibility(View.GONE);
        }

        // Отображаем даты
        if (!TextUtils.isEmpty(item.createdAt)) {
            String formattedDate = formatDate(item.createdAt);
            holder.textDate.setText(formattedDate);
            holder.textDate.setVisibility(View.VISIBLE);
        } else {
            holder.textDate.setVisibility(View.GONE);
        }

        holder.checkBox.setChecked(item.isBought);

        if (item.isBought) {
            holder.textView.setAlpha(0.5f);
            holder.textNote.setAlpha(0.5f);
            holder.textDate.setAlpha(0.5f);
        } else {
            holder.textView.setAlpha(1.0f);
            holder.textNote.setAlpha(1.0f);
            holder.textDate.setAlpha(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Метод для форматирования даты в более читаемый вид
    private String formatDate(String dateString) {
        try {
            // Пробуем разные форматы дат от сервера
            SimpleDateFormat[] inputFormats = {
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                    new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            };

            Date date = null;
            for (SimpleDateFormat format : inputFormats) {
                try {
                    date = format.parse(dateString);
                    if (date != null) break;
                } catch (ParseException e) {
                    // Пробуем следующий формат
                }
            }

            if (date != null) {
                // Форматируем для отображения (день.месяц.год час:минута)
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault());
                return outputFormat.format(date);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Если не удалось распарсить, возвращаем как есть
        return dateString;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView textView;
        TextView textNote;
        TextView textDate;
        MaterialButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkBox);
            textView = itemView.findViewById(R.id.textView);
            textNote = itemView.findViewById(R.id.textNote);
            textDate = itemView.findViewById(R.id.textDate);  // Убедитесь, что этот ID есть в layout
            btnDelete = itemView.findViewById(R.id.btnDelete);

            // Клик по чекбоксу
            checkBox.setOnClickListener(v -> {
                if (listener != null) {
                    ShoppingItem item = items.get(getAdapterPosition());
                    item.isBought = checkBox.isChecked();
                    item.updatedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(new Date());
                    listener.onItemBoughtToggled(item);
                }
            });

            // Клик по всей карточке
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    ShoppingItem item = items.get(getAdapterPosition());
                    listener.onItemClicked(item);
                }
            });

            // Клик по кнопке удаления
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    ShoppingItem item = items.get(getAdapterPosition());
                    listener.onItemDeleted(item);
                }
            });
        }
    }
}