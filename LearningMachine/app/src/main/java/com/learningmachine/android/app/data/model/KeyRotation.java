package com.learningmachine.android.app.data.model;

import org.joda.time.DateTime;

import java.io.Serializable;

public class KeyRotation implements Serializable {

    private String mCreatedDate;
    private String mKey;

    public KeyRotation(String createdDate, String key) {
        mCreatedDate = createdDate;
        mKey = key;
    }

    public String getCreatedDate() {
        return mCreatedDate;
    }

    public DateTime getCreatedDateTime() {
        return DateTime.parse(mCreatedDate);
    }

    public String getKey() {
        return mKey;
    }
}
