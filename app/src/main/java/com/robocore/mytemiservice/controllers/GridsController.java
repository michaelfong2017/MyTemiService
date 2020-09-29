package com.robocore.mytemiservice.controllers;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.robocore.mytemiservice.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridsController {
    private static final String TAG = GridsController.class.getSimpleName();
    /**
     * TableLayout
     */
    private List<String> packageNames = new ArrayList<>(Arrays.asList("com.robocore.androidtesting", "Córdoba", "La Plata"));
    private List<String> myServices = new ArrayList<>(Arrays.asList("Secret Camera", "Object Detection", "La Plata", "hahahaha"));
    private TableLayout tableLayout;

    /**
     * State
     */
    Map<Integer, Map<Integer, GridState>> gridStates = new HashMap<>();

    private Context context;
    private Activity activity;
    public GridsController(Context context) {
        this.context = context;
        this.activity = (Activity)context;
    }

    public void initializeTableLayout() {
        Log.d(TAG, "initializeTableLayout()");
        tableLayout = activity.findViewById(R.id.table_layout);

        // All text in first row
        TableRow firstRow = new TableRow(context);
        firstRow.addView(new View(context));
        for (int i = 0; i < myServices.size(); i++) {
            TextView textView = new TextView(context);
            textView.setText(myServices.get(i));
            textView.setTextSize(20);
            textView.setGravity(Gravity.CENTER);
            firstRow.addView(textView);
        }
        tableLayout.addView(firstRow);

        for (int i = 0; i < packageNames.size(); i++) {
            gridStates.put(i, new HashMap<>());
            TableRow tableRow = new TableRow(context);
            tableRow.setGravity(Gravity.CENTER);

            // Text in each row
            TextView textView = new TextView(context);
            textView.setText(packageNames.get(i));
            textView.setTextSize(20);
            textView.setGravity(Gravity.RIGHT);
            textView.setPadding(0, 0, 5, 0);
            tableRow.addView(textView);


            for (int j = 0; j < myServices.size(); j++) {
                gridStates.get(i).put(j, GridState.ON);

                ImageButton imageButton = new ImageButton(context);
                imageButton.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
                imageButton.setBackgroundResource(R.drawable.green_grid);
                imageButton.setTag(new Pair<>(i, j));

                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Pair tag = (Pair)imageButton.getTag();

                        Log.d(TAG, String.format("onClick() [%s, %s]", tag.first, tag.second));

                        GridState gridState = gridStates.get(tag.first).get(tag.second);
                        if (gridState == GridState.ON) {
                            Map gridRow = gridStates.get(tag.first);
                            gridRow.put(tag.second, GridState.OFF);
                            imageButton.setBackgroundResource(R.drawable.red_grid);

                        }
                        else if (gridState == GridState.OFF) {
                            Map gridRow = gridStates.get(tag.first);
                            gridRow.put(tag.second, GridState.ON);
                            imageButton.setBackgroundResource(R.drawable.green_grid);

                        }

                    }
                });

                tableRow.addView(imageButton);
            }
            tableLayout.addView(tableRow);
        }

    }
}

enum GridState {
    ON,
    OFF,
    DISCONNECTED
}
