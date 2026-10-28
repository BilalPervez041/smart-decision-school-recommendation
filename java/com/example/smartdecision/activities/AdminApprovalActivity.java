package com.example.smartdecision.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smartdecision.R;
import com.example.smartdecision.adapters.AdminApprovalAdapter;
import com.example.smartdecision.models.School;
import com.example.smartdecision.utils.FirebaseHelper;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class AdminApprovalActivity extends AppCompatActivity implements AdminApprovalAdapter.OnActionClickListener {

    private RecyclerView rvPendingSchools;
    private AdminApprovalAdapter adapter;
    private List<School> schoolList;
    private TextView tvEmptyPending;
    private DatabaseReference mDatabase;
    private FirebaseHelper firebaseHelper;
    private ValueEventListener schoolsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_approval);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Pending Approvals");
        }

        rvPendingSchools = findViewById(R.id.rvPendingSchools);
        tvEmptyPending = findViewById(R.id.tvEmptyPending);

        mDatabase = FirebaseDatabase.getInstance().getReference("Schools");
        firebaseHelper = new FirebaseHelper();

        rvPendingSchools.setLayoutManager(new LinearLayoutManager(this));
        schoolList = new ArrayList<>();
        adapter = new AdminApprovalAdapter(this, schoolList, this);
        rvPendingSchools.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPendingSchools();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (firebaseHelper != null && schoolsListener != null) {
            firebaseHelper.removeListener(schoolsListener);
            schoolsListener = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadPendingSchools() {
        schoolsListener = firebaseHelper.getAllSchools(new FirebaseHelper.DataListener() {
            @Override
            public void onSuccess(List<School> schools) {
                schoolList.clear();
                for (School school : schools) {
                    if (school.status != null && "pending".equalsIgnoreCase(school.status)) {
                        schoolList.add(school);
                    }
                }

                if (schoolList.isEmpty()) {
                    tvEmptyPending.setVisibility(View.VISIBLE);
                    rvPendingSchools.setVisibility(View.GONE);
                } else {
                    tvEmptyPending.setVisibility(View.GONE);
                    rvPendingSchools.setVisibility(View.VISIBLE);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AdminApprovalActivity.this, "Error loading schools: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onApprove(School school) {
        String path = (school.dbPath != null && !school.dbPath.isEmpty()) ? school.dbPath : "Schools/" + school.id;
        FirebaseDatabase.getInstance().getReference(path).child("status").setValue("approved")
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "School Approved", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onReject(School school) {
        String path = (school.dbPath != null && !school.dbPath.isEmpty()) ? school.dbPath : "Schools/" + school.id;
        FirebaseDatabase.getInstance().getReference(path).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "School Rejected and Removed", Toast.LENGTH_SHORT).show());
    }
}
