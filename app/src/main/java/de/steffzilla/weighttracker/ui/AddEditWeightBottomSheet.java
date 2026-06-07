package de.steffzilla.weighttracker.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import de.steffzilla.weighttracker.R;
import de.steffzilla.weighttracker.data.WeightEntry;
import de.steffzilla.weighttracker.databinding.FragmentAddEditWeightBinding;

public class AddEditWeightBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "AddEditWeightBottomSheet";

    private static final String ARG_ENTRY_ID = "entry_id";
    private static final String ARG_ENTRY_DATE = "entry_date";
    private static final String ARG_ENTRY_WEIGHT = "entry_weight";

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private FragmentAddEditWeightBinding binding;
    private WeightViewModel viewModel;
    private LocalDate selectedDate;
    private WeightEntry entryToEdit;

    public static AddEditWeightBottomSheet newInstance() {
        return new AddEditWeightBottomSheet();
    }

    public static AddEditWeightBottomSheet newInstance(@NonNull WeightEntry entry) {
        var sheet = new AddEditWeightBottomSheet();
        var args = new Bundle();
        args.putLong(ARG_ENTRY_ID, entry.getId());
        args.putLong(ARG_ENTRY_DATE, entry.getDate().toEpochDay());
        args.putFloat(ARG_ENTRY_WEIGHT, entry.getWeightKg());
        sheet.setArguments(args);
        return sheet;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAddEditWeightBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity(),
                new WeightViewModelFactory(requireContext()))
                .get(WeightViewModel.class);

        Bundle args = getArguments();
        if (args != null) {
            selectedDate = LocalDate.ofEpochDay(args.getLong(ARG_ENTRY_DATE));
            float weight = args.getFloat(ARG_ENTRY_WEIGHT);
            entryToEdit = new WeightEntry(selectedDate, weight);
            entryToEdit.setId(args.getLong(ARG_ENTRY_ID));
            binding.textTitle.setText(R.string.title_edit_entry);
            binding.editTextWeight.setText(
                    String.format(Locale.getDefault(), "%.1f", weight));
        } else {
            selectedDate = LocalDate.now();
            binding.textTitle.setText(R.string.title_add_entry);
        }

        updateDateDisplay();
        setupDatePicker();
        binding.buttonSave.setOnClickListener(v -> onSaveClicked());
        binding.buttonCancel.setOnClickListener(v -> dismiss());
    }

    private void updateDateDisplay() {
        binding.editTextDate.setText(selectedDate.format(DATE_FORMATTER));
    }

    private void setupDatePicker() {
        binding.editTextDate.setOnClickListener(v -> {
            if (getParentFragmentManager().findFragmentByTag("datePicker") != null) return;

            long selectionMs = selectedDate.atStartOfDay(ZoneOffset.UTC)
                    .toInstant().toEpochMilli();
            var picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.date_picker_title)
                    .setSelection(selectionMs)
                    .setCalendarConstraints(new CalendarConstraints.Builder()
                            .setValidator(DateValidatorPointBackward.now())
                            .build())
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                selectedDate = Instant.ofEpochMilli(selection)
                        .atZone(ZoneOffset.UTC).toLocalDate();
                updateDateDisplay();
            });
            picker.show(getParentFragmentManager(), "datePicker");
        });
    }

    private void onSaveClicked() {
        if (!validateWeight()) return;

        float weight = parseWeight();
        if (entryToEdit != null) {
            entryToEdit.setDate(selectedDate);
            entryToEdit.setWeightKg(weight);
            viewModel.updateEntry(entryToEdit);
        } else {
            viewModel.addEntry(selectedDate, weight);
        }
        dismiss();
    }

    private boolean validateWeight() {
        binding.textInputLayoutWeight.setError(null);
        String raw = binding.editTextWeight.getText() != null
                ? binding.editTextWeight.getText().toString().trim() : "";

        if (raw.isEmpty()) {
            binding.textInputLayoutWeight.setError(getString(R.string.error_weight_empty));
            return false;
        }

        String normalized = raw.replace(',', '.');
        float value;
        try {
            value = Float.parseFloat(normalized);
        } catch (NumberFormatException e) {
            binding.textInputLayoutWeight.setError(getString(R.string.error_weight_invalid));
            return false;
        }

        if (value <= 0 || value > 999.9f) {
            binding.textInputLayoutWeight.setError(getString(R.string.error_weight_range));
            return false;
        }

        int dotIndex = normalized.indexOf('.');
        if (dotIndex != -1 && normalized.length() - dotIndex - 1 > 1) {
            binding.textInputLayoutWeight.setError(getString(R.string.error_weight_decimal));
            return false;
        }

        return true;
    }

    private float parseWeight() {
        String raw = binding.editTextWeight.getText().toString().trim();
        return Float.parseFloat(raw.replace(',', '.'));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}