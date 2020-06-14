package com.example.xyzreader.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String START_ARTICLE_ID_STATE_KEY = "startArticleId";

    private Cursor mCursor;
    private long mStartId;

    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;

    private int currentPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_article_detail);

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
            }
        } else {
            mStartId = savedInstanceState.getLong(START_ARTICLE_ID_STATE_KEY);
            currentPosition = savedInstanceState.getInt(ArticleListActivity.EXTRA_CURRENT_POSITION);
        }

        getSupportLoaderManager().initLoader(0, null, this);

        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mPager = findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(currentPosition);

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCursor.moveToPosition(position);
                currentPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(START_ARTICLE_ID_STATE_KEY, mStartId);
        outState.putInt(ArticleListActivity.EXTRA_CURRENT_POSITION, currentPosition);
        super.onSaveInstanceState(outState);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {

            mCursor.moveToFirst();
            // TODO: optimize
            while (!mCursor.isAfterLast()) {

                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {

                    final int position = mCursor.getPosition();
                    mPager.setCurrentItem(position, false);
                    break;

                }
                mCursor.moveToNext();

            }
            mStartId = 0;

        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            mCursor.moveToPosition(position);
            return ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
        }

        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}