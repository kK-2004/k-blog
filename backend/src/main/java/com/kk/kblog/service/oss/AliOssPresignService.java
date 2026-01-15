package com.kk.kblog.service.oss;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;

@Service
public class AliOssPresignService {
    private static final Logger log = LoggerFactory.getLogger(AliOssPresignService.class);

    private final AliOssProperties props;
    private OSS client;

    public AliOssPresignService(AliOssProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        if (!props.isEnabled()) {
            log.info("Ali OSS presign disabled");
            return;
        }
        if (!StringUtils.hasText(props.getEndpoint())
                || !StringUtils.hasText(props.getBucket())
                || !StringUtils.hasText(props.getAccessKeyId())
                || !StringUtils.hasText(props.getAccessKeySecret())) {
            log.warn("Ali OSS presign enabled but config incomplete");
            return;
        }
        this.client = new OSSClientBuilder().build(props.getEndpoint(), props.getAccessKeyId(), props.getAccessKeySecret());
        log.info("Ali OSS presign client initialized. endpoint={}, bucket={}", props.getEndpoint(), props.getBucket());
    }

    @PreDestroy
    public void destroy() {
        if (client != null) {
            client.shutdown();
        }
    }

    public boolean isReady() {
        return props.isEnabled()
                && client != null
                && StringUtils.hasText(props.getBucket());
    }

    public URL presignPut(String objectKey, String contentType, long expireSeconds) {
        if (!isReady()) throw new IllegalStateException("OSS not configured");

        long exp = expireSeconds > 0 ? expireSeconds : props.getPresignExpireSeconds();
        var expiration = Date.from(Instant.now().plusSeconds(Math.max(60, exp)));

        var req = new GeneratePresignedUrlRequest(props.getBucket(), objectKey, HttpMethod.PUT);
        req.setExpiration(expiration);
        if (StringUtils.hasText(contentType)) {
            req.setContentType(contentType);
        }
        HashMap<String, String> headers = new HashMap<>();
        headers.put("x-oss-object-acl", "public-read");
        req.setHeaders(headers);

        // 生成预签名 URL 后强制使用 HTTPS（避免混合内容错误）
        var url = client.generatePresignedUrl(req);
        try {
            return new URL(url.toString().replaceFirst("^http://", "https://"));
        } catch (java.net.MalformedURLException e) {
            log.warn("Failed to convert presigned URL to HTTPS, using original", e);
            return url;
        }
    }

    public void deleteByKey(String objectKey) {
        if (!isReady()) throw new IllegalStateException("OSS not configured");
        if (!StringUtils.hasText(objectKey)) return;
        client.deleteObject(props.getBucket(), objectKey);
    }

    public String toPublicUrl(String objectKey) {
        if (StringUtils.hasText(props.getPublicBaseUrl())) {
            var base = props.getPublicBaseUrl().trim();
            if (!base.endsWith("/")) base += "/";
            return base + objectKey;
        }

        var ep = props.getEndpoint() == null ? "" : props.getEndpoint().trim();
        ep = ep.replaceFirst("^https?://", "");
        ep = ep.replaceAll("/+$", "");

        return "https://" + props.getBucket() + "." + ep + "/" + objectKey;
    }

    public String normalizeKey(String key) {
        if (!StringUtils.hasText(key)) return "";
        var prefix = props.getPrefix() == null ? "" : props.getPrefix().trim();
        if (StringUtils.hasText(prefix) && !prefix.endsWith("/")) prefix += "/";
        var k = key.startsWith("/") ? key.substring(1) : key;
        return prefix + k;
    }
}
