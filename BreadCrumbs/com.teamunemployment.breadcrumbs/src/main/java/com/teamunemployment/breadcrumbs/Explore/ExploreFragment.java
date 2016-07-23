package com.teamunemployment.breadcrumbs.Explore;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.teamunemployment.breadcrumbs.PreferencesAPI;
import com.teamunemployment.breadcrumbs.R;
import com.teamunemployment.breadcrumbs.Trails.TrailManagerWorker;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * @author Josiah Kendall.
 */
public class ExploreFragment extends Fragment implements ViewContract {
    private final String TAG = "EXPLORE";
    private ArrayList<String> ids = new ArrayList<>();

    @Bind(R.id.root) CoordinatorLayout root;
    @Bind(R.id.my_recycler_view) RecyclerView recyclerView;

    private View rootView;
    private Context context;
    private PreferencesAPI preferencesAPI;
    private Presenter presenter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rootView != null) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null)
                parent.removeView(rootView);
        }
        rootView = inflater.inflate(R.layout.explore_fragment, container, false);
        context = rootView.getContext();
        ButterKnife.bind(this, rootView);

        preferencesAPI = new PreferencesAPI(context);
        presenter = new Presenter(context, this);
        long userId = Long.parseLong(preferencesAPI.GetUserId());
        presenter.Start(userId);
        return rootView;
    }

    @Override
    public void ShowMessage(String message) {
        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void SetRecyclerViewAdapter(final RecyclerView.Adapter adapter) {
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
        AppCompatActivity appCompatActivity = (AppCompatActivity) context;
        appCompatActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.setAdapter(adapter);
                recyclerView.setLayoutManager(layoutManager);

                // Improves perf if we do not change the size of the Recycler view over the course of its lifecycle.
                recyclerView.setHasFixedSize(true);
            }
        });


    }
}
