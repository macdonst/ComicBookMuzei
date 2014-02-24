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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.simonmacdonald.muzei.comic.ComicCoverService.Cover;

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RestAdapter.LogLevel;
import retrofit.RetrofitError;

public class ComicCoverArtSource extends RemoteMuzeiArtSource {
    private static final String BY_LINE = "Data provided by Marvel. © 2014 Marvel";
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
        
        Log.d(TAG, "wifi = " + Utils.getConfigConnection(this));
        Log.d(TAG, "refresh = " + Utils.getConfigFreq(this));

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
        
        String token;
        if (currentToken == null) {
        	token = "1";
        } else {
        	token = "" + (Integer.valueOf(currentToken) + 1) ;
        }

        Log.d(TAG, "title = " + response.title);
        Log.d(TAG, "author = " + response.date);
        Log.d(TAG, "imageUrl = " + response.url);
        
        publishArtwork(new Artwork.Builder()
                .title(response.title)
                .byline(BY_LINE)
                .imageUri(Uri.parse(response.url))
                .token(token)
                .viewIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(response.url)))
                .build());

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }
}

