package com.aylanetworks.aura;
/*
 * AylaSDK
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.aura.util.ContactHelper;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaContact;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaDevice;
import com.aylanetworks.aylasdk.AylaProperty;
import com.aylanetworks.aylasdk.AylaPropertyTrigger;
import com.aylanetworks.aylasdk.AylaPropertyTriggerApp;
import com.aylanetworks.aylasdk.AylaServiceApp;
import com.aylanetworks.aylasdk.AylaSystemSettings;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.util.ArrayList;
import java.util.List;

public class PropertyNotificationFragment extends Fragment implements
        ContactListAdapter.ContactCardListener, View.OnClickListener,
        AdapterView.OnItemSelectedListener {
    private static final String ARG_DSN = "dsn";
    private static final String ARG_TRIGGER = "trigger";

    private static final String TRIGGER_COMPARE_ABSOLUTE = "compare_absolute";
    private static final String TRIGGER_ALWAYS = "always";

    private static final String LOG_TAG = "PropNotifFrag";
    private final List<AylaContact> _pushContacts;
    private final List<AylaContact> _emailContacts;
    private final List<AylaContact> _smsContacts;
    private Spinner _propertySpinner;
    private EditText _nameEditText;
    private EditText _numberEditText;
    private RadioGroup _numberRadioGroup;
    private RadioGroup _booleanRadioGroup;
    private RadioGroup _motionRadioGroup;
    private String _originalTriggerName;
    private AylaPropertyTrigger _originalTrigger;
   // private List<AylaContact> _contacts;
    // Layouts for each of the property base types. We will enable the appropriate layout
    // when the property is selected.
    private LinearLayout _booleanLayout;
    private LinearLayout _integerLayout;
    private LinearLayout _motionSensorLayout;

   /* public static PropertyNotificationFragment newInstance(AylaDevice device) {
        return newInstance(device, null);
    }*/
    public static PropertyNotificationFragment newInstance(AylaDevice device, AylaPropertyTrigger
            triggerToEdit) {
        PropertyNotificationFragment frag = new PropertyNotificationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DSN, device.getDsn());
        if ( triggerToEdit != null ) {
            args.putString(ARG_TRIGGER, triggerToEdit.getDeviceNickname());
        }
        frag.setArguments(args);
        return frag;
    }

    // Default constructor
    public PropertyNotificationFragment() {
        _pushContacts = new ArrayList<>();
        _emailContacts = new ArrayList<>();
        _smsContacts = new ArrayList<>();
    }

    private AylaDevice _device;
    private RecyclerView _recyclerView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
        _device = null;
        if (getArguments() != null ) {
            String dsn = getArguments().getString(ARG_DSN);
            _device = MainActivity.getSession().getDeviceManager().deviceWithDSN(dsn);
            Log.d(LOG_TAG, "My device: " + _device);

            _originalTriggerName = getArguments().getString(ARG_TRIGGER);
            if (_originalTriggerName != null) {
                // Try to find the trigger
                for (AylaProperty prop : _device.getProperties()) {
                    prop.fetchTriggers(
                            new Response.Listener<AylaPropertyTrigger[]>() {
                                @Override
                                public void onResponse(AylaPropertyTrigger[] response) {
                                    if (response != null && response.length > 0) {
                                        for (AylaPropertyTrigger trigger : response) {
                                            if (_originalTriggerName.equals
                                                    (trigger.getDeviceNickname())) {
                                                _originalTrigger = trigger;
                                                updateUI();
                                                break;
                                            }
                                        }
                                    }
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(AylaError error) {
                                    Toast.makeText(MainActivity.sharedInstance(), error.toString(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                }

                if (_originalTrigger == null) {
                    Log.e(LOG_TAG, "Unable to find original trigger!");
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_property_notification, container, false);
        RecyclerView.LayoutManager _layoutManager;
        _recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        _layoutManager = new LinearLayoutManager(getActivity());
        _recyclerView.setLayoutManager(_layoutManager);
        _nameEditText = (EditText)view.findViewById(R.id.notification_name);
        _propertySpinner = (Spinner)view.findViewById(R.id.property_spinner);
        _booleanLayout = (LinearLayout)view.findViewById(R.id.layout_boolean);
        _integerLayout = (LinearLayout)view.findViewById(R.id.layout_integer);
        _motionSensorLayout = (LinearLayout)view.findViewById(R.id.layout_motion);
        _numberEditText = (EditText)view.findViewById(R.id.number_edit_text);

        // Set up a listener to show / hide the numerical input field based on the selection
        _numberRadioGroup = (RadioGroup)view.findViewById(R.id.radio_group_integer);
        _numberRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if ( checkedId == R.id.radio_integer_changes ) {
                    _numberEditText.setVisibility(View.GONE);
                } else {
                    _numberEditText.setVisibility(View.VISIBLE);
                }
            }
        });

        _booleanRadioGroup = (RadioGroup)view.findViewById(R.id.radio_group_boolean);
        _motionRadioGroup = (RadioGroup)view.findViewById(R.id.radio_group_motion);

        _propertySpinner.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout
                .simple_list_item_1, getNotifiablePropertyNames()));
        _propertySpinner.setOnItemSelectedListener(this);
        view.findViewById(R.id.save_notifications).setOnClickListener(this);

        final TextView emptyView = (TextView)view.findViewById(R.id.empty);
        emptyView.setVisibility(View.GONE);
        _recyclerView.setAdapter(new ContactListAdapter(PropertyNotificationFragment.this));
        _recyclerView.setVisibility(View.VISIBLE);
        if ( _propertySpinner.getCount() > 0 ) {
            _propertySpinner.setSelection(0);
        }

        if ( _originalTrigger != null ) {
            updateUI();
        }
        return view;
    }

    private void updateUI() {
        if ( _originalTrigger == null ) {
            Log.e(LOG_TAG, "trigger: No _originalTrigger");
            return;
        }
        //Log.d(LOG_TAG, "trigger: _originalTrigger=[" + _originalTrigger + "]");

        if ( _originalTrigger.getPropertyNickname() == null ) {
            Log.e(LOG_TAG, "trigger: No property nickname");
        } else {
            // Select the property in our list
            String[] props = getNotifiablePropertyNames();
            for (int i = 0; i < props.length; i++) {
                if (props[i].equals(_originalTrigger.getPropertyNickname())) {
                    _propertySpinner.setSelection(i);
                    break;
                }
            }
        }

        _nameEditText.setText(_originalTrigger.getDeviceNickname());

        switch(_originalTrigger.getBaseType()) {
            case "boolean":
                if ( _originalTrigger.getTriggerType().equals(TRIGGER_COMPARE_ABSOLUTE)) {
                    // This is either turn on or turn off
                    _booleanRadioGroup.check(_originalTrigger.getValue().equals("1") ?
                            R.id.radio_turn_on : R.id.radio_turn_off);
                } else {
                    _booleanRadioGroup.check(R.id.radio_on_or_off);
                }
                break;

            case "integer":
            case "decimal":
                _numberEditText.setText(_originalTrigger.getValue());
                if ( _originalTrigger.getTriggerType().equals(TRIGGER_COMPARE_ABSOLUTE) &&
                        _originalTrigger.getValue() != null ) {
                    _numberRadioGroup.check(_originalTrigger.getValue().equals("<") ? R.id
                            .radio_integer_less_than : R.id.radio_integer_greater_than);
                } else {
                    _numberRadioGroup.check(R.id.radio_integer_changes);
                }
                break;
        }
        _originalTrigger.fetchApps(
                new Response.Listener<AylaPropertyTriggerApp[]>() {
                    @Override
                    public void onResponse(AylaPropertyTriggerApp[] response) {
                        for (AylaPropertyTriggerApp propertyTriggerApp : response) {
                            String contactId = propertyTriggerApp.getContactId();
                            if (contactId == null) {
                                continue;
                            }
                            AylaContact contact= ContactHelper.getContactByID(Integer.parseInt
                                    (contactId));
                            if(contact == null) {
                                return;
                            }
                            switch (propertyTriggerApp.getNotificationType()) {
                                case SMS:
                                    _smsContacts.add(contact);
                                    break;
                                case EMail:
                                    _emailContacts.add(contact);
                                    break;
                                case GooglePush:
                                    _pushContacts.add(contact);
                                    break;
                            }
                        }
                        _recyclerView.getAdapter().notifyDataSetChanged();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(MainActivity.sharedInstance(), error.toString(), Toast.LENGTH_LONG)
                                .show();
                    }
                });
    }

    @Override
    public void pushTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Push tapped: " + contact);
        if ( _pushContacts.contains(contact) ) {
            _pushContacts.remove(contact);
            Toast.makeText(MainActivity.sharedInstance(), "Push Notifications removed for: " +
                    contact.getDisplayName(), Toast.LENGTH_LONG).show();
        } else {
            _pushContacts.add(contact);
            Toast.makeText(MainActivity.sharedInstance(), "Push Notifications added for: " +
                    contact.getDisplayName(), Toast.LENGTH_LONG).show();
        }
        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void pushLongTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Push long tapped: " + contact);
        if (!_pushContacts.contains(contact)) {
            Toast.makeText(MainActivity.sharedInstance(), "Enable Push Notification by tapping " +
                    "first before changing push type for:" + contact.getDisplayName(), Toast
                    .LENGTH_LONG).show();
            return;
        }
        if (contact.getPushType() == null
                || contact.getPushType() == AylaServiceApp.PushType.BaiduPush) {
            Toast.makeText(MainActivity.sharedInstance()
                    , "Switch to Google Push for:" + contact.getDisplayName()
                    , Toast.LENGTH_LONG).show();
            contact.setPushType(AylaServiceApp.PushType.GooglePush);
            return;
        }
        if (contact.getPushType() == AylaServiceApp.PushType.GooglePush) {
            Toast.makeText(MainActivity.sharedInstance()
                    , "Switch to Baidu Push for:" + contact.getDisplayName()
                    , Toast.LENGTH_LONG).show();
            contact.setPushType(AylaServiceApp.PushType.BaiduPush);
        }
    }

    @Override
    public void emailTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Email tapped: " + contact);
        if (TextUtils.isEmpty(contact.getEmail())) {
            Toast.makeText(getActivity(), R.string.contact_email_required,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if ( _emailContacts.contains(contact) ) {
            _emailContacts.remove(contact);
            Toast.makeText(MainActivity.sharedInstance(), "Email Notifications removed for: " +
                    contact.getDisplayName(), Toast.LENGTH_LONG).show();
        } else {
            _emailContacts.add(contact);
            Toast.makeText(MainActivity.sharedInstance(), "Email Notifications added for: " +
                    contact.getDisplayName(), Toast.LENGTH_LONG).show();
        }
        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void smsTapped(AylaContact contact) {
        Log.d(LOG_TAG, "SMS tapped: " + contact);
        if (TextUtils.isEmpty(contact.getPhoneNumber())) {
            Toast.makeText(getActivity(), R.string.contact_phone_required,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if ( _smsContacts.contains(contact) ) {
            _smsContacts.remove(contact);
            Toast.makeText(MainActivity.sharedInstance(), "SMS Notifications removed for: " +
                    contact.getDisplayName(), Toast.LENGTH_LONG).show();
        } else {
            _smsContacts.add(contact);
            Toast.makeText(MainActivity.sharedInstance(), "SMS Notifications added for: " +
                    contact.getDisplayName(), Toast.LENGTH_LONG).show();
        }
        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void contactTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Contact tapped: " + contact);
        if ( _smsContacts.contains(contact) || _emailContacts.contains(contact) ||
                _pushContacts.contains(contact) ) {
            _smsContacts.remove(contact);
            _emailContacts.remove(contact);
            _pushContacts.remove(contact);
        }
        _recyclerView.getAdapter().notifyDataSetChanged();
    }

    @Override
    public void contactLongTapped(AylaContact contact) {
        Log.d(LOG_TAG, "Contact long tapped: " + contact);
    }

    @Override
    public int colorForIcon(AylaContact contact, IconType iconType) {
        switch ( iconType ) {
            case ICON_PUSH_ANDROID:
            case ICON_PUSH_BAIDU:
                if ( _pushContacts.contains(contact) ) {
                    return ContextCompat.getColor(this.getContext(),R.color.app_theme_accent);
                } else {
                    return ContextCompat.getColor(this.getContext(), R.color.disabled_text);
                }

            case ICON_SMS:
                if ( _smsContacts.contains(contact) ) {
                    return ContextCompat.getColor(this.getContext(), R.color.app_theme_accent);
                } else {
                    return ContextCompat.getColor(this.getContext(), R.color.disabled_text);
                }

            case ICON_EMAIL:
                if ( _emailContacts.contains(contact) ) {
                    return ContextCompat.getColor(this.getContext(), R.color.app_theme_accent);
                } else {
                    return ContextCompat.getColor(this.getContext(), R.color.disabled_text);
                }
        }
        return ContextCompat.getColor(this.getContext(), R.color.disabled_text);
    }

    @Override
    public void onClick(View v) {
        // Save notifications
        Log.d(LOG_TAG, "Save Notifications");

        // Make sure things are set up right
        if ( _nameEditText.getText().toString().isEmpty() ) {
            Toast.makeText(getActivity(), R.string.choose_name,
                    Toast.LENGTH_LONG).show();
            _nameEditText.requestFocus();
            return;
        }

        String propName = getNotifiablePropertyNames()[_propertySpinner.getSelectedItemPosition()];
        final AylaProperty prop = _device.getProperty(propName);
        if ( prop == null ) {
            Toast.makeText(getActivity(), R.string.unknown_error, Toast.LENGTH_LONG).show();
            return;
        }

        // Make sure somebody is selected
        if ( _emailContacts.size() + _smsContacts.size() + _pushContacts.size() == 0 ) {
            Toast.makeText(getActivity(), R.string.no_contacts_selected,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // If a value is required, make sure it's set
        Float numberValue;
        try{
            numberValue = Float.parseFloat(_numberEditText.getText().toString());
        } catch (NumberFormatException e) {
            Log.d(LOG_TAG, "Not a number: " + _numberEditText.getText().toString());
            numberValue = null;
        }

        if ( ("integer".equals(prop.getBaseType()) || "decimal".equals(prop.getBaseType())) &&
                (_numberRadioGroup.getCheckedRadioButtonId() != R.id.radio_integer_changes &&
                        _integerLayout.getVisibility() == View.VISIBLE) ){
            if ( numberValue == null ) {
                _numberEditText.requestFocus();
                Toast.makeText(getActivity(), R.string.no_value_chosen,
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Now we should be set to create the trigger and trigger apps.
        AylaPropertyTrigger trigger = new AylaPropertyTrigger();

        trigger.setDeviceNickname(_nameEditText.getText().toString());
        trigger.setBaseType(prop.getBaseType());
        trigger.setActive(true);

        if ( prop.getBaseType().equals("boolean")) {
            switch ( _booleanRadioGroup.getCheckedRadioButtonId() ) {
                case R.id.radio_turn_on:
                    trigger.setTriggerType(TRIGGER_COMPARE_ABSOLUTE);
                    trigger.setCompareType("==");
                    trigger.setValue("1");
                    break;

                case R.id.radio_turn_off:
                    trigger.setTriggerType(TRIGGER_COMPARE_ABSOLUTE);
                    trigger.setCompareType("==");
                    trigger.setValue("0");
                    break;

                case R.id.radio_on_or_off:
                    trigger.setTriggerType(TRIGGER_ALWAYS);
                    break;
            }
        } else if(prop.getBaseType().equals("integer")){
            if(friendlyNameForPropertyName(prop).equals(MainActivity.sharedInstance().getString(R.string
                    .property_motion_sensor_friendly_name))) {
                switch (_motionRadioGroup.getCheckedRadioButtonId()){
                    case R.id.radio_detected:
                        trigger.setTriggerType(TRIGGER_ALWAYS);
                        break;
                    case R.id.radio_stopped:
                        trigger.setTriggerType(TRIGGER_ALWAYS);
                        break;
                }
            } else{
                switch ( _numberRadioGroup.getCheckedRadioButtonId() ) {
                    case R.id.radio_integer_changes:
                        trigger.setTriggerType(TRIGGER_ALWAYS);
                        break;

                    case R.id.radio_integer_greater_than:
                        trigger.setTriggerType(TRIGGER_COMPARE_ABSOLUTE);
                        trigger.setCompareType(">");
                        break;

                    case R.id.radio_integer_less_than:
                        trigger.setTriggerType(TRIGGER_COMPARE_ABSOLUTE);
                        trigger.setCompareType("<");
                        break;
                }
            }
        }

        prop.createTrigger(trigger, new Response.Listener<AylaPropertyTrigger>() {
                    @Override
                    public void onResponse(AylaPropertyTrigger response) {
                        createAppNotifications(response,prop);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(getContext(), error.toString(), Toast.LENGTH_LONG)
                                .show();
                    }
                });

    }

    private void createAppNotifications(final AylaPropertyTrigger trigger, final AylaProperty
            property){
        final List<AylaError> aylaErrorList= new ArrayList<>();
        for (AylaContact emailContact:_emailContacts){
            AylaPropertyTriggerApp triggerApp = new AylaPropertyTriggerApp();
            triggerApp.setEmailAddress(emailContact.getEmail());
            //AylaEmailTemplate template = new AylaEmailTemplate();
            triggerApp.configureAsEmail(emailContact, "[[property_name]] [[property_value]]", null,
                    null);
            trigger.createApp(triggerApp,
                    new Response.Listener<AylaPropertyTriggerApp>() {
                        @Override
                        public void onResponse(AylaPropertyTriggerApp response) {
                            AylaLog.d(LOG_TAG, "Successfully created Trigger App for "+
                                    response.getEmailAddress());
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            aylaErrorList.add(error);
                            Toast.makeText(getContext(), error.toString(), Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
        }
        for (AylaContact smsContact:_smsContacts){
            AylaPropertyTriggerApp triggerApp = new AylaPropertyTriggerApp();
            triggerApp.configureAsSMS(smsContact, "[[property_name]] [[property_value]]");

            trigger.createApp(triggerApp,
                    new Response.Listener<AylaPropertyTriggerApp>() {
                        @Override
                        public void onResponse(AylaPropertyTriggerApp response) {
                            AylaLog.d(LOG_TAG, "Successfully created Trigger App for "+
                                    response.getPhoneNumber());
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            aylaErrorList.add(error);
                            Toast.makeText(MainActivity.sharedInstance(), error.toString(), Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
        }
        for (AylaContact pushContact:_pushContacts){
            AylaPropertyTriggerApp triggerApp = new AylaPropertyTriggerApp();

            if (pushContact.getPushType() == null
                    || pushContact.getPushType() == AylaServiceApp.PushType.GooglePush) {
                String registrationId =PushNotification.registrationId;
                triggerApp.configureAsPushAndroid(registrationId
                        , "[[property_name]] " + "[[property_value]] for Google Push"
                        , "default"
                        , "Google Push meta data");
            } else if (pushContact.getPushType() == AylaServiceApp.PushType.BaiduPush) {
                triggerApp.configureAsPushBaidu(
                        BaiduPushMessageReceiver.getBaiduAppID()
                        , BaiduPushMessageReceiver.getChannelId()
                        , "[[property_name]] " + "[[property_value]] for Baidu Push"
                        , "default"
                        , "Baidu Push meta data");
            }

            trigger.createApp(triggerApp,
                    new Response.Listener<AylaPropertyTriggerApp>() {
                        @Override
                        public void onResponse(AylaPropertyTriggerApp response) {
                            AylaLog.d(LOG_TAG, "Successfully created Trigger App for Push");
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            aylaErrorList.add(error);
                            Toast.makeText(MainActivity.sharedInstance(), error.toString(), Toast.LENGTH_LONG)
                                    .show();
                        }
                    });
        }
        if(aylaErrorList.isEmpty()) {
            //There are no errors in creating the apps
            Toast.makeText(getActivity(), R.string.notification_created,
                    Toast.LENGTH_LONG).show();
            if (_originalTrigger != null) {
                property.deleteTrigger(_originalTrigger, new Response.Listener<
                                AylaAPIRequest.EmptyResponse>() {
                            @Override
                            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                AylaLog.d(LOG_TAG, "Successfully Deleted the old trigger");
                                getFragmentManager().popBackStack();
                            }
                        },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {
                                Toast.makeText(MainActivity.sharedInstance(), error.toString(), Toast.LENGTH_LONG)
                                        .show();
                            }
                        });
            }
            else {
                getFragmentManager().popBackStack();
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String propertyName = getNotifiablePropertyNames()[position];
        Log.d(LOG_TAG, "onItemSelected: "+ _propertySpinner.getSelectedItem() + " == " +
                propertyName);
        AylaProperty prop = _device.getProperty(propertyName);
        if ( prop == null ) {
            Log.e(LOG_TAG, "Failed to get property: " + propertyName);
            return;
        }

        // Hide all of the type-specific layouts
        _booleanLayout.setVisibility(View.INVISIBLE);
        _integerLayout.setVisibility(View.INVISIBLE);
        _motionSensorLayout.setVisibility(View.INVISIBLE);
        Log.d(LOG_TAG, "Property " + propertyName + " base type: " + prop.getBaseType());
        _numberRadioGroup.clearCheck();
        switch ( prop.getBaseType() ) {
            case "boolean":
                _booleanLayout.setVisibility(View.VISIBLE);
                break;
            case "string":
                Log.e(LOG_TAG, "String: Not yet implemented");
                break;
            case "integer":
                if(friendlyNameForPropertyName(prop).equals(MainActivity.sharedInstance().getString(R.string
                        .property_motion_sensor_friendly_name))){
                    _motionSensorLayout.setVisibility(View.VISIBLE);

                } else{
                    _integerLayout.setVisibility(View.VISIBLE);
                    _numberEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    _numberRadioGroup.check(R.id.radio_integer_changes);
                }
                break;
            case "decimal":
                _integerLayout.setVisibility(View.VISIBLE);
                _numberEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.
                        TYPE_NUMBER_FLAG_DECIMAL);
                _numberRadioGroup.check(R.id.radio_integer_changes);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.e(LOG_TAG, "Nothing selected!");
    }
    private String friendlyNameForPropertyName(AylaProperty prop) {
        if (prop.getDisplayName() != null) {
            return prop.getDisplayName();
        }
        return prop.getName();
    }
    private String[] getNotifiablePropertyNames(){
        AylaSystemSettings.DeviceDetailProvider provider =
                AylaNetworks.sharedInstance().getSystemSettings().deviceDetailProvider;

        if (provider != null) {
            return provider.getManagedPropertyNames(_device);
        }
        return new String[0];
    }

}

