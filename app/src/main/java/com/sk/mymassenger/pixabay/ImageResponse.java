package com.sk.mymassenger.pixabay;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ImageResponse {
    public List<Images> getImagesList() {
        return imagesList;
    }

    public void setImagesList(List<Images> imagesList) {
        this.imagesList = imagesList;
    }

    @SerializedName("hits")
    List<Images> imagesList;

}
