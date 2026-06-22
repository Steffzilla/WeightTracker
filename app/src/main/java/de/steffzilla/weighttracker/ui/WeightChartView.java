package de.steffzilla.weighttracker.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import de.steffzilla.weighttracker.R;
import de.steffzilla.weighttracker.stats.ChartModel;
import de.steffzilla.weighttracker.stats.ChartPoint;

/**
 * A dumb, self-contained line chart that renders a prepared {@link ChartModel}: a
 * dominant value line with optional dots for the actual measurements, a padded
 * (non-zero-based) y-axis and date-proportional x positions. All colors come from
 * Material 3 theme attributes so day/night is handled automatically; text sizes are
 * in {@code sp} so they honour the system font scale.
 */
public class WeightChartView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();

    private final float strokeWidth;
    private final float markerRadius;
    private final float labelGap;

    @Nullable
    private ChartModel model;

    public WeightChartView(Context context) {
        this(context, null);
    }

    public WeightChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WeightChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        strokeWidth = getResources().getDimension(R.dimen.chart_line_stroke);
        markerRadius = getResources().getDimension(R.dimen.chart_marker_radius);
        labelGap = getResources().getDimension(R.dimen.chart_label_gap);

        int lineColor = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorPrimary);
        int gridColor = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOutlineVariant);
        int textColor = MaterialColors.getColor(
                this, com.google.android.material.R.attr.colorOnSurfaceVariant);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(strokeWidth);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(lineColor);

        markerPaint.setStyle(Paint.Style.FILL);
        markerPaint.setColor(lineColor);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(getResources().getDimension(R.dimen.chart_grid_stroke));
        gridPaint.setColor(gridColor);

        textPaint.setColor(textColor);
        textPaint.setTextSize(getResources().getDimension(R.dimen.chart_label_text_size));
    }

    /** Supplies a new model to render. The content description is set by the caller. */
    public void setModel(@Nullable ChartModel model) {
        this.model = model;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (model == null || model.isEmpty()) {
            return;
        }

        Locale locale = getResources().getConfiguration().getLocales().get(0);
        DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale);

        float textHeight = textPaint.getFontMetrics().descent - textPaint.getFontMetrics().ascent;

        // Reserve space: y labels on the left, x labels at the bottom.
        float maxLabel = textPaint.measureText(formatWeight(locale, model.yMax()));
        float minLabel = textPaint.measureText(formatWeight(locale, model.yMin()));
        float leftInset = Math.max(maxLabel, minLabel) + labelGap;
        float bottomInset = textHeight + labelGap;

        // Inset horizontally by the marker footprint so points at the very edges
        // (notably today at the right) are not clipped to half-circles.
        float pointInset = markerRadius + strokeWidth / 2f;
        float plotLeft = getPaddingLeft() + leftInset + pointInset;
        float plotTop = getPaddingTop() + textHeight / 2f;
        float plotRight = getWidth() - getPaddingRight() - pointInset;
        float plotBottom = getHeight() - getPaddingBottom() - bottomInset;
        float plotWidth = plotRight - plotLeft;
        float plotHeight = plotBottom - plotTop;
        if (plotWidth <= 0 || plotHeight <= 0) {
            return;
        }

        float yMin = model.yMin();
        float yMax = model.yMax();
        float yRange = yMax - yMin;

        // Horizontal gridlines + y labels (top, middle, bottom).
        drawGridLine(canvas, locale, yMax, plotLeft, plotRight, plotTop, plotBottom, yMin, yRange);
        drawGridLine(canvas, locale, (yMin + yMax) / 2f, plotLeft, plotRight, plotTop, plotBottom, yMin, yRange);
        drawGridLine(canvas, locale, yMin, plotLeft, plotRight, plotTop, plotBottom, yMin, yRange);

        long startDay = model.xStart().toEpochDay();
        long endDay = model.xEnd().toEpochDay();
        long dayRange = endDay - startDay;

        // Build the line path and remember pixel positions for the markers.
        linePath.reset();
        int n = model.points().size();
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            ChartPoint p = model.points().get(i);
            float fraction = dayRange == 0
                    ? 0.5f
                    : (float) (p.date().toEpochDay() - startDay) / dayRange;
            float x = plotLeft + fraction * plotWidth;
            float y = plotBottom - ((p.weightKg() - yMin) / yRange) * plotHeight;
            xs[i] = x;
            ys[i] = y;
            if (i == 0) {
                linePath.moveTo(x, y);
            } else {
                linePath.lineTo(x, y);
            }
        }

        if (n > 1) {
            canvas.drawPath(linePath, linePaint);
        }
        if (model.showMarkers() || n == 1) {
            for (int i = 0; i < n; i++) {
                canvas.drawCircle(xs[i], ys[i], markerRadius, markerPaint);
            }
        }

        // X labels: start and end of the window.
        textPaint.setTextAlign(Paint.Align.LEFT);
        float baseline = getHeight() - getPaddingBottom() - textPaint.getFontMetrics().descent;
        canvas.drawText(model.xStart().format(dateFormatter), plotLeft, baseline, textPaint);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(model.xEnd().format(dateFormatter), plotRight, baseline, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawGridLine(Canvas canvas, Locale locale, float value,
                              float plotLeft, float plotRight, float plotTop, float plotBottom,
                              float yMin, float yRange) {
        float y = plotBottom - ((value - yMin) / yRange) * plotHeight(plotTop, plotBottom);
        canvas.drawLine(plotLeft, y, plotRight, y, gridPaint);
        textPaint.setTextAlign(Paint.Align.RIGHT);
        float textY = y - (textPaint.getFontMetrics().ascent + textPaint.getFontMetrics().descent) / 2f;
        canvas.drawText(formatWeight(locale, value), plotLeft - labelGap, textY, textPaint);
        textPaint.setTextAlign(Paint.Align.LEFT);
    }

    private static float plotHeight(float plotTop, float plotBottom) {
        return plotBottom - plotTop;
    }

    private static String formatWeight(Locale locale, float value) {
        return String.format(locale, "%.1f", value);
    }
}
