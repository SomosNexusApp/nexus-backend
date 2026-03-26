package com.nexus.service;

import org.springframework.stereotype.Service;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.util.Utils;
import org.springframework.beans.factory.annotation.Value;

@Service
public class TwoFactorAuthService {

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), timeProvider);

    @Value("${totp.issuer:Nexus App}")
    private String issuer;

    public String generateNewSecret() {
        return secretGenerator.generate();
    }

    public String getQrCodeImage(String secret, String label) throws Exception {
        QrData data = new QrData.Builder()
                .label(label)
                .secret(secret)
                .issuer(issuer)
                .algorithm(dev.samstevens.totp.code.HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        QrGenerator generator = new ZxingPngQrGenerator();
        byte[] imageData = generator.generate(data);
        return Utils.getDataUriForImage(imageData, generator.getImageMimeType());
    }

    public boolean isCodeValid(String secret, String code) {
        return verifier.isValidCode(secret, code);
    }
}
