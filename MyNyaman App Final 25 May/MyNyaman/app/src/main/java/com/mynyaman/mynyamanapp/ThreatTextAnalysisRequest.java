package com.mynyaman.mynyamanapp;

public final class ThreatTextAnalysisRequest {
    public final String requestId;
    public final String originalText;
    public final String cleanedText;
    public final String packageName;
    public final String appName;
    public final String direction;
    public final String sender;
    public final String childUid;
    public final String parentUid;
    public final String childName;
    public final long capturedAt;
    public final long appSessionId;
    public final String sourceType;

    public ThreatTextAnalysisRequest(
            String requestId,
            String originalText,
            String cleanedText,
            String packageName,
            String appName,
            String direction,
            String sender,
            String childUid,
            String parentUid,
            String childName,
            long capturedAt,
            long appSessionId,
            String sourceType
    ) {
        this.requestId = requestId == null ? "" : requestId;
        this.originalText = originalText == null ? "" : originalText;
        this.cleanedText = cleanedText == null ? "" : cleanedText;
        this.packageName = packageName == null ? "" : packageName;
        this.appName = appName == null ? "" : appName;
        this.direction = direction == null ? "" : direction;
        this.sender = sender == null ? "" : sender;
        this.childUid = childUid == null ? "" : childUid;
        this.parentUid = parentUid == null ? "" : parentUid;
        this.childName = childName == null ? "" : childName;
        this.capturedAt = capturedAt;
        this.appSessionId = appSessionId;
        this.sourceType = sourceType == null ? "" : sourceType;
    }
}
