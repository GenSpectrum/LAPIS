package ch.ethz.lapis.source.gisaid;

import java.util.Objects;

public class GeoLocation {
    private String region;
    private String country;
    private String division;
    private String location;

    public GeoLocation() {
    }

    public GeoLocation(String region, String country, String division, String location) {
        this.region = region;
        this.country = country;
        this.division = division;
        this.location = location;
    }

    public String getRegion() {
        return region;
    }

    public GeoLocation setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public GeoLocation setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getDivision() {
        return division;
    }

    public GeoLocation setDivision(String division) {
        this.division = division;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public GeoLocation setLocation(String location) {
        this.location = location;
        return this;
    }

    public String[] toArray() {
        return new String[]{ region, country, division, location };
    }

    public static GeoLocation fromArray(String[] arr) {
        return new GeoLocation(arr[0], arr[1], arr[2], arr[3]);
    }

    @Override
    public String toString() {
        return "GeoLocation{" +
                "region='" + region + '\'' +
                ", country='" + country + '\'' +
                ", division='" + division + '\'' +
                ", location='" + location + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeoLocation that = (GeoLocation) o;
        return Objects.equals(region, that.region) && Objects.equals(country, that.country)
                && Objects.equals(division, that.division) && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, country, division, location);
    }
}
