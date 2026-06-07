package de.steffzilla.weighttracker;

import android.os.Bundle;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;

import de.steffzilla.weighttracker.data.WeightEntry;
import de.steffzilla.weighttracker.databinding.ActivityMainBinding;
import de.steffzilla.weighttracker.ui.AddEditWeightBottomSheet;
import de.steffzilla.weighttracker.ui.WeightEntryAdapter;
import de.steffzilla.weighttracker.ui.WeightViewModel;
import de.steffzilla.weighttracker.ui.WeightViewModelFactory;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private WeightViewModel viewModel;
    private WeightEntryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        applyWindowInsets();
        setupRecyclerView();
        setupViewModel();

        binding.fabAdd.setOnClickListener(v -> openAddEditSheet(null));
    }

    private void applyWindowInsets() {
        int fabSize = getResources().getDimensionPixelSize(R.dimen.fab_size);
        int fabMargin = getResources().getDimensionPixelSize(R.dimen.fab_margin);

        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, bars.bottom + fabSize + fabMargin * 2);
            return insets;
        });
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabAdd, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = fabMargin + bars.bottom;
            params.rightMargin = fabMargin + bars.right;
            v.setLayoutParams(params);
            return insets;
        });
    }

    private void setupRecyclerView() {
        adapter = new WeightEntryAdapter(new WeightEntryAdapter.OnItemActionListener() {
            @Override
            public void onEditClick(WeightEntry entry) {
                openAddEditSheet(entry);
            }

            @Override
            public void onDeleteClick(WeightEntry entry) {
                // wired up in next commit
            }
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this, new WeightViewModelFactory(this))
                .get(WeightViewModel.class);
        viewModel.getAllEntries().observe(this, adapter::setEntries);
        viewModel.getUserMessage().observe(this, event -> {
            String msg = event.getContentIfNotConsumed();
            if (msg != null) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void openAddEditSheet(WeightEntry entryToEdit) {
        if (getSupportFragmentManager()
                .findFragmentByTag(AddEditWeightBottomSheet.TAG) != null) return;
        var sheet = entryToEdit != null
                ? AddEditWeightBottomSheet.newInstance(entryToEdit)
                : AddEditWeightBottomSheet.newInstance();
        sheet.show(getSupportFragmentManager(), AddEditWeightBottomSheet.TAG);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}