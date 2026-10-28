package com.example.smartdecision.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartdecision.R;
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
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

public class ApprovedStudentsActivity extends AppCompatActivity {

    private RecyclerView rvApprovedStudents;
    private ApprovedStudentsAdapter adapter;
    private List<Map<String, Object>> studentList;
    private TextView tvEmptyState;
    private DatabaseReference mDatabase;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_approved_students);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        currentUserId = FirebaseAuth.getInstance().getUid();
        mDatabase = FirebaseDatabase.getInstance().getReference("Admissions");

        rvApprovedStudents = findViewById(R.id.rvApprovedStudents);
        tvEmptyState = findViewById(R.id.tvEmptyState);

        rvApprovedStudents.setLayoutManager(new LinearLayoutManager(this));
        studentList = new ArrayList<>();
        adapter = new ApprovedStudentsAdapter(studentList);
        rvApprovedStudents.setAdapter(adapter);

        loadApprovedStudents();
    }

    private void loadApprovedStudents() {
        if (currentUserId == null) return;

        mDatabase.orderByChild("ownerId").equalTo(currentUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        studentList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            String status = ds.child("status").getValue(String.class);
                            if ("approved".equals(status)) {
                                Map<String, Object> student = (Map<String, Object>) ds.getValue();
                                if (student != null) {
                                    student.put("id", ds.getKey()); // Add ID if needed
                                    studentList.add(0, student); // Newest first
                                }
                            }
                        }

                        if (studentList.isEmpty()) {
                            tvEmptyState.setVisibility(View.VISIBLE);
                            rvApprovedStudents.setVisibility(View.GONE);
                        } else {
                            tvEmptyState.setVisibility(View.GONE);
                            rvApprovedStudents.setVisibility(View.VISIBLE);
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private class ApprovedStudentsAdapter extends RecyclerView.Adapter<ApprovedStudentsAdapter.ViewHolder> {
        private List<Map<String, Object>> list;

        public ApprovedStudentsAdapter(List<Map<String, Object>> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_approved_student, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> student = list.get(position);
            String admissionId = (String) student.get("id");
            holder.tvName.setText((String) student.get("studentName"));
            holder.tvSchool.setText((String) student.get("schoolName"));
            holder.tvLevel.setText((String) student.get("level"));
            holder.tvStartDate.setText((String) student.get("startDate"));
            holder.tvContact.setText((String) student.get("contact"));
            holder.tvFatherName.setText((String) student.get("fatherName"));

            holder.btnDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(ApprovedStudentsActivity.this)
                        .setTitle("Delete Record")
                        .setMessage("Are you sure you want to delete this student record?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            if (admissionId != null) {
                                mDatabase.child(admissionId).removeValue()
                                        .addOnSuccessListener(aVoid -> Toast.makeText(ApprovedStudentsActivity.this, "Record deleted", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(ApprovedStudentsActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvSchool, tvLevel, tvStartDate, tvContact, tvFatherName;
            ImageButton btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvStudentName);
                tvSchool = itemView.findViewById(R.id.tvSchoolName);
                tvLevel = itemView.findViewById(R.id.tvLevel);
                tvStartDate = itemView.findViewById(R.id.tvStartDate);
                tvContact = itemView.findViewById(R.id.tvContact);
                tvFatherName = itemView.findViewById(R.id.tvFatherName);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}
