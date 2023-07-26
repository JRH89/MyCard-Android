package mycard.mycard;

public class ForecastItem {
    private String dateTime;
    private double temperatureCelsius;
    private double temperatureFahrenheit;
    private String description;

    public ForecastItem(String dateTime, double temperatureCelsius, double temperatureFahrenheit, String description) {
        this.dateTime = dateTime;
        this.temperatureCelsius = temperatureCelsius;
        this.temperatureFahrenheit = temperatureFahrenheit;
        this.description = description;
    }

    public String getDateTime() {
        return dateTime;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public double getTemperatureFahrenheit() {
        return temperatureFahrenheit;
    }

    public String getDescription() {
        return description;
    }
}

