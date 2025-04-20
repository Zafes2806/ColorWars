package com.example.colorwar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CellAdapter extends RecyclerView.Adapter<CellAdapter.CellViewHolder> {

    private int[] cellStates; // Mảng lưu trạng thái của các ô
    private OnCellClickListener onCellClickListener;

    public interface OnCellClickListener {
        void onCellClick(int position);
    }

    public CellAdapter(int[] cellStates, OnCellClickListener listener) {
        this.cellStates = cellStates;
        this.onCellClickListener = listener;
    }

    @NonNull
    @Override
    public CellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cell, parent, false);
        return new CellViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CellViewHolder holder, int position) {
        int state = cellStates[position];
        // Chỉ hiển thị các trạng thái hợp lệ (0, 1-3, 4-6)
        switch (state) {
            case 0:
                holder.cellImage.setImageResource(R.drawable.ic_cell_empty);
                break;
            case 1:
                holder.cellImage.setImageResource(R.drawable.ic_cell_red_1_dot);
                break;
            case 2:
                holder.cellImage.setImageResource(R.drawable.ic_cell_red_2_dots);
                break;
            case 3:
                holder.cellImage.setImageResource(R.drawable.ic_cell_red_3_dots);
                break;
            case 4:
                holder.cellImage.setImageResource(R.drawable.ic_cell_blue_1_dot);
                break;
            case 5:
                holder.cellImage.setImageResource(R.drawable.ic_cell_blue_2_dots);
                break;
            case 6:
                holder.cellImage.setImageResource(R.drawable.ic_cell_blue_3_dots);
                break;
            default:
                // Bỏ qua các trạng thái không hợp lệ (7, 8) để tránh lỗi
                holder.cellImage.setImageResource(R.drawable.ic_cell_empty);
                break;
        }

        // Thiết lập sự kiện nhấn
        holder.itemView.setOnClickListener(v -> {
            if (onCellClickListener != null) {
                int currentPosition = holder.getAdapterPosition();
                if (currentPosition != RecyclerView.NO_POSITION) {
                    onCellClickListener.onCellClick(currentPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return cellStates.length;
    }

    public static class CellViewHolder extends RecyclerView.ViewHolder {
        ImageView cellImage;

        public CellViewHolder(@NonNull View itemView) {
            super(itemView);
            cellImage = itemView.findViewById(R.id.cell_image);
        }
    }

    public void updateCell(int position, int newState) {
        cellStates[position] = newState;
        notifyItemChanged(position);
    }
}