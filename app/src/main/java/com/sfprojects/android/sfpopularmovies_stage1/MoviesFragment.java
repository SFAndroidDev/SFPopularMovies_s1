package com.sfprojects.android.sfpopularmovies_stage1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.graphics.Point;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;


public class MoviesFragment extends Fragment {
    public GridView gridView;
    public static int posterWidth; // width of the poster
    public static ArrayList<String> postersPopular;
    public static boolean sortByPopularity = true;
    public static boolean sortByFavorites;
    public static String API_KEY = "";// you need to insert your own API key here to use the app

    public static PreferenceChangeListener listener;
    public static SharedPreferences preferences;
    public static ArrayList<String> postersFavorites = new ArrayList<String>();

    public static ArrayList<String> overviews;
    public static ArrayList<String> ids;
    public static ArrayList<String> titles;
    public static ArrayList<String> dates;
    public static ArrayList<String> rating;
    public static ArrayList<String> youtubeLinks;
    public static ArrayList<String> youtubeLinks2;
    public static ArrayList<Boolean> favorited;
    public static ArrayList<ArrayList<String>> comments;



    public MoviesFragment() {

    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View ourView = inflater.inflate(R.layout.fragment_movies, container, false);

        WindowManager windowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        if (MainActivity.TABLET){ // if the device is a tablet
            posterWidth = size.x/6;
        }
        else posterWidth = size.x/3; // if the device is a phone

        if (getActivity() != null){
            ArrayList<String> arrayList = new ArrayList<String>();
            ImageAdapter imageAdapter = new ImageAdapter(getActivity(),arrayList, posterWidth);
            gridView = (GridView)ourView.findViewById(R.id.gv_moviesDisplayId);

            gridView.setColumnWidth(posterWidth);
            gridView.setAdapter(imageAdapter);

        }
        //listener on clicks on items in the gridview
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                favorited = new ArrayList<Boolean>();
                for (int j = 0; j <titles.size(); j++){
                    favorited.add(false);
                }
                Intent intent = new Intent(getActivity(), DetailsActivity.class)
                        .putExtra("overview",overviews.get(position))
                        .putExtra("poster",postersPopular.get(position))
                        .putExtra("ids",ids.get(position))
                        .putExtra("title", titles.get(position))
                        .putExtra("dates", dates.get(position))
                        .putExtra("rating", rating.get(position))
                        .putExtra("youtube", youtubeLinks.get(position))
                        .putExtra("youtube2", youtubeLinks2.get(position))
                        .putExtra("comments", comments.get(position))
                        .putExtra("favorite", favorited.get(position));
                startActivity(intent);


            }

        });


        return ourView;
    }

    private  class PreferenceChangeListener implements  SharedPreferences.OnSharedPreferenceChangeListener{

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            gridView.setAdapter(null);
            onStart();
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        listener = new PreferenceChangeListener();
        preferences.registerOnSharedPreferenceChangeListener(listener);

        if (preferences.getString("sortby","popularity").equals("popularity")){
            getActivity().setTitle("Most popular movies");
            sortByPopularity = true;
            sortByFavorites = false;
        }
        else if (preferences.getString("sortby","rating").equals("rating")){
            getActivity().setTitle("Top rated movies");
            sortByPopularity = false;
            sortByFavorites = false;
        }
        else if (preferences.getString("sortby","favorites").equals("favorite")){
            getActivity().setTitle("Your favorite movies");
            sortByPopularity = false;
            sortByFavorites = true;
        }

        TextView noFavorites = new TextView(getActivity());
        FrameLayout frameLayout = (FrameLayout) getActivity().findViewById(R.id.fragmentLayoutId);
        if (sortByFavorites){
            if (postersFavorites.size()==0){
                noFavorites.setText("No favorites in the Your Favorite List");
                if (frameLayout.getChildCount()==1){
                    frameLayout.addView(noFavorites);
                }
                gridView.setVisibility(GridView.GONE);
            }
            else {
                gridView.setVisibility(GridView.VISIBLE);
                frameLayout.removeView(noFavorites);
            }

            if (postersFavorites != null && getActivity() != null){
                ImageAdapter imageAdapter = new ImageAdapter(getActivity(),postersFavorites,posterWidth);
                gridView.setAdapter(imageAdapter);
            }
        }
        else {
            gridView.setVisibility(GridView.VISIBLE);
            frameLayout.removeView(noFavorites);
        }


        if (isNetworkAvailable()){

            gridView.setVisibility(GridView.VISIBLE);
            new ImageLoadTask().execute();
        }
        else {
            TextView textView = new TextView(getActivity());
            FrameLayout fragmentLayout = (FrameLayout) getActivity().findViewById(R.id.fragmentLayoutId);
            textView.setText("Not connected to the Internet !");
            if (fragmentLayout.getChildCount() == 1){

                fragmentLayout.addView(textView);
            }
            gridView.setVisibility(GridView.GONE);
        }
    }

    public  Boolean isNetworkAvailable(){
        ConnectivityManager connectivityManager = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();

    }

    public class ImageLoadTask extends AsyncTask<Void, Void, ArrayList<String>>{

        @Override
        protected ArrayList<String> doInBackground(Void... voids) {
            while(true){
                try{

                    postersPopular = new ArrayList<>(Arrays.asList(getPathsFromAPI(sortByPopularity)));
                    return postersPopular;
                }
                catch (Exception e){

                    continue;
                }

            }

        }


        public  String[] getPathsFromAPI (boolean sortbypopularity){

            while(true){
                HttpURLConnection urlConnection = null;
                BufferedReader bufferedReader = null;
                String JSONresult; // will contain the result of the API request

                try{
                    String urlAsking = null;

                    if (sortbypopularity){
                        urlAsking = "http://api.themoviedb.org/3/discover/movie?sort_by=popularity.desc&api_key="+API_KEY;
                    }
                    else {
                        urlAsking = "http://api.themoviedb.org/3/discover/movie?sort_by=vote_average.desc&vote_count.gte=500&api_key="+API_KEY;
                    }

                    URL urlRequest = new URL(urlAsking);
                    urlConnection = (HttpURLConnection) urlRequest.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect(); //connection to access the urlRequest that we want

                    //read the inputstream into a string
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer stringBuffer = new StringBuffer();
                    if (inputStream == null){
                        return null;
                    }
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while((line = bufferedReader.readLine())!= null){
                        stringBuffer.append(line+"\n");
                    }
                    if (stringBuffer.length() == 0){
                        return null;
                    }
                    JSONresult = stringBuffer.toString();

                    try {
                        overviews = new ArrayList<String>(Arrays.asList(getStringsFromJSON(JSONresult,"overview")));
                        titles = new ArrayList<String>(Arrays.asList(getStringsFromJSON(JSONresult,"original_title")));
                        rating = new ArrayList<String>(Arrays.asList(getStringsFromJSON(JSONresult,"vote_average")));
                        dates = new ArrayList<String>(Arrays.asList(getStringsFromJSON(JSONresult,"release_date")));
                        ids = new ArrayList<String>(Arrays.asList(getStringsFromJSON(JSONresult,"id")));
                        //youtubeLinks = new ArrayList<String>(Arrays.asList(getStringsFromJSON(JSONresult,"overview")));
                        //youtubeLinks2 = new ArrayList<String>(Arrays.asList(getStringsFromJSON(JSONresult,"overview")));

                        while (true) {
                            youtubeLinks = new ArrayList<String>(Arrays.asList(getYoutubeLinksFromIds(ids,0)));
                            youtubeLinks2 = new ArrayList<String>(Arrays.asList(getYoutubeLinksFromIds(ids,1)));
                            int nullCount = 0;

                            //verification for the first Youtube link
                            for (int i = 0; i<youtubeLinks.size(); i++){
                                if (youtubeLinks.get(i) == null){
                                    nullCount++;
                                    youtubeLinks.set(i,"Sorry, no video found");

                                }
                            }

                            //verification for the second Youtube link
                            for (int i = 0; i<youtubeLinks2.size(); i++){
                                if (youtubeLinks2.get(i) == null){
                                    nullCount++;
                                    youtubeLinks2.set(i,"Sorry, no video found");

                                }
                            }

                            //continue the loop in case both equal null
                            if (nullCount>2) continue;
                            break;
                        }
                        comments = getReviewsFromIds(ids);

                        return getPathsFromJSON(JSONresult);

                    } catch (JSONException jsone){
                        return null;
                    }


                } catch (Exception e){
                    continue;
                } finally {
                    if (urlConnection !=null){
                        urlConnection.disconnect();
                    }
                    if (bufferedReader != null){
                        try {
                            bufferedReader.close();

                        } catch (final  IOException e){

                        }
                    }
                }

            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> result){
            if (result !=null && getActivity() !=null){
                ImageAdapter adapter = new ImageAdapter(getActivity(),result, posterWidth);
                gridView.setAdapter(adapter);
            }
        }

        public String[] getYoutubeLinksFromIds(ArrayList<String> ids, int position){ //position is to specify if it's the first or the second youtubeLink
            String[] results = new String[ids.size()];

            for (int i = 0; i<ids.size(); i++){
                HttpURLConnection urlConnection = null;
                BufferedReader bufferedReader = null;
                String JSONresult; // will contain the result of the API request

                try {
                    String urlAsking = null;
                    urlAsking = "http://api.themoviedb.org/3/movie/" + ids.get(i) + "/videos?api_key=" + API_KEY;

                    URL urlRequest = new URL(urlAsking);
                    urlConnection = (HttpURLConnection) urlRequest.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect(); //connection to access the urlRequest that we want

                    //read the inputstream into a string
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer stringBuffer = new StringBuffer();
                    if (inputStream == null) {
                        return null;
                    }
                    bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuffer.append(line + "\n");
                    }
                    if (stringBuffer.length() == 0) {
                        return null;
                    }
                    JSONresult = stringBuffer.toString();

                    try {
                        results[i] = getYoutubeLinksFromJSON(JSONresult, position);

                    } catch (JSONException E) {
                        results[i] = "no video found";
                    }

                } catch (Exception e) {

                } finally {
                        if (urlConnection !=null){
                            urlConnection.disconnect();
                        }
                        if (bufferedReader != null){
                            try {
                                bufferedReader.close();

                            } catch (final  IOException e){

                            }
                        }
                    }
                }
                return results;


        }

        public String getYoutubeLinksFromJSON(String JSONStringParam, int position) throws JSONException
        {
            JSONObject JSONString = new JSONObject(JSONStringParam);
            JSONArray youtubesArray = JSONString.getJSONArray("results");
            JSONObject youtubeLink;
            String result = "Sorry, no video found";
            if(position == 0) // first youtubeLink
            {
                youtubeLink = youtubesArray.getJSONObject(0);
                result = youtubeLink.getString("key");
            }
            else if(position==1) // second youtubeLink
            {
                if(youtubesArray.length()>1)
                {
                    youtubeLink = youtubesArray.getJSONObject(1);
                }
                else{
                    youtubeLink = youtubesArray.getJSONObject(0);
                }
                result = youtubeLink.getString("key");
            }
            return result;
        }

        public ArrayList<ArrayList<String>> getReviewsFromIds(ArrayList<String> ids)
        {
            outerloop:
            while(true) {

                ArrayList<ArrayList<String>> results = new ArrayList<>();
                for(int i =0; i<ids.size(); i++) {

                    HttpURLConnection urlConnection = null;
                    BufferedReader bufferedReader = null;
                    String JSONResult;

                    try {
                        String urlString = null;
                        urlString = "http://api.themoviedb.org/3/movie/" + ids.get(i) + "/reviews?api_key=" + API_KEY;
                        URL url = new URL(urlString);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        urlConnection.setRequestMethod("GET");
                        urlConnection.connect();

                        //Read the input stream into a String
                        InputStream inputStream = urlConnection.getInputStream();
                        StringBuffer buffer = new StringBuffer();
                        if (inputStream == null) {
                            return null;
                        }
                        bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            buffer.append(line + "\n");
                        }
                        if (buffer.length() == 0) {
                            return null;
                        }
                        JSONResult = buffer.toString();
                        try {
                            results.add(getCommentsFromJSON(JSONResult));
                        } catch (JSONException E) {
                            return null;
                        }
                    } catch (Exception e) {
                        continue outerloop;

                    } finally {
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                        if (bufferedReader != null) {
                            try {
                                bufferedReader.close();
                            } catch (final IOException e) {
                            }
                        }
                    }
                }
                return results;

            }
        }

        public ArrayList<String> getCommentsFromJSON(String JSONStringParam)throws JSONException{

            JSONObject JSONString = new JSONObject(JSONStringParam);
            JSONArray reviewsArray = JSONString.getJSONArray("results");
            ArrayList<String> results = new ArrayList<>();
            if(reviewsArray.length()==0) {

                results.add("Sorry, no reviews found for this movie");
                return results;
            }
            for(int i = 0; i<reviewsArray.length(); i++) {

                JSONObject result = reviewsArray.getJSONObject(i);
                results.add(result.getString("content"));
            }
            return results;

        }

        public String[] getStringsFromJSON(String JSONStringParam, String param) throws JSONException{
            JSONObject JSONString = new JSONObject(JSONStringParam);

            JSONArray moviesArray = JSONString.getJSONArray("results");
            String[] result = new String[moviesArray.length()];

            for (int i = 0; i<moviesArray.length(); i++){

                JSONObject movie = moviesArray.getJSONObject(i);
                if (param.equals("vote_average")){
                    Double vote = movie.getDouble("vote_average");
                    String rating = Double.toString(vote)+"/10";
                    result[i] = rating;
                }


                String data = movie.getString(param);
                result[i] = data;
            }

            return result;

        }

        public String[] getPathsFromJSON(String JSONStringParam) throws  JSONException{
            JSONObject JSONString = new JSONObject(JSONStringParam);

            JSONArray moviesArray = JSONString.getJSONArray("results");
            String[] result = new String[moviesArray.length()];

            for (int i = 0; i<moviesArray.length(); i++){

                JSONObject movie = moviesArray.getJSONObject(i);
                String moviePosterPath = movie.getString("poster_path");
                result[i] = moviePosterPath;
            }

            return result;
        }
    }
}




















