package com.doctorsappointment.Adapters;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.RecyclerView;

import com.doctorsappointment.DataRetrievalClass.PatientAppointmentRequest;
import com.doctorsappointment.PatientFragments.PendingAppointmentFragment;
import com.doctorsappointment.PatientMainActivity;
import com.doctorsappointment.R;
import com.doctorsappointment.ReusableFunctionsAndObjects;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PendingAppointmentAdapter extends RecyclerView.Adapter<PendingAppointmentAdapter.ViewHolder> {

    private Context context;
    private List<PatientAppointmentRequest> appointmentRequestList;
    private ProgressDialog progressDialog;

    public PendingAppointmentAdapter(Context context, List<PatientAppointmentRequest> appointmentRequestList) {
        this.context = context;
        this.appointmentRequestList = appointmentRequestList;
        progressDialog = new ProgressDialog(context);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_patient_apt, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PatientAppointmentRequest request = appointmentRequestList.get(position);
        holder.doc_name.setText(request.getName());
        holder.spl.setText("Specialization: " + request.getSpecialization());
        String dateTimeString = "9/5/2024 3:04 PM";
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy h:mm a", Locale.US);
        Date date = null;
        try {
            date = format.parse(dateTimeString);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Adding 1 because Calendar.MONTH is zero-based
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int am_pm = calendar.get(Calendar.AM_PM);

        String amPmIndicator = (am_pm == Calendar.AM) ? "AM" : "PM";

        // Now you can set the date and time on your view
        // Assuming you have TextViews named dateView and timeView for displaying date and time respectively
        holder.date.setText("Date: " + day + "/" + month + "/" + year);
        holder.time.setText("Time: " + String.format(Locale.US, "%d:%02d %s", hour, minute, amPmIndicator));
        holder.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(context).setCancelable(false).setMessage("Are you sure you want to cancel the appointment of Dr. " + request.getName() + " for " + request.getDateAndTime() + "?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                progressDialog.setMessage("Cancelling...");
                                progressDialog.show();
                                FirebaseDatabase.getInstance().getReference().child("PendingDocAppointments").child(request.getDocID()).child(request.getDoctorAppointKey()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            FirebaseDatabase.getInstance().getReference().child("PendingPatientAppointments").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).child(request.getPatientAppointKey()).removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        progressDialog.dismiss();
                                                        Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show();
                                                        ((PatientMainActivity) context).getSupportFragmentManager().beginTransaction().replace(R.id.fragment_Container, new PendingAppointmentFragment(), "Pending Appointments").addToBackStack(null).commit();
                                                    } else {
                                                        progressDialog.dismiss();
                                                        ReusableFunctionsAndObjects.showMessageAlert(context, "Network Error", "Make sure you are connected to internet.", "OK", (byte) 0);
                                                    }
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    progressDialog.dismiss();
                                                    ReusableFunctionsAndObjects.showMessageAlert(context, "Network Error", "Make sure you are connected to internet.", "OK", (byte) 0);
                                                }
                                            });
                                        } else {
                                            progressDialog.dismiss();
                                            ReusableFunctionsAndObjects.showMessageAlert(context, "Network Error", "Make sure you are connected to internet.", "OK", (byte) 0);
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressDialog.dismiss();
                                        ReusableFunctionsAndObjects.showMessageAlert(context, "Network Error", "Make sure you are connected to internet.", "OK", (byte) 0);
                                    }
                                });
                            }
                        }).setNegativeButton("No", null).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return appointmentRequestList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView doc_name, spl, date, time;
        AppCompatButton cancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            doc_name = itemView.findViewById(R.id.doc_name);
            cancel = itemView.findViewById(R.id.cancel);
            spl = itemView.findViewById(R.id.spl);
            date = itemView.findViewById(R.id.date);
            time = itemView.findViewById(R.id.time);
        }
    }
}
