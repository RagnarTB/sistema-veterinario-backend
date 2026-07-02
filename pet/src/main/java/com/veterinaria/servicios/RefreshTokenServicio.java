package com.veterinaria.servicios;

import com.veterinaria.modelos.RefreshToken;
import com.veterinaria.modelos.Usuario;
import com.veterinaria.respositorios.RefreshTokenRepositorio;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenServicio {

    private static final Duration REFRESH_TTL = Duration.ofDays(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepositorio refreshTokenRepositorio;

    public RefreshTokenServicio(RefreshTokenRepositorio refreshTokenRepositorio) {
        this.refreshTokenRepositorio = refreshTokenRepositorio;
    }

    public String crearRefreshTokenParaUsuario(Usuario usuario) {
        String raw = generarTokenSeguro();
        String hash = sha256Hex(raw);

        RefreshToken rt = new RefreshToken();
        rt.setUsuario(usuario);
        rt.setTokenHash(hash);
        rt.setExpiresAt(Instant.now().plus(REFRESH_TTL));
        refreshTokenRepositorio.save(rt);

        return raw;
    }

    public String rotarRefreshToken(String refreshTokenRaw) {
        RefreshToken actual = validarYObtener(refreshTokenRaw);

        // revocamos el actual y emitimos uno nuevo (rotación)
        actual.setRevoked(true);
        refreshTokenRepositorio.save(actual);

        String rawNuevo = generarTokenSeguro();
        String hashNuevo = sha256Hex(rawNuevo);

        RefreshToken nuevo = new RefreshToken();
        nuevo.setUsuario(actual.getUsuario());
        nuevo.setTokenHash(hashNuevo);
        nuevo.setExpiresAt(Instant.now().plus(REFRESH_TTL));
        nuevo.setRotatedFromTokenHash(actual.getTokenHash());
        refreshTokenRepositorio.save(nuevo);

        return rawNuevo;
    }

    public Usuario obtenerUsuarioDesdeRefreshToken(String refreshTokenRaw) {
        return validarYObtener(refreshTokenRaw).getUsuario();
    }

    public void revocarRefreshToken(String refreshTokenRaw) {
        RefreshToken rt = validarYObtener(refreshTokenRaw);
        rt.setRevoked(true);
        refreshTokenRepositorio.save(rt);
    }

    public void revocarTodosLosTokensDeUsuario(Long usuarioId) {
        refreshTokenRepositorio.revokeAllByUsuarioId(usuarioId);
    }

    private RefreshToken validarYObtener(String refreshTokenRaw) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "refreshToken es obligatorio");
        }

        String hash = sha256Hex(refreshTokenRaw);
        RefreshToken rt = refreshTokenRepositorio.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido"));

        if (Boolean.TRUE.equals(rt.getRevoked())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token revocado");
        }
        if (rt.getExpiresAt() == null || rt.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expirado");
        }

        return rt;
    }

    private static String generarTokenSeguro() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}

