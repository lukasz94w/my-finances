package com.example.myexpenses;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class CategoryActivity extends AppCompatActivity {

    private Toolbar toolbar;
    ListView listView;
    String[] categoryNames = {
            "Jedzenie",
            "Samochód",
            "Gry",
            "Sport",
            "Zdrowie",
            "Rozwój osobisty",
            "Hobby",
            "Inne"
    };
    Integer[] categoryNamesId = {
            R.drawable.food,
            R.drawable.car,
            R.drawable.game,
            R.drawable.sport,
            R.drawable.health,
            R.drawable.self_impromevent,
            R.drawable.hobby,
            R.drawable.other
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Wybierz kategorię");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CustomList adapter = new CustomList(CategoryActivity.this, categoryNames, categoryNamesId);
        listView = findViewById(R.id.list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Intent intent = new Intent();
            intent.putExtra("CHOSEN_CATEGORY", categoryNames[position]);
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:{
                finish();
            }
        }
        return super.onOptionsItemSelected(item);
    }

}