package com.example.springaidemo.weather;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service
public class QWeatherService {

    private final WeatherProperties properties;
    private final RestClient restClient;

    public QWeatherService(WeatherProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(properties.getReadTimeoutMillis());
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }

    public TodayWeatherResult getTodayWeather(String city) {
        String normalizedCity = StringUtils.hasText(city) ? city.trim() : "北京";
        if (!StringUtils.hasText(properties.getApiKey())) {
            return TodayWeatherResult.unavailable(normalizedCity, "未配置天气服务 API Key");
        }

        try {
            CityLookupResult lookup = lookupCity(normalizedCity);
            if (!lookup.available()) {
                return TodayWeatherResult.unavailable(normalizedCity, lookup.message());
            }

            JSONObject now = queryNowWeather(lookup.locationId());
            return TodayWeatherResult.available(
                    lookup.name(),
                    lookup.adm2(),
                    lookup.adm1(),
                    now.getString("text"),
                    now.getString("temp"),
                    now.getString("feelsLike"),
                    now.getString("windDir"),
                    now.getString("windScale"),
                    now.getString("humidity"),
                    now.getString("precip"),
                    now.getString("obsTime")
            );
        } catch (Exception exception) {
            log.error("天气服务调用失败", exception);
            return TodayWeatherResult.unavailable(normalizedCity, "天气服务调用失败: " + exception.getMessage());
        }
    }

    private CityLookupResult lookupCity(String city) throws Exception {
        String url = UriComponentsBuilder.fromHttpUrl(trimTrailingSlash(properties.resolveGeoBaseUrl()))
                .path("/geo/v2/city/lookup")
                .queryParam("location", city)
                .queryParam("number", 1)
                .encode()
                .build()
                .toUriString();

        byte[] body;
        try {
            body = restClient.get()
                    .uri(url)
                    .header("X-QW-Api-Key", properties.getApiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception exception) {
            log.error("城市查询请求失败，URL={}", url, exception);
            throw new IllegalStateException("城市查询请求失败，URL=" + url, exception);
        }

        JSONObject root = JSON.parseObject(readResponseBody(body));
        String code = root.getString("code");
        if (!"200".equals(code)) {
            return CityLookupResult.unavailable("城市查询失败，状态码: " + code);
        }

        JSONArray locations = root.getJSONArray("location");
        JSONObject first = locations != null && !locations.isEmpty() ? locations.getJSONObject(0) : null;
        if (first == null) {
            return CityLookupResult.unavailable("未找到对应城市");
        }

        return CityLookupResult.available(
                first.getString("id"),
                defaultString(first.getString("name"), city),
                defaultString(first.getString("adm2"), ""),
                defaultString(first.getString("adm1"), "")
        );
    }

    private JSONObject queryNowWeather(String locationId) throws IOException {
        String url = UriComponentsBuilder.fromHttpUrl(trimTrailingSlash(properties.resolveWeatherBaseUrl()))
                .path("/v7/weather/now")
                .queryParam("location", locationId)
                .encode()
                .build()
                .toUriString();

        byte[] body;
        try {
            body = restClient.get()
                    .uri(url)
                    .header("X-QW-Api-Key", properties.getApiKey())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(byte[].class);
        } catch (Exception exception) {
            log.error("实时天气请求失败，URL={}", url, exception);
            throw new IllegalStateException("实时天气请求失败，URL=" + url, exception);
        }

        JSONObject root = JSON.parseObject(readResponseBody(body));
        String code = root.getString("code");
        if (!"200".equals(code)) {
            throw new IllegalStateException("实时天气查询失败，状态码: " + code);
        }
        return root.getJSONObject("now");
    }

    private String readResponseBody(byte[] body) throws IOException {
        if (body == null || body.length == 0) {
            return "";
        }
        if (isGzip(body)) {
            try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(body));
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                gzipInputStream.transferTo(outputStream);
                return outputStream.toString(StandardCharsets.UTF_8);
            }
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private boolean isGzip(byte[] body) {
        return body.length >= 2
                && (body[0] & 0xFF) == 0x1F
                && (body[1] & 0xFF) == 0x8B;
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public record TodayWeatherResult(
            boolean available,
            String city,
            String district,
            String province,
            String weatherText,
            String temperature,
            String feelsLike,
            String windDirection,
            String windScale,
            String humidity,
            String precipitation,
            String observedAt,
            String message
    ) {
        static TodayWeatherResult available(String city,
                                            String district,
                                            String province,
                                            String weatherText,
                                            String temperature,
                                            String feelsLike,
                                            String windDirection,
                                            String windScale,
                                            String humidity,
                                            String precipitation,
                                            String observedAt) {
            return new TodayWeatherResult(true, city, district, province, weatherText, temperature, feelsLike,
                    windDirection, windScale, humidity, precipitation, observedAt, "");
        }

        static TodayWeatherResult unavailable(String city, String message) {
            return new TodayWeatherResult(false, city, "", "", "", "", "", "", "", "", "", "", message);
        }
    }

    private record CityLookupResult(
            boolean available,
            String locationId,
            String name,
            String adm2,
            String adm1,
            String message
    ) {
        static CityLookupResult available(String locationId, String name, String adm2, String adm1) {
            return new CityLookupResult(true, locationId, name, adm2, adm1, "");
        }

        static CityLookupResult unavailable(String message) {
            return new CityLookupResult(false, "", "", "", "", message);
        }
    }
}
