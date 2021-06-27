package li.kaiott.asvzbuddy;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Lesson implements Comparable<Lesson>{
    private int id, status;
    private String title;
    private OffsetDateTime starts, ends;
    public boolean selected;
    public static int selectedCount = 0;
    public static final int WAITING_FOR_SIGN_UP = 0, MONITORING_FOR_SLOTS = 1, SIGN_UP_SUCCESSFUL = 2,
                     SIGN_UP_MISSED = 3, STATUS_PENDING = 4, ERROR_OCCURRED = 5, ENROLLMENT_REMOVED = 6;;
    public static final String statusDescriptors[] = {"Waiting", "Watching", "Enrolled",
                                               "Missed", "...", "Error", "Removed" };

    public Lesson(int id, int status, String title, OffsetDateTime starts, OffsetDateTime ends) {
        this.id = id;
        this.status = status;
        this.title = title;
        this.starts = starts;
        this.ends = ends;
        this.selected = false;
    }

    public int getId() {
        return id;
    }

    public int getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public OffsetDateTime getStarts() {
        return starts;
    }

    public OffsetDateTime getEnds() {
        return ends;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int compareTo(Lesson o) {
        return this.starts.compareTo(o.starts);
    }

    public static ArrayList<Lesson> filterInterval(ArrayList<Lesson> lessons, OffsetDateTime from, OffsetDateTime to) {
        ArrayList<Lesson> filtered = new ArrayList<>();

        for (Lesson lesson : lessons) {
            if ((from == null || !lesson.getEnds().isBefore(from)) && (to == null || lesson.getStarts().isBefore(to))) {
                filtered.add(lesson);
            }
        }

        return filtered;
    }
}
