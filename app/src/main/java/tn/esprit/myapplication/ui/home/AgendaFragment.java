// File: app/src/main/java/tn/esprit/myapplication/ui/home/AgendaFragment.java
package tn.esprit.myapplication.ui.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import tn.esprit.myapplication.R;
import tn.esprit.myapplication.core.FirebaseManager;
import tn.esprit.myapplication.data.Availability;
import tn.esprit.myapplication.data.Role;
import tn.esprit.myapplication.data.Slot;

public class AgendaFragment extends Fragment {

    // Mode switch
    private MaterialButtonToggleGroup groupMode;
    private NestedScrollView containerAvailability;
    private LinearLayout containerAgenda;
    private View modeRoot;

    // Swipe detection
    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    // Availability form
    private MaterialButtonToggleGroup groupDays;
    private NumberPicker npStartHour, npStartMinute,
            npEndHour, npEndMinute,
            npBreakStartHour, npBreakStartMinute,
            npBreakEndHour, npBreakEndMinute,
            npSessionDuration;
    private Button btnSave;
    private Button btnResetAvailability;
    private LinearLayout formContainer;

    // Agenda view
    private CalendarView calendarView;
    private RecyclerView rvSessions;
    private TextView tvSelectedDate;
    private TextView tvSessionsTitle;

    private final List<Availability> allAvailabilities = new ArrayList<>();
    private final List<String> sessionSlots = new ArrayList<>();
    private SessionAdapter sessionAdapter;

    // Mapping index → value (1..7)
    private static final int[] DAY_VALUES = {1, 2, 3, 4, 5, 6, 7};
    private static final String[] DAY_LABELS = {
            "Monday", "Tuesday", "Wednesday", "Thursday",
            "Friday", "Saturday", "Sunday"
    };

    // Form button IDs
    private static final int[] DAY_BUTTON_IDS = {
            R.id.btn_day_mon, R.id.btn_day_tue, R.id.btn_day_wed,
            R.id.btn_day_thu, R.id.btn_day_fri, R.id.btn_day_sat,
            R.id.btn_day_sun
    };

    // Session durations allowed
    private static final String[] SESSION_MINUTES_VALUES =
            {"10", "15", "20", "30", "45", "60"};

    public AgendaFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_agenda, container, false);

        // Layout references
        groupMode = root.findViewById(R.id.group_mode);
        containerAvailability = root.findViewById(R.id.container_availability);
        containerAgenda = root.findViewById(R.id.container_agenda);
        modeRoot = root.findViewById(R.id.mode_root);

        groupDays = root.findViewById(R.id.group_days);
        formContainer = root.findViewById(R.id.agenda_form_container);

        npStartHour = root.findViewById(R.id.np_start_hour);
        npStartMinute = root.findViewById(R.id.np_start_minute);
        npEndHour = root.findViewById(R.id.np_end_hour);
        npEndMinute = root.findViewById(R.id.np_end_minute);
        npBreakStartHour = root.findViewById(R.id.np_break_start_hour);
        npBreakStartMinute = root.findViewById(R.id.np_break_start_minute);
        npBreakEndHour = root.findViewById(R.id.np_break_end_hour);
        npBreakEndMinute = root.findViewById(R.id.np_break_end_minute);
        npSessionDuration = root.findViewById(R.id.np_session_duration);

        btnSave = root.findViewById(R.id.btn_save_availability);
        btnResetAvailability = root.findViewById(R.id.btn_reset_availability);

        calendarView = root.findViewById(R.id.calendar_view);
        rvSessions = root.findViewById(R.id.rv_sessions);
        tvSelectedDate = root.findViewById(R.id.tv_selected_date);
        tvSessionsTitle = root.findViewById(R.id.tv_sessions_title);

        setupSwipeGesture();
        setupModeToggle();
        setupDayButtons();
        setupNumberPickers();
        setupSessionsRecycler();
        setupCalendarInteraction();
        setupSaveButton();
        setupResetButton();
        checkRoleThenSetupForm();

        loadAvailabilities();

        return root;
    }

    // ------------------------ Swipe ------------------------

    private void setupSwipeGesture() {
        gestureDetector = new GestureDetector(requireContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) return false;
                        float diffX = e2.getX() - e1.getX();
                        if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                                Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX < 0) {
                                groupMode.check(R.id.btn_mode_agenda);
                            } else {
                                groupMode.check(R.id.btn_mode_availability);
                            }
                            return true;
                        }
                        return false;
                    }
                });

        modeRoot.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        });
    }

    // ------------------------ Mode switching ------------------------

    private void setupModeToggle() {
        groupMode.check(R.id.btn_mode_availability);

        groupMode.addOnButtonCheckedListener((g, id, isChecked) -> {
            if (!isChecked) return;

            if (id == R.id.btn_mode_availability) {
                containerAvailability.setVisibility(View.VISIBLE);
                containerAgenda.setVisibility(View.GONE);
            } else {
                containerAvailability.setVisibility(View.GONE);
                containerAgenda.setVisibility(View.VISIBLE);
                updateSessionsForDate(calendarView.getDate());
            }
        });
    }

    // ------------------------ Form setup ------------------------

    private void setupDayButtons() {
        // No default ⇒ doctor explicitly selects days
        groupDays.clearChecked();
    }

    private void setupNumberPickers() {
        String[] minutes = {"00", "15", "30", "45"};

        NumberPicker[] mins = {
                npStartMinute, npEndMinute,
                npBreakStartMinute, npBreakEndMinute
        };

        for (NumberPicker np : mins) {
            np.setMinValue(0);
            np.setMaxValue(minutes.length - 1);
            np.setDisplayedValues(minutes);
        }

        npStartHour.setMinValue(0);  npStartHour.setMaxValue(23);
        npEndHour.setMinValue(0);    npEndHour.setMaxValue(23);
        npBreakStartHour.setMinValue(0); npBreakStartHour.setMaxValue(23);
        npBreakEndHour.setMinValue(0);   npBreakEndHour.setMaxValue(23);

        // defaults
        npStartHour.setValue(9);
        npEndHour.setValue(17);
        npBreakStartHour.setValue(12);
        npBreakEndHour.setValue(13);
        npSessionDuration.setMinValue(0);
        npSessionDuration.setMaxValue(SESSION_MINUTES_VALUES.length - 1);
        npSessionDuration.setDisplayedValues(SESSION_MINUTES_VALUES);
        npSessionDuration.setValue(3);
    }

    // ------------------------ Sessions Recycler ------------------------

    private void setupSessionsRecycler() {
        sessionAdapter = new SessionAdapter(sessionSlots);
        rvSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSessions.setAdapter(sessionAdapter);
    }

    // ------------------------ Calendar interaction ------------------------

    private void setupCalendarInteraction() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            updateSessionsForDate(cal.getTimeInMillis());
        });

        updateSelectedDateLabel(calendarView.getDate());
    }

    // ------------------------ Sessions generation + slot sync ------------------------

    private void updateSessionsForDate(long millis) {
        sessionSlots.clear();

        if (allAvailabilities.isEmpty()) {
            sessionAdapter.notifyDataSetChanged();
            updateSelectedDateLabel(millis);
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        int selectedDay = mapCalendarToAvailabilityDay(cal.get(Calendar.DAY_OF_WEEK));

        HashSet<String> unique = new HashSet<>();

        for (Availability a : allAvailabilities) {
            if (a.getDayOfWeek() == selectedDay) {
                unique.addAll(buildSlotsForAvailability(a));
            }
        }

        ArrayList<String> sorted = new ArrayList<>(unique);
        Collections.sort(sorted, (a, b) ->
                Integer.compare(toMinutes(a), toMinutes(b)));

        sessionSlots.addAll(sorted);
        updateSelectedDateLabel(millis);
        sessionAdapter.notifyDataSetChanged();

        // persist generated sessions as "slots" for patients
        syncSlotsForDate(millis, sorted);
    }

    private void updateSelectedDateLabel(long millis) {
        Date date = new Date(millis);
        SimpleDateFormat fmt =
                new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());
        tvSelectedDate.setText(fmt.format(date));
        tvSessionsTitle.setText("Sessions:");
    }

    private int mapCalendarToAvailabilityDay(int dow) {
        switch (dow) {
            case Calendar.MONDAY: return 1;
            case Calendar.TUESDAY: return 2;
            case Calendar.WEDNESDAY: return 3;
            case Calendar.THURSDAY: return 4;
            case Calendar.FRIDAY: return 5;
            case Calendar.SATURDAY: return 6;
            case Calendar.SUNDAY: return 7;
        }
        return -1;
    }

    // ------------------------ Save availability ------------------------

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> {
            saveAvailability();
            groupMode.check(R.id.btn_mode_agenda);
        });
    }

    private List<Integer> getSelectedDayIndices() {
        List<Integer> indices = new ArrayList<>();
        List<Integer> checked = groupDays.getCheckedButtonIds();
        for (int i = 0; i < DAY_BUTTON_IDS.length; i++) {
            if (checked.contains(DAY_BUTTON_IDS[i])) indices.add(i);
        }
        return indices;
    }

    private void saveAvailability() {
        FirebaseUser user = FirebaseManager.auth().getCurrentUser();
        if (user == null) return;

        List<Integer> days = getSelectedDayIndices();
        if (days.isEmpty()) {
            Toast.makeText(getContext(), "Select at least one day", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] minuteValues = {"00", "15", "30", "45"};

        int startMinutes = npStartHour.getValue() * 60 + npStartMinute.getValue() * 15;
        int endMinutes = npEndHour.getValue() * 60 + npEndMinute.getValue() * 15;
        int breakStartMinutes = npBreakStartHour.getValue() * 60 + npBreakStartMinute.getValue() * 15;
        int breakEndMinutes = npBreakEndHour.getValue() * 60 + npBreakEndMinute.getValue() * 15;

        if (endMinutes <= startMinutes ||
                breakEndMinutes <= breakStartMinutes ||
                breakStartMinutes < startMinutes ||
                breakEndMinutes > endMinutes) {
            Toast.makeText(getContext(), "Invalid hours", Toast.LENGTH_SHORT).show();
            return;
        }

        int duration = Integer.parseInt(SESSION_MINUTES_VALUES[npSessionDuration.getValue()]);

        for (int d : days) {
            Availability a = new Availability(
                    user.getUid(),
                    DAY_VALUES[d],
                    DAY_LABELS[d],
                    fromMinutes(startMinutes),
                    fromMinutes(endMinutes),
                    fromMinutes(breakStartMinutes),
                    fromMinutes(breakEndMinutes),
                    duration,
                    Timestamp.now()
            );

            FirebaseManager.db().collection("availabilities")
                    .add(a)
                    .addOnFailureListener(Throwable::printStackTrace);
        }

        Toast.makeText(getContext(), "Saved", Toast.LENGTH_SHORT).show();
        loadAvailabilities();
    }

    // ------------------------ RESET AVAILABILITY ------------------------

    private void setupResetButton() {
        btnResetAvailability.setOnClickListener(v -> resetAllAvailability());
    }

    private void resetAllAvailability() {
        FirebaseUser user = FirebaseManager.auth().getCurrentUser();
        if (user == null) return;

        FirebaseManager.db()
                .collection("availabilities")
                .whereEqualTo("doctorId", user.getUid())
                .get()
                .addOnSuccessListener(q -> {
                    for (DocumentSnapshot doc : q.getDocuments()) {
                        doc.getReference().delete();
                    }
                    allAvailabilities.clear();
                    sessionSlots.clear();
                    sessionAdapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "All availability cleared", Toast.LENGTH_SHORT).show();
                    groupMode.check(R.id.btn_mode_availability);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // ------------------------ Role check ------------------------

    private void checkRoleThenSetupForm() {
        FirebaseUser user = FirebaseManager.auth().getCurrentUser();
        if (user == null) {
            formContainer.setVisibility(View.GONE);
            return;
        }

        FirebaseManager.users()
                .document(user.getUid())
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot != null && snapshot.exists()) {
                        String roleStr = snapshot.getString("role");
                        if (!TextUtils.isEmpty(roleStr)) {
                            try {
                                Role role = Role.valueOf(roleStr);
                                formContainer.setVisibility(
                                        role == Role.DOCTOR ? View.VISIBLE : View.GONE
                                );
                            } catch (IllegalArgumentException e) {
                                formContainer.setVisibility(View.GONE);
                            }
                        } else {
                            formContainer.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> formContainer.setVisibility(View.GONE));
    }

    // ------------------------ Load availability ------------------------

    private void loadAvailabilities() {
        FirebaseUser user = FirebaseManager.auth().getCurrentUser();
        if (user == null) return;

        FirebaseManager.db()
                .collection("availabilities")
                .whereEqualTo("doctorId", user.getUid())
                .get()
                .addOnSuccessListener(q -> {
                    allAvailabilities.clear();
                    for (DocumentSnapshot doc : q.getDocuments()) {
                        Availability a = doc.toObject(Availability.class);
                        if (a != null) {
                            a.setId(doc.getId());
                            allAvailabilities.add(a);
                        }
                    }
                    updateSessionsForDate(calendarView.getDate());
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    // ------------------------ Time helpers ------------------------

    private static int toMinutes(String t) {
        String[] p = t.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }

    private static String fromMinutes(int m) {
        return String.format("%02d:%02d", m / 60, m % 60);
    }

    private static List<String> buildSlotsForAvailability(Availability a) {
        List<String> slots = new ArrayList<>();

        int duration = a.getSessionDurationMinutes();
        int start = toMinutes(a.getStartTime());
        int end = toMinutes(a.getEndTime());
        int breakStart = toMinutes(a.getBreakStartTime());
        int breakEnd = toMinutes(a.getBreakEndTime());

        if (start >= end || duration <= 0) return slots;

        for (int t = start; t + duration <= end; t += duration) {
            if (t >= breakStart && t < breakEnd) continue;
            slots.add(fromMinutes(t));
        }

        return slots;
    }

    // ------------------------ Persist slots for patients ------------------------

    private void syncSlotsForDate(long millis, List<String> timesForThatDate) {
        FirebaseUser user = FirebaseManager.auth().getCurrentUser();
        if (user == null || timesForThatDate.isEmpty()) return;

        Date date = new Date(millis);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateKey = df.format(date);
        String doctorId = user.getUid();

        FirebaseManager.db()
                .collection("slots")
                .whereEqualTo("doctorId", doctorId)
                .whereEqualTo("date", dateKey)
                .get()
                .addOnSuccessListener(q -> {
                    HashSet<String> existingTimes = new HashSet<>();
                    for (DocumentSnapshot doc : q.getDocuments()) {
                        String t = doc.getString("time");
                        if (!TextUtils.isEmpty(t)) existingTimes.add(t);
                    }

                    for (String time : timesForThatDate) {
                        if (!existingTimes.contains(time)) {
                            Slot slot = new Slot(
                                    doctorId,
                                    dateKey,
                                    time,
                                    "AVAILABLE",
                                    Timestamp.now()
                            );
                            FirebaseManager.db()
                                    .collection("slots")
                                    .add(slot)
                                    .addOnFailureListener(Throwable::printStackTrace);
                        }
                    }
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    // ------------------------ Recycler ------------------------

    private static class SessionAdapter
            extends RecyclerView.Adapter<SessionAdapter.ViewHolder> {

        private final List<String> data;

        SessionAdapter(List<String> slots) { this.data = slots; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                             int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_session, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int i) {
            h.time.setText(data.get(i));
        }

        @Override
        public int getItemCount() { return data.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView time;
            ViewHolder(@NonNull View v) {
                super(v);
                time = v.findViewById(R.id.tv_session_time);
            }
        }
    }
}
