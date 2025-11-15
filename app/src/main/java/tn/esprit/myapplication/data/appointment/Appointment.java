package tn.esprit.myapplication.data.appointment;

/**
 * Appointment
 *
 * Core calendar item: links a doctor and a patient with start/end times and status.
 * Stored in the "appointments" collection.
 */
public class Appointment {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    private String id;
    private String doctorId;
    private String patientId;
    private long   startAt;  // epoch millis
    private long   endAt;    // epoch millis
    private String status;
    private String title;
    private String notes;

    // Empty constructor required by Firestore
    public Appointment() {
    }

    public Appointment(String id,
                       String doctorId,
                       String patientId,
                       long startAt,
                       long endAt,
                       String status,
                       String title,
                       String notes) {
        this.id = id;
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.title = title;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public long getStartAt() { return startAt; }
    public void setStartAt(long startAt) { this.startAt = startAt; }

    public long getEndAt() { return endAt; }
    public void setEndAt(long endAt) { this.endAt = endAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
