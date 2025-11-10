package tn.esprit.myapplication.data;

/**
 * Enum for user roles. When storing in Firestore, use role.name().
 * When reading, convert from string safely with fromString.
 */
public enum Role {
    DOCTOR, PATIENT;

    public static Role fromString(String value) {
        if (value == null) return PATIENT;
        try {
            return Role.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PATIENT;
        }
    }
}
