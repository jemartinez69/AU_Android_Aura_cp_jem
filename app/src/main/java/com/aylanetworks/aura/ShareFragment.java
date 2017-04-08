package com.aylanetworks.aura;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import com.android.volley.Response;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaShare;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.aylanetworks.aura.MainActivity.getDeviceManager;


/**
 * Fragment to display share UI
 */
public class ShareFragment extends Fragment {

    private static final String ARG_DSN = "dsn";
    private static final String LOG_TAG = "AURA_SHARES";

    private String _dsn;
    private AylaDevice _device;
    private EditText _editTextEmail;
    private EditText _editTextRole;
    private Button _btnStartDate;
    private Button _btnEndDate;
    private CheckBox _checkbox;
    private String _startDate;
    private String _endDate;

    public ShareFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param device device to be shared.
     * @return A new instance of fragment ShareFragment.
     */
    public static ShareFragment newInstance(AylaDevice device) {
        ShareFragment fragment = new ShareFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DSN, device.getDsn());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            _dsn = (String) getArguments().get(ARG_DSN);
            AylaDeviceManager deviceManager = getDeviceManager();
            if( deviceManager != null){
                _device = deviceManager.deviceWithDSN(_dsn);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_share, container, false);
        TextView txtviewDsn = (TextView) view.findViewById(R.id.share_device_dsn);
        _editTextEmail = (EditText) view.findViewById(R.id.share_user_email);
        _checkbox = (CheckBox) view.findViewById(R.id.checkbox_access);
        _editTextRole = (EditText) view.findViewById(R.id.share_role);
        _btnStartDate = (Button) view.findViewById(R.id.share_start_date);
        _btnEndDate = (Button) view.findViewById(R.id.share_end_date);
        txtviewDsn.setText(_dsn);

        _btnStartDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               getDate(v.getId());
            }
        });

        _btnEndDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               getDate(v.getId());
            }
        });

        Button btnShare = (Button) view.findViewById(R.id.btn_share_device);
        btnShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = _editTextEmail.getText().toString();
                if(email == null || email.isEmpty()){
                    return;
                }
                String role = _editTextRole.getText().toString();
                if(role.isEmpty()){
                    role = null;
                }
                String operation = "write";
                if(_checkbox.isChecked()){
                    operation = "read";
                }
                shareDevice(email, role, operation, _startDate, _endDate );
                _startDate = null;
                _endDate = null;
            }
        });
        return view;
    }

    private void shareDevice(final String email, final String role, final String operation,
                             String startDate, String endDate) {

        View view = getView();
        InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context
                .INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromInputMethod(view.getWindowToken(), 0);
        if(_device == null){
            AylaLog.e(LOG_TAG, "shareDevice: device is null");
            return;
        }
        AylaShare share = _device.shareWithEmail(email, operation, role, startDate, endDate );
        MainActivity.getSession().createShare(share, null,
                new Response.Listener<AylaShare>(){
                    @Override
                    public void onResponse(AylaShare response) {
                        Activity activity = getActivity();
                        if(activity != null){
                            View view = activity.findViewById(android.R.id.content);
                            Snackbar.make(view, R.string.share_success, Snackbar
                                    .LENGTH_SHORT).show();
                        }
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Activity activity = getActivity();
                        if(activity != null){
                            View view = activity.findViewById(android.R.id.content);
                            Snackbar.make(view, error.getLocalizedMessage(), Snackbar
                                    .LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void getDate(final int viewId){
        final SimpleDateFormat dateFormat = new SimpleDateFormat
                ("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        Date startDate = calendar.getTime();
        if(_startDate != null && viewId == R.id.share_end_date){
            try {
                startDate = dateFormat.parse(_startDate) ;
            } catch (ParseException e) {
                AylaLog.d(LOG_TAG, "DateParseException while parsing start date");
            }
        }
        calendar.setTime(startDate);
        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar cal = Calendar.getInstance();
                cal.set(year, monthOfYear, dayOfMonth);
                switch (viewId){
                    case R.id.share_start_date:
                        _startDate = dateFormat.format(cal.getTime());
                        _btnStartDate.setText(_startDate);
                        break;
                    case R.id.share_end_date:
                        _endDate = dateFormat.format(cal.getTime());
                        _btnEndDate.setText(_endDate);
                        break;
                }

            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }
}
