package com.veterinaria.seguridad;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtServicio jwtServicio;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtServicio jwtServicio, UserDetailsService userDetailsService) {
        this.jwtServicio = jwtServicio;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 1. Extraer el header "Authorization"
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Si no hay header o no empieza con "Bearer ", lo ignoramos y seguimos (Spring
        // Security lo bloqueará después si la ruta es privada)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extraer el token (quitamos la palabra "Bearer " que tiene 7 letras)
        jwt = authHeader.substring(7);
        try {
            userEmail = jwtServicio.extraerUsername(jwt);
        } catch (JwtException | IllegalArgumentException ex) {
            // Token inválido/expirado/malformado: no autenticamos y dejamos que Spring Security
            // responda 401 si el endpoint lo requiere.
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Si hay email en el token y el usuario aún no está autenticado en este hilo
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                
                // Verificar la caducidad por cambio de permisos
                // Extraer la fecha de emisión del token
                java.util.Date iat = jwtServicio.extraerIssuedAt(jwt);
                boolean tokenObsoleto = false;
                
                // Necesitamos acceso a los roles del usuario desde la BD
                // Obtenemos los roles casteando si es necesario o usando un repositorio si estuviera inyectado.
                // Como UserDetails ya contiene las Authorities, la forma más limpia es inyectar UsuarioRepositorio en el filtro.
                // Pero podemos resolverlo extrayendo el usuario aquí
                com.veterinaria.modelos.Usuario u = null;
                if (userDetailsService instanceof UserDetailsServiceImpl) {
                    u = ((UserDetailsServiceImpl) userDetailsService).getUsuarioPorEmail(userEmail);
                }
                
                if (u != null) {
                    for (com.veterinaria.modelos.Rol r : u.getRoles()) {
                        if (r.getFechaModificacionPermisos() != null) {
                            java.util.Date modDate = java.util.Date.from(r.getFechaModificacionPermisos().atZone(java.time.ZoneId.systemDefault()).toInstant());
                            // Si el token fue emitido antes de la modificación de permisos, es obsoleto
                            if (iat.before(modDate)) {
                                tokenObsoleto = true;
                                break;
                            }
                        }
                    }
                }

                if (tokenObsoleto) {
                    filterChain.doFilter(request, response);
                    return;
                }

                if (jwtServicio.esTokenValido(jwt, userDetails)) {
                    java.util.List<String> tokenRoles = jwtServicio.extraerRoles(jwt);
                    java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities;
                    
                    if (tokenRoles != null && !tokenRoles.isEmpty()) {
                        authorities = tokenRoles.stream()
                            .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
                            .collect(java.util.stream.Collectors.toList());
                    } else {
                        authorities = userDetails.getAuthorities();
                    }
                    
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, authorities);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ex) {
                // Ignore and continue chain
            }
        }

        filterChain.doFilter(request, response); // Continuar con la petición
    }
}