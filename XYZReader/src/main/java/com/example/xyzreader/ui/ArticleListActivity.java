package com.example.xyzreader.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener {

    public static final String EXTRA_START_POSITION = "extra_start";
    public static final String EXTRA_CURRENT_POSITION = "extra_current";

    private static final String TAG = ArticleListActivity.class.toString();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private final SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private boolean isRefreshing = false;


    private final BroadcastReceiver refreshBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                isRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);

        recyclerView = findViewById(R.id.recycler_view);
        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            onRefresh();
        }

    }

    public void onRefresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(refreshBroadcastReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(refreshBroadcastReceiver);
    }

    private void updateRefreshingUI() {
        swipeRefreshLayout.setRefreshing(isRefreshing);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        recyclerView.setAdapter(adapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(sglm);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        recyclerView.setAdapter(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int id = menuItem.getItemId();
        if (id == android.R.id.home) {
            ActivityCompat.finishAfterTransition(this);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final ImageView thumbnailView;
        public final TextView titleView;
        public final TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private final Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Bundle bundle = null;

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        bundle = ActivityOptionsCompat
                                .makeSceneTransitionAnimation(
                                        ArticleListActivity.this,
                                        vh.thumbnailView,
                                        vh.thumbnailView.getTransitionName()
                                ).toBundle();
                    }

                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition())));

                    intent.putExtra(EXTRA_START_POSITION, vh.getAdapterPosition());

                    startActivity(intent, bundle);
                }
            });
            return vh;
        }

        private Date parsePublishedDate() {
            try {
                String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);

            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            String transitionName = getString(R.string.shared_transition) + position;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                holder.thumbnailView.setTransitionName(transitionName);
            }

            holder.thumbnailView.setTag(transitionName);

            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                                + "<br/>" + " by "
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)));
            }

            Picasso.get()
                    .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                    .resize(600, 400)
                    .centerCrop()
                    .into(holder.thumbnailView);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }
}
