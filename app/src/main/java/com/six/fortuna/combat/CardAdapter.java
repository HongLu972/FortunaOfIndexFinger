package com.six.fortuna.combat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.six.fortuna.R;

public class CardAdapter extends RecyclerView.Adapter<CardAdapter.ViewHolder> {

    private List<Card> cardList;
    private OnCardClickListener listener;

    public interface OnCardClickListener {
        void onCardClick(Card card);
    }

    public CardAdapter(List<Card> cardList, OnCardClickListener listener) {
        this.cardList = cardList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Card card = cardList.get(position);
        holder.tvName.setText(card.name);
        holder.tvCost.setText("⚡" + card.cost);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCardClick(card);
        });
    }

    @Override
    public int getItemCount() {
        return cardList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCost;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_card_name);
            tvCost = itemView.findViewById(R.id.tv_card_cost);
        }
    }
}