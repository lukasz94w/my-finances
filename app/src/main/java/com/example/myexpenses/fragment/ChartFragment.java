package com.example.myexpenses.fragment;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myexpenses.R;
import com.example.myexpenses.customValueFormatter.IntValueFormatter;
import com.example.myexpenses.model.Transaction;
import com.example.myexpenses.repository.TransactionRepository;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingDouble;


public class ChartFragment extends Fragment implements View.OnClickListener {

    //onCreate
    private TransactionRepository transactionRepository;
    //onCreateView
    private View view;
    private ImageView previousMonthButton;
    private TextView actualChosenMonth;
    private ImageView nextMonthButton;
    private TextView monthlyTransactionSum;
    private RelativeLayout chartContainer;

    private class BarEntryHolder {
        float xVal;
        float yVal;

        public BarEntryHolder(float xVal, float yVal) {
            this.xVal = xVal;
            this.yVal = yVal;
        }

        public float getxVal() {
            return xVal;
        }

        public float getyVal() {
            return yVal;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transactionRepository = new TransactionRepository(getContext());
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.fragment_chart, container, false);

        previousMonthButton = view.findViewById(R.id.previousMonthButton);
        previousMonthButton.setOnClickListener(this);

        actualChosenMonth = view.findViewById(R.id.actualChosenMonth);

        nextMonthButton = view.findViewById(R.id.nextMonthButton);
        nextMonthButton.setOnClickListener(this);

        monthlyTransactionSum = view.findViewById(R.id.monthlyTransactionSum);

        chartContainer = view.findViewById(R.id.chartContainer);

        Calendar calendar = Calendar.getInstance();
        int currentMonth = calendar.get(Calendar.MONTH) + 1; //month index start at 0
        int currentYear = calendar.get(Calendar.YEAR);

        updateView(currentMonth, currentYear);

        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.previousMonthButton: {
                int currentChosenMonth = Integer.parseInt(this.actualChosenMonth.getText().toString().substring(0, 2));
                int currentChosenMonthYear = Integer.parseInt(this.actualChosenMonth.getText().toString().substring(3, 7));

                Integer[] previousMonthInTable = getPreviousMonth(currentChosenMonth, currentChosenMonthYear);
                int previousMonth = previousMonthInTable[0];
                int previousMonthYear = previousMonthInTable[1];

                updateView(previousMonth, previousMonthYear);

                break;
            }

            case R.id.nextMonthButton: {
                int currentChosenMonth = Integer.parseInt(this.actualChosenMonth.getText().toString().substring(0, 2));
                int currentChosenMonthYear = Integer.parseInt(this.actualChosenMonth.getText().toString().substring(3, 7));

                Integer[] nextMonthInTable = getNextMonth(currentChosenMonth, currentChosenMonthYear);
                int nextMonth = nextMonthInTable[0];
                int nextMonthYear = nextMonthInTable[1];

                updateView(nextMonth, nextMonthYear);

                break;
            }

            default:
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateView(int currentChosenMonth, int currentChosenMonthYear) {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, currentChosenMonth - 1);
        calendar.set(Calendar.YEAR, currentChosenMonthYear);
        int numberOfDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        ArrayList<Transaction> monthTransactions = transactionRepository.findTransactionsInMonth(currentChosenMonth, currentChosenMonthYear);
        double totalSum = monthTransactions.stream()
                .mapToDouble(o -> o.getAmount())
                .sum();

        actualChosenMonth.setText(convertMonthToString(currentChosenMonth, currentChosenMonthYear));
        setMonthlyTransactionSum(totalSum);

        chartContainer.removeAllViews();
        drawMonthlyTransactionOuterPieChart(monthTransactions, numberOfDaysInMonth, totalSum);
        drawMonthlyTransactionInnerPieChart(monthTransactions);
        drawMonthlyTransactionBarCharts(monthTransactions, numberOfDaysInMonth);
    }

    private void setMonthlyTransactionSum(double totalSum) {
        if (totalSum >= 0) {
            monthlyTransactionSum.setText(String.format("+%.2f", totalSum));
            monthlyTransactionSum.setTextColor(ContextCompat.getColor(getContext(), R.color.ColorPrimary));
        } else {
            monthlyTransactionSum.setText(String.format("%.2f", totalSum));
            monthlyTransactionSum.setTextColor(ContextCompat.getColor(getContext(), R.color.bacgroundColorPopup));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void drawMonthlyTransactionOuterPieChart(ArrayList<Transaction> monthTransactions, int numberOfDaysInMonth, double totalSum) {
        PieChart pieChart = new PieChart(getContext());
        Legend legendInner = pieChart.getLegend();
        legendInner.setEnabled(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setTransparentCircleRadius(58f);
        pieChart.setHoleRadius(58f);

        //customize no data communicate
        pieChart.setNoDataText("No records found");
        Paint p = pieChart.getPaint(Chart.PAINT_INFO);
        p.setTextSize(27);
        p.setColor(getResources().getColor(R.color.black));

        int idMonthlyTransactionOuterPieChart = 1;
        pieChart.setId(idMonthlyTransactionOuterPieChart);

        RelativeLayout.LayoutParams layoutParamsForBarOuterPieChart = new RelativeLayout.LayoutParams(600, 600);
        layoutParamsForBarOuterPieChart.setMargins(0, 140, 0, 0); //margin for PieChart with no data ("No records found")
        layoutParamsForBarOuterPieChart.addRule(RelativeLayout.CENTER_HORIZONTAL);

        //check if there are data to present on pieChart, if not set no data communicate
        if(monthTransactions.size() > 0) {
            layoutParamsForBarOuterPieChart.setMargins(0, 10, 0, 0); //margin for PieChart with data

            //prepare map which contains sum of transactions with the same category
            //by using LinkedHashMap I keep order needed when outer and inner charts are drawn
            Map<String, Double> map = monthTransactions.stream().
                    collect(groupingBy(Transaction::getCategory, LinkedHashMap::new, summingDouble(Transaction::getAmount)));

            ArrayList<Integer> transactionColor = new ArrayList<>();
            ArrayList<PieEntry> transactionValue = new ArrayList<>();

            for (Map.Entry<String, Double> entry : map.entrySet()) {
                //prepare values for PieChart
                transactionValue.add(new PieEntry(Math.abs(entry.getValue().floatValue()), entry.getKey()));
                //prepare colors for values
                String colorName = (entry.getKey() + "_color").toLowerCase().replace(" ", "_");
                int colorId = getContext().getResources().getColor(getContext().getResources().getIdentifier(colorName, "color", getContext().getPackageName()));
                transactionColor.add(colorId);
            }

            PieDataSet pieDataSet = new PieDataSet(transactionValue, "Transactions");
            PieData pieData = new PieData(pieDataSet);
            pieData.setValueFormatter(new PercentFormatter());
            pieData.setValueTextSize(13f);
            pieData.setValueTextColor(Color.DKGRAY);
            pieDataSet.setColors(transactionColor);
            pieChart.setData(pieData);
            pieChart.invalidate();

            //if outerPieChart contains data, show average transaction per day
            TextView monthlyTransactionAverage = new TextView(getContext());
            int idMonthlyTransactionAverage = 2;
            monthlyTransactionAverage.setId(idMonthlyTransactionAverage);
            monthlyTransactionAverage.setTextSize(15);
            String text = String.format("Average: %.2f / day", totalSum / numberOfDaysInMonth);
            monthlyTransactionAverage.setText(text);
            RelativeLayout.LayoutParams layoutParamsForMonthlyTransactionAverage = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParamsForMonthlyTransactionAverage.addRule(RelativeLayout.BELOW, pieChart.getId()); //show below outerPieChart
            layoutParamsForMonthlyTransactionAverage.setMargins(10, 20, 0, 0);
            chartContainer.addView(monthlyTransactionAverage, layoutParamsForMonthlyTransactionAverage);
        }
        chartContainer.addView(pieChart, layoutParamsForBarOuterPieChart);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void drawMonthlyTransactionInnerPieChart(ArrayList<Transaction> monthTransactions) {
        PieChart pieChart = new PieChart(getContext());
        Legend legendInner = pieChart.getLegend();
        legendInner.setEnabled(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setTransparentCircleRadius(58f);
        pieChart.setHoleRadius(58f);
        pieChart.setNoDataText(""); //no data text is set on outerPieChart

        RelativeLayout.LayoutParams layoutParamsForBarInnerPieChart = new RelativeLayout.LayoutParams(364, 364);
        layoutParamsForBarInnerPieChart.setMargins(0, 128, 0, 0);
        layoutParamsForBarInnerPieChart.addRule(RelativeLayout.CENTER_HORIZONTAL);

        //check if there are data to present on pieChart, if not set no data communicate
        if(monthTransactions.size() > 0) {
            //prepare map which contains sum of transactions with the same category
            //by using LinkedHashMap I keep order needed when outer and inner charts are drawn
            Map<Integer, Double> map = monthTransactions.stream().
                    collect(groupingBy(Transaction::getType, LinkedHashMap::new, summingDouble(Transaction::getAmount)));

            ArrayList<Integer> transactionColor = new ArrayList<>();
            ArrayList<PieEntry> transactionValue = new ArrayList<>();

            for (Map.Entry<Integer, Double> entry : map.entrySet()) {
                //prepare values for PieChart
                transactionValue.add(new PieEntry(Math.abs(entry.getValue().floatValue()), entry.getKey()));
                //prepare colors for values
                String colorName;
                if (entry.getKey().equals(1)) {
                    colorName = ("expenses_color").toLowerCase().replace(" ", "_");
                } else {
                    colorName = ("incomes_color").toLowerCase().replace(" ", "_");
                }
                int colorId = getContext().getResources().getColor(getContext().getResources().getIdentifier(colorName, "color", getContext().getPackageName()));
                transactionColor.add(colorId);
            }

            PieDataSet pieDataSet = new PieDataSet(transactionValue, "Transactions");
            PieData pieData = new PieData(pieDataSet);
            pieData.setValueFormatter(new PercentFormatter());
            pieData.setValueTextSize(13f);
            pieData.setValueTextColor(Color.DKGRAY);
            pieDataSet.setColors(transactionColor);
            pieChart.setData(pieData);
            pieChart.invalidate();
        }
        chartContainer.addView(pieChart, layoutParamsForBarInnerPieChart);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void drawMonthlyTransactionBarCharts(ArrayList<Transaction> monthTransactions, int numberOfDaysInMonth) {
        //return lists of transactions from whole month grouped by category (f.e. sport, health)
        ArrayList<List<Transaction>> listsOfMonthTransactionsGroupedByCategory = new ArrayList<>(
                monthTransactions.stream()
                        .collect(Collectors.groupingBy(Transaction::getCategory))
                        .values());

        int numberOfBarChartsToDraw = listsOfMonthTransactionsGroupedByCategory.size();
        ArrayList<ArrayList> listsOfBarEntries = new ArrayList<>();

        for (int i = 0; i < numberOfBarChartsToDraw; i++) {

            //configure barChart appearance
            BarChart barChart = new BarChart(getContext());
            barChart.getLegend().setEnabled(false);
            barChart.getDescription().setEnabled(false);
            barChart.getAxisRight().setDrawLabels(false);
            barChart.getAxisRight().setDrawAxisLine(false);
            barChart.getAxisRight().setDrawGridLines(false);
            barChart.getAxisLeft().setDrawAxisLine(false);
            barChart.getAxisLeft().setGridColor(getResources().getColor(R.color.grid_color)); //color of left Y axis
            barChart.getAxisLeft().setTextSize(12);
            barChart.getAxisLeft().setXOffset(10);
            barChart.getAxisLeft().setValueFormatter(new IntValueFormatter());
            XAxis xAxis = barChart.getXAxis();
            xAxis.setDrawAxisLine(false);
            xAxis.setGridColor(getResources().getColor(R.color.grid_color));
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setAxisMinimum(-1);
            xAxis.setTextSize(12);
            if (numberOfDaysInMonth == 31) {
                xAxis.setAxisMaximum(31);
            }
            else if (numberOfDaysInMonth == 30){
                xAxis.setAxisMaximum(29.5f); //to last bar look better
            }
            else xAxis.setAxisMaximum(28); //february leap and non-leap year
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    return String.valueOf((int) value + 1); //this is not the most elegant way but it works...
                }
            });

            ArrayList<BarEntryHolder> holderOfBarEntries = new ArrayList<>();
            float sumOfTransactionsOfCurrentCategoryInMonth = 0;
            float maximumTransactionValueOfCurrentCategoryInMonth = 0;

            for (int j = 0; j < listsOfMonthTransactionsGroupedByCategory.get(i).size(); j++) {

                Calendar calendarToGetDayOfMonth = Calendar.getInstance();
                Date dateOfTransaction = listsOfMonthTransactionsGroupedByCategory.get(i).get(j).getDate();
                calendarToGetDayOfMonth.setTime(dateOfTransaction);
                int dayOfMonth = calendarToGetDayOfMonth.get(Calendar.DAY_OF_MONTH);

                float dayOfTransaction = dayOfMonth - 1; //because at getFormattedValue for xAxis +1 is added
                float valueOfTransaction = (float) Math.abs(listsOfMonthTransactionsGroupedByCategory.get(i).get(j).getAmount()); //abs because expense numbers are stored with '-' sign

                //checking if there is already a transaction with the same date, if so, actualize total sum
                int index = IntStream.range(0, holderOfBarEntries.size())
                        .filter(k -> holderOfBarEntries.get(k).getxVal() == dayOfTransaction)
                        .findFirst()
                        .orElse(-1);
                if (index != -1 ) {
                    float currentDayTotalSumOfTransactions = holderOfBarEntries.get(index).getyVal();
                    holderOfBarEntries.set(index, new BarEntryHolder(dayOfTransaction, currentDayTotalSumOfTransactions + valueOfTransaction));
                }
                else {
                    holderOfBarEntries.add(new BarEntryHolder(dayOfTransaction, valueOfTransaction));
                }

                if (valueOfTransaction > maximumTransactionValueOfCurrentCategoryInMonth) {
                    maximumTransactionValueOfCurrentCategoryInMonth = valueOfTransaction;
                }
                sumOfTransactionsOfCurrentCategoryInMonth = sumOfTransactionsOfCurrentCategoryInMonth + valueOfTransaction;
            }
            barChart.getAxisLeft().setAxisMinimum(-(maximumTransactionValueOfCurrentCategoryInMonth * 0.075f)); //make additional space between bottom of the chart and labels
            //copy data from barEntryHolder to barEntriesList
            ArrayList barEntriesOfCurrentCategoryInMonth = new ArrayList<>();
            for (int m = 0; m < holderOfBarEntries.size(); m++) {
                barEntriesOfCurrentCategoryInMonth.add(new BarEntry(holderOfBarEntries.get(m).getxVal(), holderOfBarEntries.get(m).getyVal()));
            }
            listsOfBarEntries.add(barEntriesOfCurrentCategoryInMonth);

            BarDataSet barDataSet = new BarDataSet(listsOfBarEntries.get(i), "");
            BarData barData = new BarData(barDataSet);
            barChart.setData(barData);
            barDataSet.setValueTextSize(6);
            barDataSet.setValueFormatter(new IntValueFormatter()); //value without decimal points
            barDataSet.setValueTextColor(Color.BLACK);

            //start drawing barChart
            View separatorLine = new View(getContext());
            separatorLine.setId(i * 10 + 3);
            RelativeLayout.LayoutParams separatorLineLayout = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            separatorLineLayout.setMargins(7, 28, 7, 0);
            if (i == 0) {
                separatorLineLayout.addRule(RelativeLayout.BELOW, 2); //if it's first barChart draw line under monthlyTransactionAverage (id = 2)
            } else {
                separatorLineLayout.addRule(RelativeLayout.BELOW, (i - 1) * 10 + 9); //draw liner under previous barChart
            }
            separatorLine.setBackgroundColor(getResources().getColor(R.color.separator_color));
            chartContainer.addView(separatorLine, separatorLineLayout);

            ImageView categoryImage = new ImageView(getContext());
            categoryImage.setId(i * 10 + 4);
            String categoryImageResource = listsOfMonthTransactionsGroupedByCategory.get(i).get(0).getCategory().toLowerCase().replace(" ", "_"); //prepare R.drawable.name: toLowerCase() because Android restrict Drawable filenames to not use Capital letters in their names, and also simple replace
            int res = getContext().getResources().getIdentifier(categoryImageResource, "drawable", getContext().getPackageName());
            categoryImage.setImageResource(res);
            RelativeLayout.LayoutParams layoutParamsForCategoryImage = new RelativeLayout.LayoutParams(80, 80);
            layoutParamsForCategoryImage.addRule(RelativeLayout.BELOW, separatorLine.getId());
            layoutParamsForCategoryImage.setMargins(5, 10, 0, 0);
            chartContainer.addView(categoryImage, layoutParamsForCategoryImage);

            TextView categoryName = new TextView(getContext());
            categoryName.setId(i * 10 + 5);
            categoryName.setTextSize(16);
            categoryName.setText(listsOfMonthTransactionsGroupedByCategory.get(i).get(0).getCategory());
            RelativeLayout.LayoutParams layoutParamsForCategoryName = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParamsForCategoryName.addRule(RelativeLayout.BELOW, separatorLine.getId());
            layoutParamsForCategoryName.addRule(RelativeLayout.RIGHT_OF, categoryImage.getId());
            layoutParamsForCategoryName.setMargins(10, 30, 0, 0);
            chartContainer.addView(categoryName, layoutParamsForCategoryName);

            ImageView moneyIcon = new ImageView(getContext());
            moneyIcon.setId(i * 10 + 6);
            moneyIcon.setImageResource(R.drawable.money);
            RelativeLayout.LayoutParams layoutParamsForMoneyIcon = new RelativeLayout.LayoutParams(50, 50);
            layoutParamsForMoneyIcon.addRule(RelativeLayout.BELOW, separatorLine.getId());
            layoutParamsForMoneyIcon.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            layoutParamsForMoneyIcon.setMargins(0, 30, 5, 0);
            chartContainer.addView(moneyIcon, layoutParamsForMoneyIcon);

            TextView totalAmountForMonth = new TextView(getContext());
            totalAmountForMonth.setId(i * 10 + 7);
            totalAmountForMonth.setTextSize(17);
            totalAmountForMonth.setTypeface(null, Typeface.BOLD);
            if (listsOfMonthTransactionsGroupedByCategory.get(i).get(0).getType() == 1) {
                sumOfTransactionsOfCurrentCategoryInMonth = - sumOfTransactionsOfCurrentCategoryInMonth; //if it's expense
            }
            String totalAmountForMonthText = String.format("%.2f", sumOfTransactionsOfCurrentCategoryInMonth);
            totalAmountForMonth.setText(totalAmountForMonthText);
            RelativeLayout.LayoutParams layoutParamsForTotalAmountForMonth = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParamsForTotalAmountForMonth.addRule(RelativeLayout.BELOW, separatorLine.getId());
            layoutParamsForTotalAmountForMonth.addRule(RelativeLayout.LEFT_OF, moneyIcon.getId());
            layoutParamsForTotalAmountForMonth.setMargins(0, 30, 0, 0);
            chartContainer.addView(totalAmountForMonth, layoutParamsForTotalAmountForMonth);

            TextView averageForMonth = new TextView(getContext());
            averageForMonth.setId(i * 10 + 8);
            averageForMonth.setTextSize(15);
            String averageForMonthText = String.format("Average: %.2f / day", sumOfTransactionsOfCurrentCategoryInMonth / numberOfDaysInMonth);
            averageForMonth.setText(averageForMonthText);
            RelativeLayout.LayoutParams layoutParamsForAverageForMonth = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParamsForAverageForMonth.addRule(RelativeLayout.BELOW, categoryImage.getId());
            layoutParamsForAverageForMonth.setMargins(10, 0, 0, 0);
            chartContainer.addView(averageForMonth, layoutParamsForAverageForMonth);

            //prepare color for category bars
            String barColorName = (listsOfMonthTransactionsGroupedByCategory.get(i).get(0).getCategory() + "_color").toLowerCase().replace(" ", "_");
            int barColorId = getContext().getResources().getColor(getContext().getResources().getIdentifier(barColorName, "color", getContext().getPackageName()));
            barDataSet.setColors(barColorId);

            RelativeLayout.LayoutParams layoutParamsForBarChart = new RelativeLayout.LayoutParams(740, 200);
            barChart.setId(i * 10 + 9);
            layoutParamsForBarChart.addRule(RelativeLayout.BELOW, averageForMonth.getId());
            chartContainer.addView(barChart, layoutParamsForBarChart);
        }
    }

    private Integer[] getPreviousMonth(int actualChosenMonth, int actualChosenYear) {
        int previousMonth;
        int previousMonthYear;
        Integer[] previousMonthInTable = new Integer[2];

        if (actualChosenMonth == 1) {
            previousMonth = 12;
            previousMonthYear = actualChosenYear - 1;
        } else {
            previousMonth = actualChosenMonth - 1;
            previousMonthYear = actualChosenYear;
        }
        previousMonthInTable[0] = previousMonth;
        previousMonthInTable[1] = previousMonthYear;

        return previousMonthInTable;
    }

    private Integer[] getNextMonth(int actualChosenMonth, int actualChosenYear) {
        int nextMonth;
        int nextMonthYear;
        Integer[] previousMonthInTable = new Integer[2];

        if (actualChosenMonth == 12) {
            nextMonth = 1;
            nextMonthYear = actualChosenYear + 1;
        } else {
            nextMonth = actualChosenMonth + 1;
            nextMonthYear = actualChosenYear;
        }

        previousMonthInTable[0] = nextMonth;
        previousMonthInTable[1] = nextMonthYear;

        return previousMonthInTable;
    }

    public String convertMonthToString(int month, int year) {
        month = month; //months are indexed starting at 0
        String MM = "" + month;
        String yyyy = "" + year;

        if (month < 10) {
            MM = "0" + month;
        }

        return MM + "/" + yyyy;
    }
}

