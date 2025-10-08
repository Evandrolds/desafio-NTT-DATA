package com.evandro.ntt_data.desafio.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
public class ApiExternaConfiguration {

    @Value("${spring.datasource.properties.api_url}")
    private String api_Url;

    public String getApi_Url() {
        return api_Url;
    }
    public String getReturnQuery(double raio, double lat, double lon){
        return String.format(
                Locale.US,
                "[out:json];node[\"amenity\"=\"bank\"](around:%d,%.6f,%.6f);out;",
                (int)(raio * 1000), lat, lon
        );
    }
}
