/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.simonmacdonald.muzei.comic;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.simonmacdonald.muzei.comic.ComicCoverService.Cover;

import java.util.Random;

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RestAdapter.LogLevel;
import retrofit.RetrofitError;

public class ComicCoverArtSource extends RemoteMuzeiArtSource {
    private static final String TAG = "ComicCoverArtSource";
    private static final String SOURCE_NAME = "ComicCoverArtSource";

    private static final int ROTATE_TIME_MILLIS = 3 * 60 * 60 * 1000; // rotate every 3 hours

    public ComicCoverArtSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://mighty-harbor-8598.herokuapp.com/")
                .setLogLevel(LogLevel.FULL)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addQueryParam("consumer_key", Config.CONSUMER_KEY);
                    }
                })
                .setErrorHandler(new ErrorHandler() {
                    @Override
                    public Throwable handleError(RetrofitError retrofitError) {
                        int statusCode = retrofitError.getResponse().getStatus();
                        if (retrofitError.isNetworkError()
                                || (500 <= statusCode && statusCode < 600)) {
                            return new RetryException();
                        }
                        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
                        return retrofitError;
                    }
                })
                .build();

        ComicCoverService service = restAdapter.create(ComicCoverService.class);
        Cover response = service.getRandomCover();

        if (response == null) {
            throw new RetryException();
        }

//        if (response.photos.size() == 0) {
//            Log.w(TAG, "No photos returned from API.");
//            scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
//            return;
//        }
//
//        Random random = new Random();
//        Photo photo;
//        String token;
//        while (true) {
//            photo = response.photos.get(random.nextInt(response.photos.size()));
//            token = Integer.toString(photo.id);
//            if (response.photos.size() <= 1 || !TextUtils.equals(token, currentToken)) {
//                break;
//            }
//        }
        
        // <img id="image-1" class="cover" src="http://i.annihil.us/u/prod/marvel/i/mg/6/80/5284ea82357da.jpg">

        Log.d(TAG, "title = " + response.title);
        Log.d(TAG, "author = " + response.date);
        Log.d(TAG, "imageUrl = " + response.url);
        
        publishArtwork(new Artwork.Builder()
                .title(response.title)
                .byline("Data provided by Marvel. © 2014 Marvel")
                .imageUri(Uri.parse(response.url))
                //.token(token)
                .viewIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(response.url)))
                .build());

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }
}

