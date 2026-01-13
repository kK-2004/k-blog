package com.kk.kblog.service.admin;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Slf4j
public class FastApiIpLocationResolver implements IpLocationResolver {

    public record IpInfo(
            String ip,
            String country,
            String province,
            String city,
            String isp
    ) {
    }

    private final RestClient restClient;
    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final String urlTemplate;
    private final IpLocationResolver fallback;

    public FastApiIpLocationResolver(
            String urlTemplate,
            int connectTimeoutMs,
            int readTimeoutMs,
            IpLocationResolver fallback
    ) {
        this.urlTemplate = urlTemplate;
        this.fallback = fallback;

        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public String resolveLocation(String ip) {
        if (ip == null || ip.isBlank()) return "UNKNOWN";
        if ("127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) return "LOCAL";

        try {
            var info = fetch(ip);
            var formatted = format(info);
            System.out.println(formatted);
            if (formatted == null || formatted.isBlank() || "UNKNOWN".equalsIgnoreCase(formatted)) {
                return fallback.resolveLocation(ip);
            }
            return formatted;
        } catch (Exception ignored) {
            return fallback.resolveLocation(ip);
        }
    }

    private @Nullable IpInfo fetch(String ip) throws IOException {
        return restClient.get()
                .uri(urlTemplate, Map.of("ip", ip))
                .exchange((req, res) -> {
                    if (res.getStatusCode().value() != HttpStatus.OK.value()) {
                        log.debug("ip_fastapi_non_200 ip={} status={}", ip, res.getStatusCode().value());
                        return null;
                    }
                    var bytes = res.getBody().readAllBytes();
                    if (bytes.length == 0) {
                        log.debug("ip_fastapi_empty_body ip={}", ip);
                        return null;
                    }
                    return objectMapper.readValue(bytes, IpInfo.class);
                });
    }

    private @Nullable String format(@Nullable IpInfo info) {
        if (info == null) return null;

        var country = clean(info.country());
        var province = clean(info.province());
        var city = clean(info.city());
        var isp = clean(info.isp());

        // Keep as much info as possible; ignore blanks.
        // Examples:
        // - 中国 广东省 深圳市 电信
        // - 澳大利亚 新南威尔士 悉尼
        return joinNonBlank(country, province, city, isp);
    }

    private String joinNonBlank(String... parts) {
        List<String> items = new ArrayList<>();
        for (var p : parts) {
            if (p != null && !p.isBlank()) items.add(p.trim());
        }
        return items.isEmpty() ? "UNKNOWN" : String.join(" ", items);
    }

    private String clean(String value) {
        if (value == null) return "";
        var v = value.trim();
        return v.equals("0") ? "" : v;
    }
}
