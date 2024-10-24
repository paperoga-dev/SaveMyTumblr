package com.github.savemytumblr;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Url {
    String main;
    String path;
    Map<String, String> bodyParams = new HashMap<>();

    public Url(String sMain, String sPath) {
        this.main = sMain;
        this.path = sPath;
    }

    public void addParameter(String key, String value) {
        this.bodyParams.put(key, value);
    }

    @Override
    public String toString() {
        String result = "";

        if (!this.main.isEmpty()) {
            result += this.main;
        }

        if (!this.path.isEmpty()) {
            result += this.path;
        }

        List<String> qParams = new ArrayList<>();
        for (Map.Entry<String, ?> entry : this.bodyParams.entrySet()) {
            qParams.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                    + URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }

        if (qParams.size() > 0) {
            if (!result.isEmpty()) {
                result += "?";
            }
            result += String.join("&", qParams);
        }

        return result;
    }
}
