package tn.esprit.myapplication.data;

import com.google.firebase.Timestamp;

public class Availability {

    private String id;
    private String doctorId;
    private int dayOfWeek;
    private String dayLabel;
    private String startTime;
    private String endTime;
    private String breakStartTime;
    private String breakEndTime;
    private int sessionDurationMinutes;
    private Timestamp createdAt;

    public Availability() {}

    public Availability(String doctorId, int dayOfWeek, String dayLabel,
                        String startTime, String endTime,
                        String breakStartTime, String breakEndTime,
                        int sessionDurationMinutes, Timestamp createdAt) {

        this.doctorId = doctorId;
        this.dayOfWeek = dayOfWeek;
        this.dayLabel = dayLabel;
        this.startTime = startTime;
        this.endTime = endTime;
        this.breakStartTime = breakStartTime;
        this.breakEndTime = breakEndTime;
        this.sessionDurationMinutes = sessionDurationMinutes;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDoctorId() { return doctorId; }
    public int getDayOfWeek() { return dayOfWeek; }
    public String getDayLabel() { return dayLabel; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getBreakStartTime() { return breakStartTime; }
    public String getBreakEndTime() { return breakEndTime; }
    public int getSessionDurationMinutes() { return sessionDurationMinutes; }
    public Timestamp getCreatedAt() { return createdAt; }
}