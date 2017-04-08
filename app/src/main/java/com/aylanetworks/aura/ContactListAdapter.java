package com.aylanetworks.aura;
/*
 * AylaSDK
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import com.aylanetworks.aura.util.ContactHelper;
import com.aylanetworks.aylasdk.AylaContact;
import com.aylanetworks.aylasdk.AylaServiceApp;

import java.util.ArrayList;
import java.util.List;

public class ContactListAdapter extends RecyclerView.Adapter {
    public interface ContactCardListener {
        enum IconType { ICON_PUSH_BAIDU, ICON_PUSH_ANDROID, ICON_EMAIL, ICON_SMS }

        void pushTapped(AylaContact contact);
        void pushLongTapped(AylaContact contact);
        void emailTapped(AylaContact contact);
        void smsTapped(AylaContact contact);
        void contactTapped(AylaContact contact);
        void contactLongTapped(AylaContact contact);
        int colorForIcon(AylaContact contact, IconType iconType);
    }

    private final List<AylaContact> _aylaContacts = new ArrayList<>();
    private final ContactCardListener _listener;

    public ContactListAdapter(ContactCardListener listener) {
        _listener = listener;
        _aylaContacts.addAll(ContactHelper.getContacts());
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_contact,
                parent, false);
        return new ContactViewHolder(v, _listener);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ContactViewHolder h = (ContactViewHolder) holder;
        h._contact = _aylaContacts.get(position);

        if (TextUtils.isEmpty(h._contact.getDisplayName()) || TextUtils.isEmpty(h._contact
                        .getDisplayName().trim()
        )) {
            if (TextUtils.isEmpty(h._contact.getFirstname()) && TextUtils.isEmpty(
                    h._contact.getLastname())) {
                h._contactNameTextView.setText(h._contact.getEmail());
            } else {
                String str= h._contact.getFirstname() +" " + h._contact.getLastname();
                h._contactNameTextView.setText(str);
            }
        } else {
            h._contactNameTextView.setText(h._contact.getDisplayName());
        }

        // TODO: Figure out what to do with push notifications, owner contact, etc.
        h._pushButton.setVisibility(View.INVISIBLE);

        h._emailButton.setColorFilter(_listener.colorForIcon(h._contact,
                ContactCardListener.IconType.ICON_EMAIL));
        h._smsButton.setColorFilter(_listener.colorForIcon(h._contact,
                ContactCardListener.IconType.ICON_SMS));
    }

    @Override
    public int getItemCount() {
        return _aylaContacts.size();
    }

     class ContactViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener,
             View.OnLongClickListener {
        private AylaContact _contact;
        private final TextView _contactNameTextView;
        private final ImageButton _pushButton;
        private final ImageButton _emailButton;
        private final ImageButton _smsButton;
        private final ContactCardListener _listener;

        public ContactViewHolder(View v, ContactCardListener listener) {
            super(v);
            _listener = listener;
            v.setOnClickListener(this);
            v.setOnLongClickListener(this);
            _contactNameTextView = (TextView) v.findViewById(R.id.contact_name);

            _pushButton = (ImageButton)v.findViewById(R.id.button_push);
            _emailButton = (ImageButton)v.findViewById(R.id.button_email);
            _smsButton = (ImageButton)v.findViewById(R.id.button_sms);

            _pushButton.setOnClickListener(this);
            _pushButton.setOnLongClickListener(this);

            _emailButton.setOnClickListener(this);
            _smsButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            AylaContact contact = _aylaContacts.get(getAdapterPosition());
            switch (v.getId()) {
                case R.id.button_push:
                    _listener.pushTapped(contact);
                    break;

                case R.id.button_sms:
                    _listener.smsTapped(contact);
                    break;

                case R.id.button_email:
                    _listener.emailTapped(contact);
                    break;

                default:
                    _listener.contactTapped(contact);
                    break;
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if ( v == _pushButton) {
                AylaContact contact = _aylaContacts.get(getAdapterPosition());
                _listener.pushLongTapped(contact);
                if (contact.getPushType() == AylaServiceApp.PushType.BaiduPush) {
                    _pushButton.setImageResource(R.drawable.ic_push_baidu);
                } else {
                    _pushButton.setImageResource(R.drawable.ic_push_google);
                }
                return true;
            }

            _listener.contactLongTapped(_aylaContacts.get(getAdapterPosition()));
            return true;
        }
    }
}

