package com.safar.app.models;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class JoinRequest {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";

    private String requestId;
    private String adId;
    private String requestorUid;
    private String requestorName;
    private String requestorPhone;
    private String adCreatorUid;
    private String status;
    private long timestamp;
    private String chatRoomId;

    public JoinRequest() {}

    public JoinRequest(String adId, String requestorUid, String requestorName,
                       String requestorPhone, String adCreatorUid) {
        this.adId = adId;
        this.requestorUid = requestorUid;
        this.requestorName = requestorName;
        this.requestorPhone = requestorPhone;
        this.adCreatorUid = adCreatorUid;
        this.status = STATUS_PENDING;
        this.timestamp = System.currentTimeMillis();
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getAdId() { return adId; }
    public void setAdId(String adId) { this.adId = adId; }

    public String getRequestorUid() { return requestorUid; }
    public void setRequestorUid(String requestorUid) { this.requestorUid = requestorUid; }

    public String getRequestorName() { return requestorName; }
    public void setRequestorName(String requestorName) { this.requestorName = requestorName; }

    public String getRequestorPhone() { return requestorPhone; }
    public void setRequestorPhone(String requestorPhone) { this.requestorPhone = requestorPhone; }

    public String getAdCreatorUid() { return adCreatorUid; }
    public void setAdCreatorUid(String adCreatorUid) { this.adCreatorUid = adCreatorUid; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getChatRoomId() { return chatRoomId; }
    public void setChatRoomId(String chatRoomId) { this.chatRoomId = chatRoomId; }
}
