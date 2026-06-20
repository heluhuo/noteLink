package com.example.myapplication.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 天气 API 工具类
 * 使用 wttr.in 免费天气接口，无需 API Key
 *
 * API: https://wttr.in/{city}?format=j1
 * 备用: Open-Meteo (https://api.open-meteo.com)
 */
public class WeatherApi {

    private static final String API_URL = "https://wttr.in/%s?format=j1";
    private static final String FALLBACK_URL = "https://api.open-meteo.com/v1/forecast"
            + "?latitude=%f&longitude=%f"
            + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
            + "&daily=temperature_2m_max,temperature_2m_min,weather_code"
            + "&timezone=Asia%%2FShanghai";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    // 城市经纬度映射（常用城市）
    private static final java.util.Map<String, double[]> CITY_COORDS = new java.util.HashMap<>();
    static {
        CITY_COORDS.put("北京",  new double[]{39.9042, 116.4074});
        CITY_COORDS.put("上海",  new double[]{31.2304, 121.4737});
        CITY_COORDS.put("广州",  new double[]{23.1291, 113.2644});
        CITY_COORDS.put("深圳",  new double[]{22.5431, 114.0579});
        CITY_COORDS.put("杭州",  new double[]{30.2741, 120.1551});
        CITY_COORDS.put("成都",  new double[]{30.5728, 104.0668});
        CITY_COORDS.put("武汉",  new double[]{30.5928, 114.3055});
        CITY_COORDS.put("南京",  new double[]{32.0603, 118.7969});
        CITY_COORDS.put("重庆",  new double[]{29.4316, 106.9123});
        CITY_COORDS.put("西安",  new double[]{34.3416, 108.9398});
        CITY_COORDS.put("天津",  new double[]{39.3434, 117.3616});
        CITY_COORDS.put("苏州",  new double[]{31.2990, 120.5853});
        CITY_COORDS.put("长沙",  new double[]{28.2282, 112.9388});
        CITY_COORDS.put("郑州",  new double[]{34.7466, 113.6254});
        CITY_COORDS.put("青岛",  new double[]{36.0671, 120.3826});
        CITY_COORDS.put("大连",  new double[]{38.9140, 121.6147});
        CITY_COORDS.put("厦门",  new double[]{24.4798, 118.0894});
        CITY_COORDS.put("福州",  new double[]{26.0745, 119.2965});
        CITY_COORDS.put("昆明",  new double[]{25.0389, 102.7183});
        CITY_COORDS.put("哈尔滨", new double[]{45.8038, 126.5350});
    }

    /**
     * 天气数据模型
     */
    public static class WeatherData {
        public String city;
        public String date;
        public String weather;      // 天气描述，如 "晴"
        public String temperature;  // 温度，如 "18℃ ~ 26℃"
        public String wind;         // 风力
        public String humidity;     // 湿度
        public String airQuality;   // 空气质量（wttr.in 不提供）
        public String tips;         // 温馨提示
        public boolean success;

        public WeatherData() { this.success = false; }
    }

    /**
     * 异步获取天气
     */
    public static Future<WeatherData> fetch(String city) {
        return executor.submit(new WeatherTask(city));
    }

    /**
     * 同步获取天气
     */
    public static WeatherData fetchSync(String city) {
        return new WeatherTask(city).call();
    }

    /**
     * WMO 天气码 → 中文描述映射
     */
    private static String wmoToChinese(int code) {
        if (code <= 1) return "晴";
        if (code == 2) return "多云";
        if (code == 3) return "阴";
        if (code <= 49) return "雾/霾";
        if (code <= 59) return "小雨";
        if (code <= 69) return "中雨";
        if (code <= 79) return "雨夹雪";
        if (code <= 82) return "阵雨";
        if (code <= 86) return "雪";
        if (code <= 94) return "冰雹";
        if (code == 95) return "雷阵雨";
        if (code <= 99) return "大雷雨";
        return "未知";
    }

    private static class WeatherTask implements Callable<WeatherData> {
        private final String city;

        WeatherTask(String city) {
            this.city = city;
        }

        @Override
        public WeatherData call() {
            // 优先尝试 wttr.in
            WeatherData data = tryWttr();
            if (data.success) return data;

            // 备用：尝试 Open-Meteo
            data = tryOpenMeteo();
            return data;
        }

        /** 尝试 wttr.in API */
        private WeatherData tryWttr() {
            WeatherData data = new WeatherData();
            try {
                // 处理英文城市名（如拼音）
                String queryCity = city;
                if (CITY_COORDS.containsKey(city)) {
                    // 用拼音查询 wttr.in
                    // 保留原中文名用于显示
                }

                String urlStr = String.format(API_URL, URLEncoder.encode(queryCity, "UTF-8"));
                String json = httpGet(urlStr);
                if (json == null || json.isEmpty()) return data;

                JSONObject root = new JSONObject(json);

                // 城市名
                JSONArray areaArr = root.optJSONArray("nearest_area");
                if (areaArr != null && areaArr.length() > 0) {
                    JSONObject area = areaArr.getJSONObject(0);
                    JSONArray areaNameArr = area.optJSONArray("areaName");
                    if (areaNameArr != null && areaNameArr.length() > 0) {
                        data.city = areaNameArr.getJSONObject(0).optString("value", city);
                    }
                    // 国家
                    JSONArray countryArr = area.optJSONArray("country");
                    if (countryArr != null && countryArr.length() > 0) {
                        String country = countryArr.getJSONObject(0).optString("value", "");
                        if (country.equals("China") && !data.city.contains("中国")) {
                            // 已经是城市名，不额外处理
                        }
                    }
                }
                if (data.city == null || data.city.isEmpty()) data.city = city;

                // 当前天气
                JSONArray currentArr = root.optJSONArray("current_condition");
                if (currentArr != null && currentArr.length() > 0) {
                    JSONObject current = currentArr.getJSONObject(0);

                    data.temperature = current.optString("temp_C", "") + "℃";
                    data.humidity = current.optString("humidity", "") + "%";

                    JSONArray descArr = current.optJSONArray("weatherDesc");
                    if (descArr != null && descArr.length() > 0) {
                        String engDesc = descArr.getJSONObject(0).optString("value", "");
                        data.weather = translateWeather(engDesc);
                    }

                    String windSpeed = current.optString("windspeedKmph", "");
                    String windDir = current.optString("winddir16Point", "");
                    if (!windSpeed.isEmpty()) {
                        data.wind = windDir + " " + windSpeed + "km/h";
                    }
                }

                // 今日天气范围 & 温馨提示
                JSONArray weatherArr = root.optJSONArray("weather");
                if (weatherArr != null && weatherArr.length() > 0) {
                    JSONObject today = weatherArr.getJSONObject(0);
                    data.date = today.optString("date", "");

                    // 平均温度
                    String avgHigh = today.optString("maxtempC", "");
                    String avgLow = today.optString("mintempC", "");
                    if (!avgLow.isEmpty() && !avgHigh.isEmpty()) {
                        data.temperature = avgLow + "℃ ~ " + avgHigh + "℃";
                    }

                    // 小时数据（用于获取当日最高最低和提示）
                    JSONArray hourlyArr = today.optJSONArray("hourly");
                    if (hourlyArr != null && hourlyArr.length() > 0) {
                        // 计算当日最高最低温度
                        double maxTemp = -999, minTemp = 999;
                        String dayDesc = "";
                        int rainChance = 0;
                        for (int i = 0; i < hourlyArr.length(); i++) {
                            JSONObject h = hourlyArr.getJSONObject(i);
                            double t = h.optDouble("tempC", -999);
                            if (t > maxTemp) maxTemp = t;
                            if (t < minTemp) minTemp = t;
                            int rain = h.optInt("chanceofrain", 0);
                            if (rain > rainChance) rainChance = rain;
                        }
                        if (maxTemp > -999 && minTemp < 999) {
                            data.temperature = String.format("%.0f℃ ~ %.0f℃", minTemp, maxTemp);
                        }

                        // 温馨提示
                        StringBuilder tips = new StringBuilder();
                        if (rainChance > 50) {
                            tips.append("降雨概率 ").append(rainChance).append("%，建议带伞");
                        } else {
                            // 检查是否有风
                            double windMax = 0;
                            for (int i = 0; i < hourlyArr.length(); i++) {
                                double w = hourlyArr.getJSONObject(i).optDouble("windspeedKmph", 0);
                                if (w > windMax) windMax = w;
                            }
                            if (windMax > 30) {
                                tips.append("风力较大，注意防风");
                            } else {
                                tips.append("天气不错，适合出门");
                            }
                        }
                        data.tips = tips.toString();
                    }
                }

                // wttr.in 不提供 AQI，用湿度代替说明
                data.airQuality = data.humidity != null ? "湿度 " + data.humidity : "";

                data.success = true;
            } catch (Exception e) {
                data.success = false;
            }
            return data;
        }

        /** 备用：Open-Meteo API */
        private WeatherData tryOpenMeteo() {
            WeatherData data = new WeatherData();
            try {
                double lat, lon;
                double[] coords = CITY_COORDS.get(city);
                if (coords != null) {
                    lat = coords[0];
                    lon = coords[1];
                } else {
                    // 默认北京
                    lat = 39.9042;
                    lon = 116.4074;
                }

                String urlStr = String.format(java.util.Locale.US, FALLBACK_URL, lat, lon);
                String json = httpGet(urlStr);
                if (json == null || json.isEmpty()) return data;

                JSONObject root = new JSONObject(json);
                JSONObject current = root.optJSONObject("current");
                if (current == null) return data;

                data.city = city;

                double temp = current.optDouble("temperature_2m", -999);
                int weatherCode = current.optInt("weather_code", 0);
                double humidity = current.optDouble("relative_humidity_2m", -1);
                double windSpeed = current.optDouble("wind_speed_10m", -1);

                data.weather = wmoToChinese(weatherCode);
                data.temperature = String.format("%.1f℃", temp);
                if (humidity >= 0) data.humidity = String.format("%.0f%%", humidity);
                if (windSpeed >= 0) data.wind = String.format("%.1f km/h", windSpeed);
                data.airQuality = data.humidity != null ? "湿度 " + data.humidity : "";

                // 今日最高最低
                JSONObject daily = root.optJSONObject("daily");
                if (daily != null) {
                    JSONArray maxArr = daily.optJSONArray("temperature_2m_max");
                    JSONArray minArr = daily.optJSONArray("temperature_2m_min");
                    if (maxArr != null && minArr != null && maxArr.length() > 0 && minArr.length() > 0) {
                        double maxT = maxArr.optDouble(0, -999);
                        double minT = minArr.optDouble(0, -999);
                        if (maxT > -999 && minT < 999) {
                            data.temperature = String.format("%.0f℃ ~ %.0f℃", minT, maxT);
                        }
                    }

                    JSONArray wcArr = daily.optJSONArray("weather_code");
                    if (wcArr != null && wcArr.length() > 0) {
                        data.weather = wmoToChinese(wcArr.optInt(0, 0));
                    }
                }

                data.tips = temp > 30 ? "天气炎热，注意防晒" : temp < 10 ? "天气较冷，注意保暖" : "体感舒适";
                data.success = true;
            } catch (Exception e) {
                data.success = false;
            }
            return data;
        }

        /** HTTP GET 请求 */
        private String httpGet(String urlStr) {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", "NoteLink/1.0");

                int code = conn.getResponseCode();
                if (code != 200) {
                    conn.disconnect();
                    return null;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                conn.disconnect();
                return sb.toString();
            } catch (Exception e) {
                return null;
            }
        }

        /** 英文天气描述 → 中文 */
        private String translateWeather(String eng) {
            if (eng == null) return "未知";
            String lower = eng.toLowerCase();
            if (lower.contains("sunny") || lower.contains("clear")) return "晴";
            if (lower.contains("partly cloudy")) return "多云";
            if (lower.contains("cloudy") || lower.contains("overcast")) return "阴";
            if (lower.contains("mist") || lower.contains("fog") || lower.contains("haze")) return "雾/霾";
            if (lower.contains("light rain") || lower.contains("drizzle")) return "小雨";
            if (lower.contains("moderate rain")) return "中雨";
            if (lower.contains("heavy rain") || lower.contains("rain")) return "大雨";
            if (lower.contains("thunder")) return "雷阵雨";
            if (lower.contains("snow") || lower.contains("sleet")) return "雪";
            if (lower.contains("ice") || lower.contains("hail")) return "冰雹";
            // 默认返回原文首字母大写
            if (eng.length() > 1) {
                return eng.substring(0, 1).toUpperCase() + eng.substring(1).toLowerCase();
            }
            return eng;
        }
    }
}
