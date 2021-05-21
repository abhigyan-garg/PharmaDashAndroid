package com.PharmaDash.PharmaDashApp.historyRecyclerView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.PharmaDash.PharmaDashApp.R;

import java.util.List;

public class HistoryAdapter  extends RecyclerView.Adapter<HistoryViewHolders> {
    private List<HistoryObject> itemList;
    private Context context;
    private String userCustomerOrDriver;

    public HistoryAdapter(List<HistoryObject> itemList, Context context, String customerOrDriver){
        this.itemList=itemList;
        this.context=context;
        userCustomerOrDriver = customerOrDriver;
    }

    @NonNull
    @Override
    public HistoryViewHolders onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);
        HistoryViewHolders rcv = new HistoryViewHolders(layoutView, userCustomerOrDriver);
        return rcv;
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolders holder, int position) {
        holder.rideId = itemList.get(position).getRideId();
        holder.time.setText(itemList.get(position).getTime());
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

}
