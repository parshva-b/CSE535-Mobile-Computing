package com.example.cse5351;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import com.google.android.material.slider.Slider;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class Symptoms_screen extends AppCompatActivity {

    String[] inputs = {"Nausea", "Headache", "Diarrhea", "Soar Throat", "Fever", "Muscle Ache", "Loss of smell or taste", "Cough", "Shortness of Breath", "Feeling tired"};

    String timestamp = null;

    // dropdown list variables
    AutoCompleteTextView autoCompleteTextView;
    ArrayAdapter<String> arrayAdapter;
    String curSelection = null;

    // slider variables
    Slider slider;
    float rating = 0;

    // current selections
    Map<String, Float> symptoms = new Hashtable<>();
    TextView curSelectionsSoFar;

    // Buttons
    Button pushListToDB, addSymptomToList;

    DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptoms_screen);

        databaseHelper = new DatabaseHelper(Symptoms_screen.this);

        setupDropDownSymptomList();
        setupSliderRanking();

        setupButtonToAddToList();
        setupPushListToDBButton();
    }

    private Map<String, String> prepSymptomListForDBconn() {
        Map<String, String> dbSymptomsMap = new HashMap<>();
        for(String x: inputs) dbSymptomsMap.put(x, String.valueOf(0.0f));

        for(String key: symptoms.keySet()) dbSymptomsMap.put(key, String.valueOf(symptoms.get(key)));

        return dbSymptomsMap;
    }

    private void setupPushListToDBButton() {
        pushListToDB = findViewById(R.id.PushDataToDB);

        pushListToDB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /**
                 * push data to database
                 */
                Map<String, String> map = prepSymptomListForDBconn();
                databaseHelper.setMap(map);
                databaseHelper.addItems();
                Toast.makeText(Symptoms_screen.this, "Push to DB successful", Toast.LENGTH_SHORT).show();
                cleanUpScreen();
            }
        });
    }

    private String dataFromMap() {
        StringBuilder sb = new StringBuilder();
        for(String key: symptoms.keySet()) {
            sb.append(key + ": " + symptoms.get(key) + "\n");
        }
        return sb.toString();
    }

    private void setTextViewForSelectedMap() {
        curSelectionsSoFar = findViewById(R.id.dataSelectedSoFar);
        curSelectionsSoFar.setText(dataFromMap());
    }

    private void setupButtonToAddToList() {
        addSymptomToList = findViewById(R.id.addToSymptoms);

        addSymptomToList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /**
                 * add value from dropDown list and slider to list
                 * ensure and perform null checks for if dropDown is empty and slider is empty
                 */

                if(getCurSelection() != null && getRating() > 0) {
                    symptoms.put(getCurSelection(), getRating());
                    setTextViewForSelectedMap();
                } else {
                    Toast.makeText(Symptoms_screen.this, "Please ensure all selections are made", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void cleanUpScreen() {
        setCurSelection(null);
        symptoms.clear();
        setTextViewForSelectedMap();
        slider.setValue(0.0f);
    }

    /**
     * the function is to setupDropdown slider
     * refactored into its own function for sake of simplicity
     */
    private void setupSliderRanking() {
        slider = findViewById(R.id.discrete_slider);
        slider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
//                Toast.makeText(Symptoms_screen.this, String.valueOf(slider.getValue()), Toast.LENGTH_SHORT).show();
                // setvalue of slider from here
                setRating(slider.getValue());
            }
        });
    }

    /**
     * the function is to setupDropdown list and register item click listener on each item
     * refactored into its own function for sake of simplicity
     */
    private void setupDropDownSymptomList() {
        autoCompleteTextView = findViewById(R.id.auto_complete_textview);
        arrayAdapter = new ArrayAdapter<>(this, R.layout.symptom_list, inputs);

        autoCompleteTextView.setAdapter(arrayAdapter);

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String item = adapterView.getItemAtPosition(position).toString();
                setCurSelection(item);
//                Toast.makeText(Symptoms_screen.this, "Symptom: "+item+" selected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setCurSelection(String curSelection) {
        this.curSelection = curSelection;
    }

    private String getCurSelection() {
        if(TextUtils.isEmpty(this.curSelection)) return null;
        return this.curSelection;
    }

    private void setRating(float val) {
        this.rating = val;
    }

    private float getRating() {
        return this.rating;
    }
}