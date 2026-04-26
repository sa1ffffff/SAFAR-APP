package com.safar.app.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class RideAd {

    public static final long EXPIRY_WINDOW_MS = 3L * 24 * 60 * 60 * 1000;
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_FILLED = "FILLED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private String adId;
    private String creatorUid;
    private String creatorName;
    private String creatorPhone;
    private String creatorPhotoUrl;
    private String origin;
    private String destination;
    private double originLat;
    private double originLng;
    private double destinationLat;
    private double destinationLng;
    private long departureTime;
    private int availableSeats;
    private String vehicleDetails;
    private String status;
    private long createdAt;
    private long expiresAt;
    private String notes;

    // Required empty constructor for Firebase
    public RideAd() {}

    public RideAd(String creatorUid, String creatorName, String creatorPhone, String creatorPhotoUrl,
                  String origin, String destination,
                  double originLat, double originLng, double destinationLat, double destinationLng,
                  long departureTime, int availableSeats, String vehicleDetails, String notes) {
        this.creatorUid = creatorUid;
        this.creatorName = creatorName;
        this.creatorPhone = creatorPhone;
        this.creatorPhotoUrl = creatorPhotoUrl;
        this.origin = origin;
        this.destination = destination;
        this.originLat = originLat;
        this.originLng = originLng;
        this.destinationLat = destinationLat;
        this.destinationLng = destinationLng;
        this.departureTime = departureTime;
        this.availableSeats = availableSeats;
        this.vehicleDetails = vehicleDetails;
        this.notes = notes;
        this.status = STATUS_OPEN;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = this.createdAt + EXPIRY_WINDOW_MS;
    }

    // Legacy constructor for backward compatibility
    public RideAd(String creatorUid, String creatorName, String creatorPhone, String origin, String destination,
                  double originLat, double originLng, double destinationLat, double destinationLng,
                  long departureTime, int availableSeats, String vehicleDetails, String notes) {
        this(creatorUid, creatorName, creatorPhone, null, origin, destination,
             originLat, originLng, destinationLat, destinationLng,
             departureTime, availableSeats, vehicleDetails, notes);
    }

    // Getters and Setters
    public String getAdId() { return adId; }
    public void setAdId(String adId) { this.adId = adId; }

    public String getCreatorUid() { return creatorUid; }
    public void setCreatorUid(String creatorUid) { this.creatorUid = creatorUid; }

    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }

    public String getCreatorPhone() { return creatorPhone; }
    public void setCreatorPhone(String creatorPhone) { this.creatorPhone = creatorPhone; }

    public String getCreatorPhotoUrl() { return creatorPhotoUrl; }
    public void setCreatorPhotoUrl(String creatorPhotoUrl) { this.creatorPhotoUrl = creatorPhotoUrl; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public double getOriginLat() { return originLat; }
    public void setOriginLat(double originLat) { this.originLat = originLat; }

    public double getOriginLng() { return originLng; }
    public void setOriginLng(double originLng) { this.originLng = originLng; }

    public double getDestinationLat() { return destinationLat; }
    public void setDestinationLat(double destinationLat) { this.destinationLat = destinationLat; }

    public double getDestinationLng() { return destinationLng; }
    public void setDestinationLng(double destinationLng) { this.destinationLng = destinationLng; }

    public long getDepartureTime() { return departureTime; }
    public void setDepartureTime(long departureTime) { this.departureTime = departureTime; }

    public int getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }

    public String getVehicleDetails() { return vehicleDetails; }
    public void setVehicleDetails(String vehicleDetails) { this.vehicleDetails = vehicleDetails; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Exclude
    public boolean isOpen() {
        return STATUS_OPEN.equals(status);
    }

    @Exclude
    public long getEffectiveExpiresAt() {
        if (expiresAt > 0L) return expiresAt;
        return createdAt + EXPIRY_WINDOW_MS;
    }

    @Exclude
    public boolean isExpired(long nowMillis) {
        return getEffectiveExpiresAt() <= nowMillis;
    }
}
