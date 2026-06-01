package com.example.springaidemo.login.auth.support;

import com.example.springaidemo.login.auth.config.LoginPasswordCryptoProperties;
import com.example.springaidemo.login.auth.exception.LoginException;
import com.example.springaidemo.login.config.LoginConfigConst;
import com.example.springaidemo.login.config.service.SystemConfigService;
import com.example.springaidemo.login.config.service.SystemConfigCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
public class RsaPasswordCryptoService {

    private final SystemConfigService systemConfigService;
    private final SystemConfigCacheService cacheService;
    private final LoginPasswordCryptoProperties cryptoProperties;
    private volatile KeyPair fallbackKeyPair;
    private volatile String lastResolvedSource = "";

    public RsaPasswordCryptoService(SystemConfigService systemConfigService,
                                    SystemConfigCacheService cacheService,
                                    LoginPasswordCryptoProperties cryptoProperties) {
        this.systemConfigService = systemConfigService;
        this.cacheService = cacheService;
        this.cryptoProperties = cryptoProperties;
    }

    public String decrypt(String encryptedPassword) {
        try {
            String algorithm = resolveAlgorithm();
            if ("RSA_PKCS1".equals(algorithm)) {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.DECRYPT_MODE, resolvePrivateKey());
                byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
                return new String(decrypted, StandardCharsets.UTF_8);
            }
            if ("RSA_OAEP_SHA256".equals(algorithm)) {
                Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
                cipher.init(Cipher.DECRYPT_MODE, resolvePrivateKey(),
                        new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
                byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
                return new String(decrypted, StandardCharsets.UTF_8);
            }
            throw new LoginException("不支持的密码加密模式");
        } catch (Exception exception) {
            log.error("登录密码解密失败", exception);
            throw new LoginException("密码解密失败");
        }
    }

    private String resolveAlgorithm() {
        String algorithm = systemConfigService.getValue(LoginConfigConst.PASSWORD_ENCRYPT_ALGORITHM, "RSA_OAEP_SHA256");
        if ("RSA_PKCS1".equalsIgnoreCase(algorithm)) {
            return "RSA_PKCS1";
        }
        if ("RSA_OAEP_SHA256".equalsIgnoreCase(algorithm) || "RSA-OAEP".equalsIgnoreCase(algorithm)) {
            return "RSA_OAEP_SHA256";
        }
        throw new LoginException("不支持的密码加密模式");
    }

    public String publicKeyBase64() {
        return Base64.getEncoder().encodeToString(resolveActiveKeyPair().getPublic().getEncoded());
    }

    private PrivateKey resolvePrivateKey() {
        return resolveActiveKeyPair().getPrivate();
    }

    public PublicKey resolvePublicKey() {
        return resolveActiveKeyPair().getPublic();
    }

    private KeyPair resolveActiveKeyPair() {
        cacheService.evictValues(LoginConfigConst.PASSWORD_KEY_CONFIGS);
        String publicKey = systemConfigService.getValue(LoginConfigConst.PASSWORD_ENCRYPT_PUBLIC_KEY, "");
        String privateKey = systemConfigService.getValue(LoginConfigConst.PASSWORD_ENCRYPT_PRIVATE_KEY, "");
        KeyPair configKeyPair = resolveKeyPair(publicKey, privateKey, "配置中心");
        if (configKeyPair != null) {
            return markResolvedKeyPair(configKeyPair, "配置中心");
        }
        KeyPair ymlKeyPair = resolveKeyPair(cryptoProperties.getPublicKey(), cryptoProperties.getPrivateKey(), "YML");
        if (ymlKeyPair != null) {
            return markResolvedKeyPair(ymlKeyPair, "YML");
        }
        return markResolvedKeyPair(resolveFallbackKeyPair(), "运行期自动生成");
    }

    private KeyPair markResolvedKeyPair(KeyPair keyPair, String sourceName) {
        String fingerprint = fingerprint(keyPair.getPublic());
        String marker = sourceName + ":" + fingerprint;
        if (!marker.equals(lastResolvedSource)) {
            lastResolvedSource = marker;
            log.info("登录密码 RSA 密钥来源已生效：source={}, publicKeyFingerprint={}", sourceName, fingerprint);
        }
        return keyPair;
    }

    private KeyPair resolveKeyPair(String publicKey, String privateKey, String sourceName) {
        if (!StringUtils.hasText(publicKey) || !StringUtils.hasText(privateKey)) {
            return null;
        }
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey resolvedPublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(normalizeKey(publicKey))));
            PrivateKey resolvedPrivateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(normalizeKey(privateKey))));
            if (isSameKeyPair(resolvedPublicKey, resolvedPrivateKey)) {
                return new KeyPair(resolvedPublicKey, resolvedPrivateKey);
            }
            log.error("登录密码 RSA {}公钥和私钥不匹配，跳过该密钥来源", sourceName);
            return null;
        } catch (Exception exception) {
            log.error("解析登录密码 RSA {}密钥失败，跳过该密钥来源", sourceName, exception);
            return null;
        }
    }

    private boolean isSameKeyPair(PublicKey publicKey, PrivateKey privateKey) {
        if (publicKey instanceof RSAPublicKey rsaPublicKey && privateKey instanceof RSAPrivateKey rsaPrivateKey) {
            return rsaPublicKey.getModulus().equals(rsaPrivateKey.getModulus());
        }
        return false;
    }

    private String normalizeKey(String key) {
        return key
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
    }

    private String fingerprint(PublicKey publicKey) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(publicKey.getEncoded());
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (Exception exception) {
            return "unknown";
        }
    }

    private KeyPair resolveFallbackKeyPair() {
        KeyPair current = fallbackKeyPair;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (fallbackKeyPair == null) {
                fallbackKeyPair = createFallbackKeyPair();
            }
            return fallbackKeyPair;
        }
    }

    private KeyPair createFallbackKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            log.warn("未配置登录密码 RSA 密钥对，已自动生成临时密钥对。publicKey={}",
                    Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            log.warn("未配置登录密码 RSA 密钥对，已自动生成临时密钥对。privateKey={}",
                    Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            return keyPair;
        } catch (Exception exception) {
            log.error("生成登录密码兜底 RSA 密钥失败", exception);
            throw new IllegalStateException("Unable to generate fallback RSA key pair", exception);
        }
    }
}
