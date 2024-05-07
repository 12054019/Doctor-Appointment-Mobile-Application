package com.doctorsappointment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.database.FirebaseDatabase;
import com.hbb20.CountryCodePicker;

import java.util.HashMap;
import java.util.UUID;
import java.util.regex.Pattern;

public class DoctorRegister extends AppCompatActivity {

    private TextInputLayout FirstNameTB, LastNameTB, MobileNoTB, EmailTB, PassTB, ConfirmPassTB;
    private CountryCodePicker ccp;
    private AppCompatButton register;
    private ProgressDialog progressDialog;
    private TextView goToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_register);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        FirstNameTB = findViewById(R.id.FirstNameTB);
        LastNameTB = findViewById(R.id.LastNameTB);
        MobileNoTB = findViewById(R.id.PhoneTB);
        EmailTB = findViewById(R.id.EmailTB);
        PassTB = findViewById(R.id.PassTB);
        ConfirmPassTB = findViewById(R.id.ConfirmPassTB);
        ccp = findViewById(R.id.ccp);
        goToLogin = findViewById(R.id.gotosign);
        addKeyListenerToTextInputLayout(FirstNameTB);
        addKeyListenerToTextInputLayout(LastNameTB);
        addKeyListenerToTextInputLayout(MobileNoTB);
        addKeyListenerToTextInputLayout(EmailTB);
        addKeyListenerToTextInputLayout(PassTB);
        addKeyListenerToTextInputLayout(ConfirmPassTB);
        register = findViewById(R.id.register);
        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        goToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DoctorRegister.this, LoginActivity.class).putExtra("ForPatient", true));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isValid()) {
                    String email = EmailTB.getEditText().getText().toString().trim();
                    String pass = PassTB.getEditText().getText().toString().trim();
                    String firstName = FirstNameTB.getEditText().getText().toString().trim();
                    String lastName = LastNameTB.getEditText().getText().toString().trim();
                    String mobileNo = MobileNoTB.getEditText().getText().toString().trim();
                    progressDialog.setMessage("Registering, please wait...");
                    progressDialog.show();
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass).addOnCompleteListener(DoctorRegister.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                FirebaseAuth.getInstance().getCurrentUser().sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            String docId = generateDocId(); // Generate a unique docId
                                            HashMap<String, String> hashMap = new HashMap<>();
                                            hashMap.put("DocId", docId);
                                            hashMap.put("Email", email);
                                            hashMap.put("MobileNo", ccp.getSelectedCountryCodeWithPlus() + mobileNo);
                                            hashMap.put("FirstName", firstName);
                                            hashMap.put("LastName", lastName);
                                            hashMap.put("UserType", "DOCTOR");

                                            FirebaseDatabase.getInstance().getReference().child("UserDetails").child(userId).setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    progressDialog.dismiss();
                                                    ReusableFunctionsAndObjects.showMessageAlert(DoctorRegister.this, "Partially Completed", "To complete the registration click the verification link sent to your email (" + email + ").", "OK", (byte) 1);
                                                    try {
                                                        FirebaseAuth.getInstance().signOut();
                                                    } catch (Exception e) {

                                                    }
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    progressDialog.dismiss();
                                                    try {
                                                        FirebaseAuth.getInstance().signOut();
                                                    } catch (Exception SS) {
                                                    }
                                                    ReusableFunctionsAndObjects.showMessageAlert(DoctorRegister.this, "Network Error", "Couldn't register, Please make sure you are connected to the internet", "OK", (byte) 0);
                                                }
                                            });
                                        } else {
                                            progressDialog.dismiss();
                                            ReusableFunctionsAndObjects.showMessageAlert(DoctorRegister.this, "Network Error", "Couldn't register, Please make sure you are connected to the internet", "OK", (byte) 0);
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        progressDialog.dismiss();
                                        ReusableFunctionsAndObjects.showMessageAlert(DoctorRegister.this, "Network Error", "Couldn't register, Please make sure you are connected to the internet", "OK", (byte) 0);
                                    }
                                });
                            } else {
                                try {
                                    throw task.getException();
                                } catch (FirebaseAuthWeakPasswordException weakPassword) {
                                    progressDialog.dismiss();
                                    ReusableFunctionsAndObjects.showMessageAlert(DoctorRegister.this, "Weak Password", "Your password is weak", "OK", (byte) 0);
                                } catch (FirebaseAuthUserCollisionException existEmail) {
                                    progressDialog.dismiss();
                                    ReusableFunctionsAndObjects.showMessageAlert(DoctorRegister.this, "Error in Registration", "This email is already registered", "OK", (byte) 0);
                                } catch (Exception e) {
                                    progressDialog.dismiss();
                                    ReusableFunctionsAndObjects.showMessageAlert(DoctorRegister.this, "Network Error", "Couldn't register, Please make sure you are connected to the internet", "OK", (byte) 0);
                                }
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });
                }
            }
        });
    }

    protected void addKeyListenerToTextInputLayout(final TextInputLayout textInputLayout) {
        textInputLayout.getEditText().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                textInputLayout.setError(null);
                textInputLayout.setErrorEnabled(false);
                return false;
            }
        });
    }

    protected boolean isValid() {
        boolean ispass1fill = false, ispass2fill = false, isemailvalid = false, isphonevalid = false, ispasswordsvalid = false;

        if ((((ConfirmPassTB.getEditText().getText()).toString()).trim()).equals("")) {
            ConfirmPassTB.setErrorEnabled(true);
            ConfirmPassTB.setError("Please enter password confirmation");
            ConfirmPassTB.requestFocus();
            ispass2fill = false;
        } else {
            ispass2fill = true;
        }

        if ((((PassTB.getEditText().getText()).toString()).trim()).equals("")) {
            PassTB.setErrorEnabled(true);
            PassTB.setError("Please enter password");
            PassTB.requestFocus();
            ispass1fill = false;
        } else {
            ispass1fill = true;
        }

        if (ispass1fill && ispass2fill) {
            if ((((PassTB.getEditText().getText()).toString()).trim()).equals(((ConfirmPassTB.getEditText().getText()).toString()).trim())) {
                ispasswordsvalid = true;
            } else {
                ConfirmPassTB.setErrorEnabled(true);
                ConfirmPassTB.setError("Password doesn't match");
                ConfirmPassTB.requestFocus();
                ispasswordsvalid = false;
            }
        }

        if ((((MobileNoTB.getEditText().getText()).toString()).trim()).equals("")) {
            MobileNoTB.setErrorEnabled(true);
            MobileNoTB.setError("Please enter mobile number");
            MobileNoTB.requestFocus();
            isphonevalid = false;
        } else {
            if (((MobileNoTB.getEditText().getText()).toString()).trim().length() > 10) {
                MobileNoTB.setErrorEnabled(true);
                MobileNoTB.setError("Invalid mobile number");
                MobileNoTB.requestFocus();
                isphonevalid = false;
            } else {
                isphonevalid = true;
            }
        }

        if ((((EmailTB.getEditText().getText()).toString()).trim()).equals("")) {
            EmailTB.setErrorEnabled(true);
            EmailTB.setError("Please enter email address");
            EmailTB.requestFocus();
            isemailvalid = false;
        } else {
            if (Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$").matcher(((EmailTB.getEditText().getText()).toString()).trim()).matches()) {
                isemailvalid = true;
            } else {
                EmailTB.setErrorEnabled(true);
                EmailTB.setError("Please enter a valid email address");
                EmailTB.requestFocus();
                isemailvalid = false;
            }
        }
        return (isemailvalid && isphonevalid && ispasswordsvalid) ? true : false;
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(DoctorRegister.this, PatientLoginRegisterChoice.class));
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        finish();
    }
    public static String generateDocId() {
        // Generate a random UUID (Universally Unique Identifier)
        String uuid = UUID.randomUUID().toString();

        // Remove hyphens from UUID to get a single string
        String docId = uuid.replaceAll("-", "");

        return docId;
    }

}