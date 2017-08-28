package com.sfprojects.android.sfpopularmovies_stage1;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import dalvik.annotation.TestTarget;


public class ImageAdapter extends BaseAdapter {
    private Context mContext;
    private ArrayList<String> mPaths;
    private int mWidth;


    public ImageAdapter(Context context, ArrayList<String> paths, int width){
        mContext = context;
        mPaths = paths;
        mWidth = width;

    }
    @Override
    public int getCount() {
        return mPaths.size();
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
    public View getView(int position, View view, ViewGroup viewGroup) {
        ImageView imageView;

        if (view ==null){
            imageView = new ImageView(mContext);
        }
        else {
            imageView = (ImageView)view;
        }

        //Drawable drawable = resizeDrawable(ContextCompat().getDrawable(getResources(), R.drawable.loading1, null));
        Drawable drawable = resizeDrawable(mContext.getResources().getDrawable(R.drawable.loading1));
        Picasso.with(mContext).load("http://image.tmdb.org/t/p/w185/" + mPaths.get(position))
                .resize(mWidth, (int)(mWidth*1.5)).placeholder(drawable).into(imageView);
        return imageView;
    }
    private Drawable resizeDrawable(Drawable image){
        Bitmap bmp = ((BitmapDrawable)image).getBitmap();
        Bitmap bmpResized = Bitmap.createScaledBitmap(bmp, mWidth, (int)(mWidth*1.5), false);
        return new BitmapDrawable(mContext.getResources(), bmpResized);
    }
}
