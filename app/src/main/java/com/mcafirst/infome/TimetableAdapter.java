package com.mcafirst.infome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TimetableAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<TimetableItem> itemList;

    public TimetableAdapter(List<TimetableItem> itemList) {
        this.itemList = itemList;
    }

    @Override
    public int getItemViewType(int position) {
        return itemList.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TimetableItem.TYPE_DAY) {
            View view = inflater.inflate(R.layout.item_day_header, parent, false);
            return new DayViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_timetable_entry, parent, false);
            return new EntryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TimetableItem item = itemList.get(position);

        if (holder instanceof DayViewHolder) {
            ((DayViewHolder) holder).dayText.setText(item.dayName);
        } else if (holder instanceof EntryViewHolder) {
            TimetableEntry entry = item.entry;
            ((EntryViewHolder) holder).periodText.setText(item.period);
            ((EntryViewHolder) holder).subjectText.setText(entry.subjectName);
            ((EntryViewHolder) holder).roomText.setText(entry.roomNumber);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    // 🔹 ViewHolder for day header
    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayText;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayText = itemView.findViewById(R.id.tvDayHeader);
        }
    }

    // 🔹 ViewHolder for timetable entry
    static class EntryViewHolder extends RecyclerView.ViewHolder {
        TextView periodText, subjectText, roomText;

        EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            periodText = itemView.findViewById(R.id.tvPeriod);
            subjectText = itemView.findViewById(R.id.tvSubject);
            roomText = itemView.findViewById(R.id.tvRoom);
        }
    }
}
