// File: app/src/main/java/tn/esprit/myapplication/data/Slot.java
package tn.esprit.myapplication.data;

import com.google.firebase.Timestamp;

public class Slot {

    private String id;
    private String doctorId;
    private String patientId;       // null when free
    private String date;            // "yyyy-MM-dd"
    private String time;            // "HH:mm"
    private String status;          // "AVAILABLE", "REQUESTED", "CONFIRMED"
    private Timestamp createdAt;

    public Slot() {}

    public Slot(String doctorId,
                String date,
                String time,
                String status,
                Timestamp createdAt) {
        this.doctorId = doctorId;
        this.date = date;
        this.time = time;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
