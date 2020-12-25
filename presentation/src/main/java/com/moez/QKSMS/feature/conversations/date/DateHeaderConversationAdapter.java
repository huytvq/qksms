package com.moez.QKSMS.feature.conversations.date;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.moez.QKSMS.R;
import com.moez.QKSMS.common.util.DateFormatter;
import com.moez.QKSMS.common.widget.QkTextView;

import java.util.ArrayList;

public class DateHeaderConversationAdapter extends RecyclerView.Adapter<DateHeaderConversationAdapter.ViewHolder> {

    private Context context;
    private ArrayList<String> dates;
    private DateFormatter dateFormatter;
    private OnSelectedDateListener onSelectedDateListener;
    private int index = 0;

    public DateHeaderConversationAdapter(Context context, ArrayList<String> dates, OnSelectedDateListener onSelectedDateListener) {
        this.context = context;
        this.dates = dates;
        dateFormatter = new DateFormatter(context);
        this.onSelectedDateListener = onSelectedDateListener;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @NonNull
    @Override
    public DateHeaderConversationAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_header_date_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateHeaderConversationAdapter.ViewHolder holder, int position) {
        holder.date.setText(dates.get(position).replaceAll(" ", "\n"));

        holder.date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onSelectedDateListener != null) {
                    index = position;
                    notifyDataSetChanged();

                    onSelectedDateListener.onSelected(position, dates.get(position));
                }
            }
        });

        if (index == position) {
            holder.iconSelected.setVisibility(View.VISIBLE);
        } else {
            holder.iconSelected.setVisibility(View.GONE);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        QkTextView date;
        ImageView iconSelected;

        public ViewHolder(View itemView) {
            super(itemView);

            date = itemView.findViewById(R.id.date);
            iconSelected = itemView.findViewById(R.id.iconSelected);
        }
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }
}
