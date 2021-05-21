package com.PharmaDash.PharmaDashApp.historyRecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.PharmaDash.PharmaDashApp.HistorySingleActivity;
import com.PharmaDash.PharmaDashApp.R;

public class HistoryViewHolders extends RecyclerView.ViewHolder implements View.OnClickListener {

    public String rideId;
    public TextView time;
    private String userCustomerOrDriver;
    public HistoryViewHolders(@NonNull View itemView, String customerOrDriver) {
        super(itemView);
        itemView.setOnClickListener(this);

        time = (TextView) itemView.findViewById(R.id.time);
        userCustomerOrDriver = customerOrDriver;
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(view.getContext(), HistorySingleActivity.class);
        Bundle b = new Bundle();
        b.putString("rideId", rideId);
        b.putString("customerOrDriver", userCustomerOrDriver);
        intent.putExtras(b);
        view.getContext().startActivity(intent);

    }

}
