package com.vam.vam;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Medicine implements Parcelable {
    public static final Creator<Medicine> CREATOR = new Creator<Medicine>() {
        @Override
        public Medicine createFromParcel(Parcel in) {
            return new Medicine(in);
        }

        @Override
        public Medicine[] newArray(int size) {
            return new Medicine[size];
        }
    };

    @JsonProperty("name")
    private String name;

    @JsonProperty("manufacturer")
    private String manufacturer;

    @JsonProperty("usage")
    private String usage;

    @JsonProperty("contents")
    private String contents;

    protected Medicine(Parcel in) {
        name = in.readString();
        manufacturer = in.readString();
        usage = in.readString();
        contents = in.readString();
    }

    Medicine() {
        //default constructor
    }



    public String getName() {
        return name;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getUsage() {
        return usage;
    }

    public String getContents() {
        return contents;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(manufacturer);
        dest.writeString(usage);
        dest.writeString(contents);
    }

    public static class Builder {
        private String name;
        private String contents;
        private String usage;
        private String manufacturer;

        Builder(Medicine medicine) {
            this.name = medicine.name;
            this.contents = medicine.contents;
            this.usage = medicine.usage;
            this.manufacturer = medicine.manufacturer;
        }

        Builder() {

        }

        public Medicine build() {
            Medicine medicine = new Medicine();
            medicine.contents = this.contents;
            medicine.manufacturer = this.manufacturer;
            medicine.usage = this.usage;
            medicine.name = this.name;

            return medicine;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setContents(String contents) {
            this.contents = contents;
            return this;
        }

        public Builder setUsage(String usage) {
            this.usage = usage;
            return this;
        }

        public Builder setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
            return this;
        }
    }
}
