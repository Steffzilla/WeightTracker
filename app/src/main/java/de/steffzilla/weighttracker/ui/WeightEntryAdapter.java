package de.steffzilla.weighttracker.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import de.steffzilla.weighttracker.data.WeightEntry;
import de.steffzilla.weighttracker.databinding.ItemWeightEntryBinding;

public class WeightEntryAdapter extends RecyclerView.Adapter<WeightEntryAdapter.ViewHolder> {

    public interface OnItemActionListener {
        void onEditClick(WeightEntry entry);
        void onDeleteClick(WeightEntry entry);
    }

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private List<WeightEntry> entries = Collections.emptyList();
    private final OnItemActionListener listener;

    public WeightEntryAdapter(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setEntries(List<WeightEntry> newEntries) {
        this.entries = newEntries != null ? newEntries : Collections.emptyList();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var binding = ItemWeightEntryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        var entry = entries.get(position);

        holder.binding.textDate.setText(entry.getDate().format(DATE_FORMATTER));
        holder.binding.textWeight.setText(
                String.format(Locale.getDefault(), "%.1f kg", entry.getWeightKg()));

        boolean hasOlderEntry = position + 1 < entries.size();
        if (hasOlderEntry) {
            float diff = entry.getWeightKg() - entries.get(position + 1).getWeightKg();
            holder.binding.textDiff.setText(
                    String.format(Locale.getDefault(), "%+.1f kg", diff));
        } else {
            holder.binding.textDiff.setText("—"); // em dash
        }

        holder.binding.cardRoot.setOnClickListener(v -> listener.onEditClick(entry));
        holder.binding.buttonDelete.setOnClickListener(v -> listener.onDeleteClick(entry));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemWeightEntryBinding binding;

        ViewHolder(ItemWeightEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}