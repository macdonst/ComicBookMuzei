/*
 * Copyright (c) 2014 Simon MacDonald
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.simonmacdonald.muzei.comic;

import android.content.Context;
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
        if (Utils.isDownloadOnlyOnWifi(this) && !Utils.isWifiConnected(this)) {
            scheduleUpdate(System.currentTimeMillis() + Utils.getRefreshRate(this));
            return;
    	}
    	
        String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;
        
        final Context that = this;
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
                    	if (retrofitError != null && retrofitError.getResponse() != null) {
                            int statusCode = retrofitError.getResponse().getStatus();
                            if (retrofitError.isNetworkError()
                                    || (500 <= statusCode && statusCode < 600)) {
                                return new RetryException();
                            }
                    	}
                        scheduleUpdate(System.currentTimeMillis() + Utils.getRefreshRate(that));
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

//        Log.d(TAG, "title = " + response.title);
//        Log.d(TAG, "author = " + response.date);
//        Log.d(TAG, "imageUrl = " + response.url);
//        Log.d(TAG, "detailsUrl = " + response.detailsUrl);
        
        if (response.detailsUrl == null) {
        	response.detailsUrl = response.url;
        }
        
        publishArtwork(new Artwork.Builder()
                .title(response.title)
                .byline(BY_LINE)
                .imageUri(Uri.parse(response.url))
                .token(token)
                .viewIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(response.detailsUrl)))
                .build());

        scheduleUpdate(System.currentTimeMillis() + Utils.getRefreshRate(this));
    }
}

