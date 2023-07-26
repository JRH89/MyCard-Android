package mycard.mycard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import mycard.mycard.ForecastItem;
import mycard.mycard.R;

public class ForecastAdapter extends RecyclerView.Adapter<ForecastAdapter.ForecastViewHolder> {

    private List<ForecastItem> forecastItems;
    private boolean isImperialUnit;

    public ForecastAdapter(List<ForecastItem> forecastItems, boolean isImperialUnit) {
        this.forecastItems = forecastItems;
        this.isImperialUnit = isImperialUnit;
    }

    public void setForecastItems(List<ForecastItem> forecastItems) {
        this.forecastItems = forecastItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forecast, parent, false);
        return new ForecastViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        ForecastItem forecastItem = forecastItems.get(position);
        holder.textViewDateTime.setText(forecastItem.getDateTime());
        holder.textViewTemperature.setText(isImperialUnit ? forecastItem.getTemperatureFahrenheit() + " °F" : forecastItem.getTemperatureCelsius() + " °C");
        holder.textViewDescription.setText(forecastItem.getDescription());
    }

    @Override
    public int getItemCount() {
        return forecastItems.size();
    }

    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        TextView textViewDateTime;
        TextView textViewTemperature;
        TextView textViewDescription;

        public ForecastViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewDateTime = itemView.findViewById(R.id.textViewDateTime);
            textViewTemperature = itemView.findViewById(R.id.textViewTemperature);
            textViewDescription = itemView.findViewById(R.id.textViewDescription);
        }
    }
}
