package com.safar.app.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.safar.app.R;
import com.safar.app.models.RideAd;
import com.safar.app.utils.DateUtils;

import java.util.List;

public class RideAdAdapter extends RecyclerView.Adapter<RideAdAdapter.ViewHolder> {

    public interface OnAdClickListener {
        void onAdClick(RideAd ad);
    }

    private List<RideAd> ads;
    private final OnAdClickListener listener;

    public RideAdAdapter(List<RideAd> ads, OnAdClickListener listener) {
        this.ads = ads;
        this.listener = listener;
    }

    public void updateList(List<RideAd> newAds) {
        this.ads = newAds;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_ad, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RideAd ad = ads.get(position);
        holder.tvRoute.setText(ad.getOrigin() + "  →  " + ad.getDestination());
        holder.tvDateTime.setText(DateUtils.formatDeparture(ad.getDepartureTime()));
        holder.tvSeats.setText(ad.getAvailableSeats() + " seat(s)");
        holder.tvPostedBy.setText("By " + ad.getCreatorName());

        String status = ad.getStatus();
        holder.tvStatus.setText(status);
        int color = RideAd.STATUS_OPEN.equals(status)
                ? holder.itemView.getContext().getColor(R.color.status_open)
                : holder.itemView.getContext().getColor(R.color.status_filled);
        holder.tvStatus.setTextColor(color);

        if (ad.getVehicleDetails() != null && !ad.getVehicleDetails().isEmpty()) {
            holder.tvVehicle.setText("🚗 " + ad.getVehicleDetails());
            holder.tvVehicle.setVisibility(View.VISIBLE);
        } else {
            holder.tvVehicle.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onAdClick(ad));
    }

    @Override
    public int getItemCount() { return ads.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute, tvDateTime, tvSeats, tvPostedBy, tvStatus, tvVehicle;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvSeats = itemView.findViewById(R.id.tvSeats);
            tvPostedBy = itemView.findViewById(R.id.tvPostedBy);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvVehicle = itemView.findViewById(R.id.tvVehicle);
        }
    }
}
