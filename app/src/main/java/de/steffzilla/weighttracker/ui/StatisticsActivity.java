package de.steffzilla.weighttracker.ui;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import de.steffzilla.weighttracker.R;
import de.steffzilla.weighttracker.databinding.ActivityStatisticsBinding;
import de.steffzilla.weighttracker.stats.ChartModel;
import de.steffzilla.weighttracker.stats.ChartRange;
import de.steffzilla.weighttracker.stats.WeightStatistics;

public class StatisticsActivity extends AppCompatActivity {

    private ActivityStatisticsBinding binding;
    private StatisticsViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        applyWindowInsets();
        setupRangeToggle();
        setupViewModel();
        setupPointBudget();
    }

    /**
     * Derives how many points the chart can legibly draw from its measured width and
     * feeds it to the ViewModel. Re-runs on width changes (e.g. rotation) so the chart
     * resolution adapts to the available space.
     */
    private void setupPointBudget() {
        float minSpacing = getResources().getDimension(R.dimen.chart_min_point_spacing);
        binding.chartView.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                    int width = right - left;
                    if (width > 0) {
                        viewModel.setMaxPoints(Math.round(width / minSpacing));
                    }
                });
    }

    private void applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bars.bottom);
            return insets;
        });
    }

    private void setupRangeToggle() {
        binding.rangeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                viewModel.setRange(rangeForButton(checkedId));
            }
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this, new StatisticsViewModelFactory(this))
                .get(StatisticsViewModel.class);

        viewModel.getSelectedRange().observe(this, range -> {
            int buttonId = buttonForRange(range);
            if (binding.rangeToggle.getCheckedButtonId() != buttonId) {
                binding.rangeToggle.check(buttonId);
            }
        });

        viewModel.getChartModel().observe(this, this::render);
    }

    private void render(ChartModel model) {
        boolean empty = model == null || model.isEmpty();
        binding.statsRow.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.chartView.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);

        binding.chartView.setModel(model);
        binding.chartView.setContentDescription(buildContentDescription(model));

        if (empty) {
            return;
        }

        Locale locale = getResources().getConfiguration().getLocales().get(0);
        WeightStatistics stats = model.stats();
        binding.valueAverage.setText(formatWeight(locale, stats.average()));
        binding.valueChange.setText(formatChange(locale, stats.change()));
        binding.valueMin.setText(formatWeight(locale, stats.min()));
        binding.valueMax.setText(formatWeight(locale, stats.max()));
    }

    private CharSequence buildContentDescription(ChartModel model) {
        ChartRange range = viewModel.getSelectedRange().getValue();
        String rangeLabel = getString(range != null ? range.getLabelRes() : R.string.range_all);

        if (model == null || model.isEmpty()) {
            return getString(R.string.chart_content_description_empty, rangeLabel);
        }

        Locale locale = getResources().getConfiguration().getLocales().get(0);
        DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale);
        WeightStatistics stats = model.stats();

        if (stats.count() == 1) {
            return getString(R.string.chart_content_description_single,
                    rangeLabel,
                    formatWeight(locale, stats.average()),
                    stats.firstDate().format(dateFormatter));
        }

        return getString(R.string.chart_content_description,
                rangeLabel,
                formatWeight(locale, stats.firstWeight()),
                stats.firstDate().format(dateFormatter),
                formatWeight(locale, stats.lastWeight()),
                stats.lastDate().format(dateFormatter),
                formatChange(locale, stats.change()));
    }

    private static String formatWeight(Locale locale, float value) {
        return String.format(locale, "%.1f", value);
    }

    private static String formatChange(Locale locale, float value) {
        return String.format(locale, "%+.1f", value);
    }

    private static ChartRange rangeForButton(@IdRes int buttonId) {
        if (buttonId == R.id.btnWeek) {
            return ChartRange.WEEK;
        } else if (buttonId == R.id.btnThreeMonths) {
            return ChartRange.THREE_MONTHS;
        } else if (buttonId == R.id.btnYear) {
            return ChartRange.YEAR;
        } else if (buttonId == R.id.btnAll) {
            return ChartRange.ALL;
        }
        return ChartRange.MONTH;
    }

    @IdRes
    private static int buttonForRange(ChartRange range) {
        return switch (range) {
            case WEEK -> R.id.btnWeek;
            case THREE_MONTHS -> R.id.btnThreeMonths;
            case YEAR -> R.id.btnYear;
            case ALL -> R.id.btnAll;
            case MONTH -> R.id.btnMonth;
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
