    package com.mobileapplication.streetassist.ui.resident.Home;

    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.auth.FirebaseUser;
    import com.google.firebase.firestore.DocumentSnapshot;
    import com.google.firebase.firestore.FirebaseFirestore;
    import com.google.firebase.firestore.ListenerRegistration;
    import com.google.firebase.firestore.MetadataChanges;

    import java.util.HashMap;
    import java.util.Map;

    /**
     * Watches the current user's reports in real-time.
     * When a report's status changes, it automatically writes
     * a notification document to the "notifications" collection.
     *
     * Usage:
     *   watcher = new ReportStatusWatcher();
     *   watcher.start();
     *   ...
     *   watcher.stop(); // call in onDestroyView / onStop
     */
    public class ReportStatusWatcher {

        private final FirebaseFirestore db   = FirebaseFirestore.getInstance();
        private final FirebaseAuth      auth = FirebaseAuth.getInstance();

        // Keeps the last known status per reportId so we can detect changes
        private final Map<String, String> lastKnownStatus = new HashMap<>();

        private ListenerRegistration listenerReg;

        // ── Start watching ────────────────────────────────────────────────────────

        public void start() {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) return;

            listenerReg = db.collection("reports")
                    .whereEqualTo("userId", user.getUid())
                    // MetadataChanges.EXCLUDE means we only react to server writes,
                    // not local cache updates — avoids false triggers on app launch
                    .addSnapshotListener(MetadataChanges.EXCLUDE, (snapshots, error) -> {
                        if (error != null || snapshots == null) return;

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String docId    = doc.getId();
                            String newStatus = doc.getString("status");
                            if (newStatus == null) continue;

                            String prevStatus = lastKnownStatus.get(docId);

                            if (prevStatus == null) {
                                // First time we've seen this report — just record it,
                                // don't fire a notification (avoids spam on first load)
                                lastKnownStatus.put(docId, newStatus);

                            } else if (!prevStatus.equals(newStatus)) {
                                // Status genuinely changed — create notification
                                lastKnownStatus.put(docId, newStatus);
                                createStatusNotification(doc, newStatus);
                            }
                        }
                    });
        }

        // ── Stop watching ─────────────────────────────────────────────────────────

        public void stop() {
            if (listenerReg != null) {
                listenerReg.remove();
                listenerReg = null;
            }
            lastKnownStatus.clear();
        }

        // ── Write notification document ───────────────────────────────────────────

        private void createStatusNotification(DocumentSnapshot doc, String newStatus) {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) return;

            String reportId = doc.getString("reportId");
            if (reportId == null) reportId = doc.getId();

            String title   = "Report Status Updated";
            String message = buildMessage(reportId, newStatus);

            Map<String, Object> notif = new HashMap<>();
            notif.put("userId",    user.getUid());
            notif.put("reportId",  reportId);
            notif.put("title",     title);
            notif.put("message",   message);
            notif.put("status",    newStatus);                                    // ← stored separately for chip
            notif.put("isRead",    false);
            notif.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

            db.collection("notifications")
                    .add(notif)
                    .addOnFailureListener(e ->
                            android.util.Log.e("ReportStatusWatcher",
                                    "Failed to write notification", e));
        }

        // ── Message copy per status ───────────────────────────────────────────────
        // Call this immediately after successfully writing a report to Firestore
        public static void notifyReportSubmitted(String userId, String reportId) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            String message = reportId + " has been submitted and is awaiting review.";

            Map<String, Object> notif = new HashMap<>();
            notif.put("userId",    userId);
            notif.put("reportId",  reportId);
            notif.put("title",     "Report Submitted");
            notif.put("message",   message);
            notif.put("status",    "Pending");
            notif.put("isRead",    false);
            notif.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

            db.collection("notifications")
                    .add(notif)
                    .addOnFailureListener(e ->
                            android.util.Log.e("ReportStatusWatcher",
                                    "Failed to write submitted notification", e));
        }
        private String buildMessage(String reportId, String status) {
            switch (status) {
                case "Verified":
                    return reportId + " has been verified by our team.";
                case "In Progress":
                    return reportId + " is now being acted upon by responders.";
                case "Resolved":
                    return reportId + " has been resolved. Thank you for your report!";
                case "Pending":
                    return reportId + " is pending review.";
                default:
                    return reportId + " status changed to " + status + ".";
            }
        }
    }