package de.steffzilla.weighttracker.ui;

import android.os.Bundle;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
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
    private static final String STATE_SELECTED_DATE = "state_selected_date";
    private static final String DATE_PICKER_TAG = "datePicker";

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

        // numberDecimal's default key listener only accepts '.'; allow the
        // locale decimal separator (e.g. ',' in German) to be typed as well.
        binding.editTextWeight.setKeyListener(
                DigitsKeyListener.getInstance(Locale.getDefault(), false, true));
        // The locale-aware DigitsKeyListener reports a TEXT input type when the
        // decimal separator is non-ASCII (e.g. ',' in German), which would summon
        // the full keyboard. Force the decimal number IME without touching the key
        // listener, so only digits + the locale separator stay typeable.
        binding.editTextWeight.setRawInputType(
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

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

        // A date picked before a configuration change must win over the initial value
        // from the arguments (edit) or today (add).
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_SELECTED_DATE)) {
            selectedDate = LocalDate.ofEpochDay(savedInstanceState.getLong(STATE_SELECTED_DATE));
        }

        updateDateDisplay();
        setupDatePicker();
        reattachDatePickerListener();
        binding.buttonSave.setOnClickListener(v -> onSaveClicked());
        binding.buttonCancel.setOnClickListener(v -> dismiss());
    }

    private void updateDateDisplay() {
        binding.editTextDate.setText(selectedDate.format(DATE_FORMATTER));
    }

    private void setupDatePicker() {
        binding.editTextDate.setOnClickListener(v -> {
            if (getParentFragmentManager().findFragmentByTag(DATE_PICKER_TAG) != null) return;

            long selectionMs = selectedDate.atStartOfDay(ZoneOffset.UTC)
                    .toInstant().toEpochMilli();
            var picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(R.string.date_picker_title)
                    .setSelection(selectionMs)
                    .setCalendarConstraints(new CalendarConstraints.Builder()
                            .setValidator(DateValidatorPointBackward.now())
                            .build())
                    .build();
            picker.addOnPositiveButtonClickListener(this::onDatePicked);
            picker.show(getParentFragmentManager(), DATE_PICKER_TAG);
        });
    }

    /**
     * Re-attaches the result listener to a date picker that survived a configuration
     * change. The fragment manager restores the picker dialog itself, but not its
     * listeners — without this, a date picked after e.g. a rotation would be silently
     * discarded.
     */
    @SuppressWarnings("unchecked")
    private void reattachDatePickerListener() {
        var restored = (MaterialDatePicker<Long>)
                getParentFragmentManager().findFragmentByTag(DATE_PICKER_TAG);
        if (restored != null) {
            restored.addOnPositiveButtonClickListener(this::onDatePicked);
        }
    }

    private void onDatePicked(Long selection) {
        selectedDate = Instant.ofEpochMilli(selection)
                .atZone(ZoneOffset.UTC).toLocalDate();
        updateDateDisplay();
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedDate != null) {
            outState.putLong(STATE_SELECTED_DATE, selectedDate.toEpochDay());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}