package com.example.springaidemo.weather;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.weather")
public class WeatherProperties {

    private String provider = "qweather";
    private String apiKey;
    private String apiHost = "https://kh2yw2259h.re.qweatherapi.com";
    private String geoBaseUrl;
    private String weatherBaseUrl;
    private int connectTimeoutMillis = 5000;
    private int readTimeoutMillis = 5000;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getGeoBaseUrl() {
        return geoBaseUrl;
    }

    public void setGeoBaseUrl(String geoBaseUrl) {
        this.geoBaseUrl = geoBaseUrl;
    }

    public String getWeatherBaseUrl() {
        return weatherBaseUrl;
    }

    public void setWeatherBaseUrl(String weatherBaseUrl) {
        this.weatherBaseUrl = weatherBaseUrl;
    }

    public String getApiHost() {
        return apiHost;
    }

    public void setApiHost(String apiHost) {
        this.apiHost = apiHost;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public String resolveGeoBaseUrl() {
        return StringUtils.hasText(geoBaseUrl) ? geoBaseUrl : apiHost;
    }

    public String resolveWeatherBaseUrl() {
        return StringUtils.hasText(weatherBaseUrl) ? weatherBaseUrl : apiHost;
    }
}
