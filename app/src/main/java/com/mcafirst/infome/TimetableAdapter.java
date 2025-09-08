package com.mcafirst.infome;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TimetableAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<TimetableItem> itemList;

    public TimetableAdapter(List<TimetableItem> itemList) {
        this.itemList = itemList;
    }

    @Override
    public int getItemViewType(int position) {
        return itemList.get(position).getType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TimetableItem.TYPE_DAY) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_day, parent, false);
            return new DayViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_timetable, parent, false);
            return new EntryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        TimetableItem item = itemList.get(position);

        if (holder instanceof DayViewHolder) {
            ((DayViewHolder) holder).textDay.setText(item.getDay());
        } else if (holder instanceof EntryViewHolder) {
            TimetableEntry entry = item.getEntry();
            ((EntryViewHolder) holder).textTimePeriod.setText(item.getTimePeriod());
            ((EntryViewHolder) holder).textSubject.setText(entry.getSubjectName() + " (" + entry.getSubjectCode() + ")");
            ((EntryViewHolder) holder).textRoom.setText("Room: " + entry.getRoomNumber());
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    // ViewHolder for Day
    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView textDay;
        DayViewHolder(View itemView) {
            super(itemView);
            textDay = itemView.findViewById(R.id.textDay);
        }
    }

    // ViewHolder for Entry
    static class EntryViewHolder extends RecyclerView.ViewHolder {
        TextView textTimePeriod, textSubject, textRoom;
        EntryViewHolder(View itemView) {
            super(itemView);
            textTimePeriod = itemView.findViewById(R.id.textTimePeriod);
            textSubject = itemView.findViewById(R.id.textSubject);
            textRoom = itemView.findViewById(R.id.textRoom);
        }
    }
}
