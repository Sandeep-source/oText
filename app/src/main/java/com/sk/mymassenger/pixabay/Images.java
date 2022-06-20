package com.sk.mymassenger.pixabay;


import android.net.Uri;

import com.google.gson.annotations.SerializedName;

public class Images {
    int id;

    public String getLargeImage() {
        return largeImage;
    }

    public void setLargeImage(String largeImage) {
        this.largeImage = largeImage;
    }

    @SerializedName("largeImageURL")
    private String largeImage;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPreviewURL() {
        return previewURL;
    }

    public void setPreviewURL(String previewURL) {
        this.previewURL = previewURL;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }


    String previewURL;
    String tags;
}
