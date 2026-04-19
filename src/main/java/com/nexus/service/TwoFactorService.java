package com.nexus.service;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nexus.entity.Actor;
import com.nexus.repository.ActorRepository;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.qr.*;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
/**
 * servicio de autenticacion de dos factores (2FA).
 * soporta dos metodos:
 * - TOTP: codigo de 6 digitos que rota cada 30 segundos (Google Authenticator, etc.)
 * - OTP EMAIL: codigo de 6 digitos que se manda por email, caduca en X minutos
 *
 * Los codigos se guardan en memoria (ConcurrentHashMap) porque son temporales.
 * OJO: si hay multiples instancias del servidor (cluster) esto no funcionaria bien.
 * Para un cluster habria que usar Redis u otro almacen compartido.
 *
 * Overloads disponibles (distintas firmas para distintos controladores):
 *
 * AjustesController:
 *   configurarTotp(Integer)                        
 *   verificarCodigoTotp(Integer, String)           
 *   enviarOtpEmail(String, Integer)                
 *
 * ActorController:
 *   enviarOtpEmail(Integer, String, String)        
 *   verificarLoginTotp(Integer, String)            
 */
@Service
public class TwoFactorService {
    @Autowired private ActorRepository actorRepository;
    @Autowired private EmailService    emailService;
    @Value("${totp.issuer:Nexus App}") private String issuer; // nombre que aparece en el autenticador
    @Value("${nexus.two-factor-email.expiry-minutes:10}") private int expiry; // minutos hasta que caduca el otp
    // mapa en memoria: actorId -> {codigo, fechaExpiracion}
    private final ConcurrentHashMap<Integer,OtpEntry> otpStore=new ConcurrentHashMap<>();
    // guardamos los secrets TOTP pendientes de confirmacion hasta que el usuario verifica el primer codigo
    private final ConcurrentHashMap<Integer,String> pendingSecrets=new ConcurrentHashMap<>();

    // --- TOTP (Time-based One-Time Password) ---
    // genera un secret TOTP y el QR para escanear con el autenticador
    // el secret queda "pendiente" hasta que el usuario confirma con el primer codigo
    public Map<String,String> configurarTotp(Integer actorId) {
        Actor a=actorRepository.findById(actorId).orElseThrow(()->new IllegalArgumentException("Actor no encontrado"));
        String secret=new DefaultSecretGenerator().generate();
        pendingSecrets.put(actorId,secret); // guardamos en espera de confirmacion
        try{
            QrData qr=new QrData.Builder().label(a.getEmail()).secret(secret).issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1).digits(6).period(30).build();
            byte[]bytes=new ZxingPngQrGenerator().generate(qr);
            // devolvemos el secret (por si el usuario quiere introducirlo manualmente) y el QR en base64
            return Map.of("secret",secret,"qr","data:image/png;base64,"+Base64.getEncoder().encodeToString(bytes),"issuer",issuer,"accountName",a.getEmail());
        }catch(Exception e){throw new RuntimeException("Error QR: "+e.getMessage());}
    }
    // el usuario ha escaneado el QR y envia el primer codigo para confirmar que funciona
    @Transactional
    public boolean confirmarActivacionTotp(Integer actorId, String codigo) {
        String secret=pendingSecrets.get(actorId);
        if(secret==null||!verificarTotp(secret,codigo))return false; // codigo incorrecto o no habia pendiente
        Actor a=actorRepository.findById(actorId).orElseThrow(()->new IllegalArgumentException("Actor no encontrado"));
        a.setTwoFactorSecret(secret); a.setTwoFactorEnabled(true); a.setTwoFactorMethod("TOTP");
        actorRepository.save(a); pendingSecrets.remove(actorId); return true;
    }
    /** verifica un codigo TOTP mirando el secret del actor en la bbdd */
    public boolean verificarCodigoTotp(Integer actorId, String codigo) {
        Actor a=actorRepository.findById(actorId).orElse(null);
        if(a==null||a.getTwoFactorSecret()==null)return false;
        return verificarTotp(a.getTwoFactorSecret(),codigo);
    }
    /** alias usado en el login con TOTP */
    public boolean verificarLoginTotp(Integer actorId, String codigo) { return verificarCodigoTotp(actorId,codigo); }
    /** overload que recibe el secret directamente (para el flujo de activacion) */
    public boolean verificarCodigoTotp(String secret, String codigo) { return verificarTotp(secret,codigo); }
    private boolean verificarTotp(String secret, String codigo) {
        try{return new DefaultCodeVerifier(new DefaultCodeGenerator(),new SystemTimeProvider()).isValidCode(secret,codigo);}
        catch(Exception e){return false;}
    }

    // --- OTP por Email ---
    // tres versiones con distinto orden de parametros por compatibilidad con llamadas existentes
    public void enviarOtpEmail(String email, Integer actorId) { _sendOtp(email,actorId,"verificacion de identidad"); }
    public void enviarOtpEmail(String email, Integer actorId, String motivo) { _sendOtp(email,actorId,motivo); }
    public void enviarOtpEmail(Integer actorId, String email, String motivo) { _sendOtp(email,actorId,motivo); }

    // genera un otp de 6 digitos, lo guarda en memoria con su fecha de expiracion y lo envia por email
    private void _sendOtp(String email, Integer actorId, String motivo) {
        String otp=String.format("%06d",new SecureRandom().nextInt(999999));
        otpStore.put(actorId,new OtpEntry(otp,LocalDateTime.now().plusMinutes(expiry)));
        emailService.enviarOtp2FA(email,otp,motivo);
    }
    // verifica el otp y lo borra si es correcto o si ya ha caducado
    public boolean verificarOtpEmail(Integer actorId, String codigo) {
        OtpEntry e=otpStore.get(actorId); if(e==null)return false;
        if(e.expira().isBefore(LocalDateTime.now())){otpStore.remove(actorId);return false;} // caducado
        boolean ok=e.otp().equals(codigo); if(ok)otpStore.remove(actorId); return ok; // borramos si acierta
    }
    // record de Java: clase inmutable para guardar el otp y su fecha de expiracion juntos
    private record OtpEntry(String otp, LocalDateTime expira){}
}
