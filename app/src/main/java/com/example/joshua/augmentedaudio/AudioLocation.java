package com.example.joshua.augmentedaudio;

/**
 * Created by Joshua on 4/4/2016.
 */
public class AudioLocation {
    int date;
    int radius;
    double longitude;
    double latitude;
    //double longitude;
    //String id;

    public AudioLocation() {
        // empty default constructor, necessary for Firebase to be able to deserialize blog posts
    }
    public int getDate() {
        return date;
    }
    public int getRadius() {
        return radius;
    }
    public double getLatitude() {
        return latitude;
    }
    public double getLongitude(){
        return longitude;
    }
    /*public String getTestID(){
        return id;
    }*/
}