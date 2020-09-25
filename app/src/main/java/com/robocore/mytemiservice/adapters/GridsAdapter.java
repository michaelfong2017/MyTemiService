package com.robocore.mytemiservice.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.robocore.mytemiservice.R;
import com.robocore.mytemiservice.models.Grid;

public class GridsAdapter extends BaseAdapter {
    private final Context mContext;
    LayoutInflater inflter;
    private final int[] grids;

    public GridsAdapter(Context context, int[] grids) {
        this.mContext = context;
        inflter = (LayoutInflater.from(context));
        this.grids = grids;
    }

    @Override
    public int getCount() {
        if (grids == null) {
            return 0;
        }
        return grids.length;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflter.inflate(R.layout.linearlayout_grid, null); // inflate the layout
        ImageView grid_view = (ImageView) view.findViewById(R.id.green_grid_view); // get the reference of ImageView
        grid_view.setImageResource(grids[i]);
        return view;
    }
}
