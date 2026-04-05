package com.nexus.service;

import java.util.Optional;
import com.nexus.entity.Actor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nexus.repository.ActorRepository;

@Service
public class ActorService {

    @Autowired
    private ActorRepository actorRepository;

    public Optional<Actor> findById(Integer id) {
        return actorRepository.findById(id);
    }


    @Transactional
    public void activar2FA(Integer actorId, String metodo, String secret) {
        actorRepository.findById(actorId).ifPresent(a -> {
            a.setTwoFactorEnabled(true);
            a.setTwoFactorMethod(metodo);
            if (secret != null) a.setTwoFactorSecret(secret);
            actorRepository.save(a);
        });
    }

    @Transactional
    public void desactivar2FA(Integer actorId) {
        actorRepository.findById(actorId).ifPresent(a -> {
            a.setTwoFactorEnabled(false);
            a.setTwoFactorMethod(null);
            a.setTwoFactorSecret(null);
            actorRepository.save(a);
        });
    }

    public String getTotpSecret(Integer actorId) {
        return actorRepository.findById(actorId)
            .map(a -> a.getTwoFactorSecret())
            .orElse(null);
    }

    @Transactional
    public void incrementarJwtVersion(Integer actorId) {
        actorRepository.findById(actorId).ifPresent(a -> {
            a.setJwtVersion(a.getJwtVersion() + 1);
            actorRepository.save(a);
        });
    }

    public int getJwtVersion(Integer actorId) {
        return actorRepository.findById(actorId)
            .map(a -> a.getJwtVersion())
            .orElse(0);
    }
}