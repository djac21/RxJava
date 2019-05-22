package com.djac21.retrofitrxjava.activities;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;

import com.djac21.retrofitrxjava.R;
import com.djac21.retrofitrxjava.adapter.NewsAdapter;
import com.djac21.retrofitrxjava.api.APIClient;
import com.djac21.retrofitrxjava.api.APIInterface;
import com.djac21.retrofitrxjava.models.NewsModel;
import com.google.gson.Gson;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();
    private final static String API_KEY = "212c1dceeac8453d99337f0062e998f3";
    private String selectedCategory;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    public static final String PREFS_NAME = "sharedPref";
    private CompositeDisposable disposable = new CompositeDisposable();
    private APIInterface apiInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        progressBar = findViewById(R.id.progressBar);
        apiInterface = APIClient.getClient().create(APIInterface.class);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
        boolean dialogShown = sharedPreferences.getBoolean("dialogShown", false);
        if (!dialogShown) {
            aboutDialog();
            sharedPreferences.edit().putBoolean("dialogShown", true).apply();
        }

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                getNews(selectedCategory);
            }
        });

        ArrayAdapter<CharSequence> categoryAdapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.categories, R.layout.text_view_item);
        categoryAdapter.setDropDownViewResource(R.layout.text_view_item);

        final Spinner categorySpinner = findViewById(R.id.spinner);
        categorySpinner.setAdapter(categoryAdapter);
        categorySpinner.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.grey_500)));
        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = (String) categorySpinner.getSelectedItem();

                if (selectedCategory.equals("Top US News"))
                    selectedCategory = null;

                getNews(selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        getNews(selectedCategory);
    }

    public void getNews(String category) {
        disposable.add(
                apiInterface.fetchAllNews(API_KEY, category)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe(new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) {
                                progressBar.setVisibility(View.VISIBLE);
                            }
                        })
                        .subscribeWith(new DisposableSingleObserver<NewsModel>() {
                            @Override
                            public void onSuccess(NewsModel news) {
                                try {
                                    Gson gson = new Gson();
                                    String responderString = gson.toJson(news);
                                    Log.d(TAG, "Full Response: " + responderString);
                                } catch (Exception ex) {
                                    Log.e(TAG, "Error on JSON: " + ex);
                                }

                                List<NewsModel.Articles> newsList = news.getArticles();
                                recyclerView.setAdapter(new NewsAdapter(newsList, getApplicationContext()));

                                swipeRefreshLayout.setRefreshing(false);
                                findViewById(R.id.news_layout).setVisibility(View.VISIBLE);
                                findViewById(R.id.error_layout).setVisibility(View.GONE);
                                progressBar.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG, "onError: " + e.getMessage());
                                swipeRefreshLayout.setRefreshing(false);
                                findViewById(R.id.news_layout).setVisibility(View.GONE);
                                findViewById(R.id.error_layout).setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.GONE);
                            }
                        })
        );
    }

    public void aboutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialog)
                .setTitle("Welcome!")
                .setMessage(R.string.about_dialog)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null);
        builder.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_about)
            aboutDialog();
        else if (id == R.id.action_refresh)
            getNews(selectedCategory);

        return super.onOptionsItemSelected(item);
    }
}