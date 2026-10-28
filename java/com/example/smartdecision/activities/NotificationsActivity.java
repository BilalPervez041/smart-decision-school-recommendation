package com.example.smartdecision.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartdecision.R;
import com.example.smartdecision.adapters.NotificationAdapter;
import com.example.smartdecision.models.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private DatabaseReference notifRef;
    private String currentUserId;
    private TextView btnClearAll;

    private ValueEventListener notifListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) {
            finish();
            return;
        }

        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList, this);
        rvNotifications.setAdapter(adapter);

        btnClearAll = findViewById(R.id.btnClearAll);
        btnClearAll.setOnClickListener(v -> showClearAllConfirmation());

        notifRef = FirebaseDatabase.getInstance().getReference("Users").child(currentUserId).child("notifications");
    }

    private void showClearAllConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Notifications")
                .setMessage("Are you sure you want to delete all notifications?")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    notifRef.removeValue();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (notifListener != null) {
            notifRef.removeEventListener(notifListener);
        }
    }

    private void loadNotifications() {
        notifListener = notifRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Notification notif = ds.getValue(Notification.class);
                    if (notif != null) {
                        notif.id = ds.getKey();
                        notificationList.add(0, notif); // Newest first
                    }
                }
                if (notificationList.isEmpty()) {
                    btnClearAll.setVisibility(View.GONE);
                } else {
                    btnClearAll.setVisibility(View.VISIBLE);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onApprove(Notification notification) {
        showApprovalDialog(notification);
    }

    private void showApprovalDialog(Notification notification) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Approve Admission");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText etDate = new EditText(this);
        etDate.setHint("Classes Start Date (e.g. 1st Sep 2024)");
        layout.addView(etDate);

        builder.setView(layout);
        builder.setPositiveButton("Approve", (dialog, which) -> {
            String startDate = etDate.getText().toString().trim();
            if (startDate.isEmpty()) {
                Toast.makeText(this, "Please enter start date", Toast.LENGTH_SHORT).show();
                return;
            }
            processApproval(notification, startDate);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void processApproval(Notification notification, String startDate) {
        DatabaseReference appRef = FirebaseDatabase.getInstance().getReference("Admissions").child(notification.applicationId);
        appRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String studentUserId = snapshot.child("userId").getValue(String.class);
                    String schoolId = snapshot.child("schoolId").getValue(String.class);
                    String dbPath = snapshot.child("dbPath").getValue(String.class); // Get dbPath if stored in Admission

                    // Update Admission Status
                    appRef.child("status").setValue("approved");
                    appRef.child("startDate").setValue(startDate);

                    // Notify Student
                    if (studentUserId != null) {
                        DatabaseReference studentNotifRef = FirebaseDatabase.getInstance().getReference("Users").child(studentUserId).child("notifications").push();
                        Map<String, Object> studentNotif = new HashMap<>();
                        studentNotif.put("id", studentNotifRef.getKey());
                        studentNotif.put("title", "Admission Approved!");
                        studentNotif.put("message", "Congratulations! Your admission has been approved. Classes start on: " + startDate);
                        studentNotif.put("status", "approved");
                        studentNotif.put("type", "user");
                        studentNotif.put("timestamp", System.currentTimeMillis());
                        studentNotifRef.setValue(studentNotif);
                    }

                    // Decrement Seat Count - Passing dbPath
                    decrementSeats(schoolId, dbPath, snapshot.child("level").getValue(String.class));

                    // Delete the owner notification
                    notifRef.child(notification.id).removeValue();
                    
                    Toast.makeText(NotificationsActivity.this, "Admission approved and student notified", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void decrementSeats(String schoolId, String dbPath, String level) {
        String path = (dbPath != null && !dbPath.isEmpty()) ? dbPath : "Schools/" + schoolId;
        DatabaseReference schoolRef = FirebaseDatabase.getInstance().getReference(path);
        schoolRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData currentData) {
                if (currentData.getValue() == null) return com.google.firebase.database.Transaction.success(currentData);

                Integer boys = currentData.child("boysSeats").getValue(Integer.class);
                Integer girls = currentData.child("girlsSeats").getValue(Integer.class);
                
                if (boys != null && boys > 0) {
                    currentData.child("boysSeats").setValue(boys - 1);
                } else if (girls != null && girls > 0) {
                    currentData.child("girlsSeats").setValue(girls - 1);
                }
                
                return com.google.firebase.database.Transaction.success(currentData);
            }

            @Override
            public void onComplete(@com.google.firebase.database.annotations.Nullable DatabaseError error, boolean committed, @com.google.firebase.database.annotations.Nullable DataSnapshot currentData) {
                if (error != null) {
                    android.util.Log.e("NotificationsActivity", "Transaction failed: " + error.getMessage());
                }
            }
        });
    }

    @Override
    public void onReject(Notification notification) {
        new AlertDialog.Builder(this)
                .setTitle("Reject Admission")
                .setMessage("Are you sure you want to reject this application?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    processRejection(notification);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void processRejection(Notification notification) {
        DatabaseReference appRef = FirebaseDatabase.getInstance().getReference("Admissions").child(notification.applicationId);
        appRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String studentUserId = snapshot.child("userId").getValue(String.class);
                    
                    appRef.child("status").setValue("rejected");

                    if (studentUserId != null) {
                        DatabaseReference studentNotifRef = FirebaseDatabase.getInstance().getReference("Users").child(studentUserId).child("notifications").push();
                        Map<String, Object> studentNotif = new HashMap<>();
                        studentNotif.put("id", studentNotifRef.getKey());
                        studentNotif.put("title", "Admission Rejected");
                        studentNotif.put("message", "We regret to inform you that your admission application has been rejected.");
                        studentNotif.put("status", "rejected");
                        studentNotif.put("type", "user");
                        studentNotif.put("timestamp", System.currentTimeMillis());
                        studentNotifRef.setValue(studentNotif);
                    }

                    notifRef.child(notification.id).removeValue();
                    Toast.makeText(NotificationsActivity.this, "Admission rejected", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onDelete(Notification notification) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notification")
                .setMessage("Delete this notification?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    notifRef.child(notification.id).removeValue();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
