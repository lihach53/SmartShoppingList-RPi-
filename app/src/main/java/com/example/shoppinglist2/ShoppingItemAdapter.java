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
import java.util.ArrayList;
import java.util.List;

public class ShoppingItemAdapter extends RecyclerView.Adapter<ShoppingItemAdapter.ViewHolder> {

    private List<ShoppingItem> items = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemBoughtToggled(ShoppingItem item);
        void onItemDeleted(ShoppingItem item);
        void onItemClicked(ShoppingItem item); // для редактирования заметки
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

        if (!TextUtils.isEmpty(item.note)) {
            holder.textNote.setText(item.note);
            holder.textNote.setVisibility(View.VISIBLE);
        } else {
            holder.textNote.setVisibility(View.GONE);
        }

        holder.checkBox.setChecked(item.isBought);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView textView;
        TextView textNote;
        MaterialButton btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkBox);
            textView = itemView.findViewById(R.id.textView);
            textNote = itemView.findViewById(R.id.textNote);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            // Клик по чекбоксу — только переключение статуса
            checkBox.setOnClickListener(v -> {
                if (listener != null) {
                    ShoppingItem item = items.get(getAdapterPosition());
                    item.isBought = checkBox.isChecked();
                    listener.onItemBoughtToggled(item);
                }
            });

            // Клик по всей карточке — редактирование заметки
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    ShoppingItem item = items.get(getAdapterPosition());
                    listener.onItemClicked(item);
                }
            });

            // Клик по кнопке — удаление
            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    ShoppingItem item = items.get(getAdapterPosition());
                    listener.onItemDeleted(item);
                }
            });
        }
    }
}