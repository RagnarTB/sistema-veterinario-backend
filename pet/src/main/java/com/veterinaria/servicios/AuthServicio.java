package com.veterinaria.servicios;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.veterinaria.dtos.AuthResponseDTO;
import com.veterinaria.dtos.CambiarPasswordRequestDTO;
import com.veterinaria.dtos.GoogleLoginRequestDTO;
import com.veterinaria.dtos.LoginRequestDTO;
import com.veterinaria.dtos.MensajeResponseDTO;
import com.veterinaria.dtos.RegistroClienteDTO;
import com.veterinaria.modelos.Cliente;
import com.veterinaria.modelos.Rol;
import com.veterinaria.modelos.Usuario;
import com.veterinaria.respositorios.ClienteRepositorio;
import com.veterinaria.respositorios.RolRespositorio;
import com.veterinaria.respositorios.UsuarioRepositorio;
import com.veterinaria.seguridad.JwtServicio;

import jakarta.transaction.Transactional;

@Service
public class AuthServicio {

    private UsuarioRepositorio usuarioRepositorio;
    private RolRespositorio rolRespositorio;
    private ClienteRepositorio clienteRepositorio;
    private final RefreshTokenServicio refreshTokenServicio;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtServicio jwtServicio;
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    private final GoogleTokenVerifierServicio googleTokenVerifierServicio;

    public AuthServicio(UsuarioRepositorio usuarioRepositorio, RolRespositorio rolRespositorio,
            ClienteRepositorio clienteRepositorio, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtServicio jwtServicio,
            org.springframework.security.core.userdetails.UserDetailsService userDetailsService,
            RefreshTokenServicio refreshTokenServicio,
            GoogleTokenVerifierServicio googleTokenVerifierServicio) {
        this.usuarioRepositorio = usuarioRepositorio;
        this.rolRespositorio = rolRespositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtServicio = jwtServicio;
        this.userDetailsService = userDetailsService;
        this.refreshTokenServicio = refreshTokenServicio;
        this.googleTokenVerifierServicio = googleTokenVerifierServicio;
    }

    @Transactional
    public MensajeResponseDTO registrarCliente(RegistroClienteDTO dto) {
        if (usuarioRepositorio.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El email ya estÃ¡ registrado");
        }
        if (clienteRepositorio.existsByDni(dto.getDni())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El DNI ya se encuentra registrado");
        }

        Rol rolCliente = rolRespositorio.findByNombre("ROLE_CLIENTE")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El rol ROLE_CLIENTE no existe en la BD"));

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setEmail(dto.getEmail());
        nuevoUsuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        nuevoUsuario.getRoles().add(rolCliente);

        Usuario usuarioGuardado = usuarioRepositorio.save(nuevoUsuario);

        Cliente nuevoCliente = new Cliente();
        nuevoCliente.setNombre(dto.getNombre());
        nuevoCliente.setApellido(dto.getApellido());
        nuevoCliente.setDni(dto.getDni());
        nuevoCliente.setTelefono(dto.getTelefono());
        nuevoCliente.setEmail(dto.getEmail());
        nuevoCliente.setUsuario(usuarioGuardado);

        clienteRepositorio.save(nuevoCliente);

        return new MensajeResponseDTO("Cliente registrado con Ã©xito");
    }

    public AuthResponseDTO login(LoginRequestDTO dto) {
        Usuario usuario = usuarioRepositorio.findByEmail(dto.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no vÃ¡lido"));

        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cuenta no tiene inicio de sesiÃ³n por correo habilitado");
        }

        authenticationManager.authenticate(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        dto.getEmail(), dto.getPassword()));

        return construirAuthResponse(usuario);
    }

    @Transactional
    public AuthResponseDTO loginConGoogle(GoogleLoginRequestDTO dto) {
        GoogleTokenVerifierServicio.GoogleUserInfo googleUserInfo = googleTokenVerifierServicio.verificar(dto.getIdToken());

        Usuario usuario = usuarioRepositorio.findByEmail(googleUserInfo.email())
                .map(usuarioExistente -> vincularGoogleAUsuarioExistente(usuarioExistente, googleUserInfo))
                .orElseGet(() -> crearClienteDesdeGoogle(googleUserInfo));

        return construirAuthResponse(usuario);
    }

    @Transactional
    public MensajeResponseDTO cambiarPassword(String emailUsuarioLogueado, CambiarPasswordRequestDTO dto) {
        Usuario usuario = usuarioRepositorio.findByEmail(emailUsuarioLogueado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cuenta no tiene password local configurado");
        }

        if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseÃ±a actual es incorrecta");
        }

        if (passwordEncoder.matches(dto.getPasswordNueva(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La nueva contraseÃ±a no puede ser igual a la anterior");
        }

        usuario.setPassword(passwordEncoder.encode(dto.getPasswordNueva()));
        usuarioRepositorio.save(usuario);
        refreshTokenServicio.revocarTodosLosTokensDeUsuario(usuario.getId());

        return new MensajeResponseDTO("ContraseÃ±a actualizada correctamente");
    }

    public AuthResponseDTO refreshToken(String refreshTokenRaw) {
        String refreshTokenNuevo = refreshTokenServicio.rotarRefreshToken(refreshTokenRaw);

        Usuario usuario = refreshTokenServicio.obtenerUsuarioDesdeRefreshToken(refreshTokenNuevo);
        org.springframework.security.core.userdetails.UserDetails userDetails = userDetailsService
                .loadUserByUsername(usuario.getEmail());

        String token = jwtServicio.generarToken(userDetails);
        return new AuthResponseDTO(token, refreshTokenNuevo, usuario.getEmail(), extraerRoles(usuario));
    }

    @Transactional
    public MensajeResponseDTO logout(String refreshTokenRaw) {
        refreshTokenServicio.revocarRefreshToken(refreshTokenRaw);
        return new MensajeResponseDTO("SesiÃ³n cerrada correctamente");
    }

    private AuthResponseDTO construirAuthResponse(Usuario usuario) {
        org.springframework.security.core.userdetails.UserDetails userDetails = userDetailsService
                .loadUserByUsername(usuario.getEmail());
        String token = jwtServicio.generarToken(userDetails);
        String refreshToken = refreshTokenServicio.crearRefreshTokenParaUsuario(usuario);
        return new AuthResponseDTO(token, refreshToken, usuario.getEmail(), extraerRoles(usuario));
    }

    private Usuario vincularGoogleAUsuarioExistente(Usuario usuario, GoogleTokenVerifierServicio.GoogleUserInfo googleUserInfo) {
        if (!tieneRol(usuario, "ROLE_CLIENTE")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El correo ya pertenece a una cuenta no habilitada para acceso con Google");
        }

        if (usuario.getGoogleSubject() != null && !usuario.getGoogleSubject().equals(googleUserInfo.subject())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cuenta ya estÃ¡ vinculada a un perfil distinto de Google");
        }

        usuario.setGoogleSubject(googleUserInfo.subject());
        usuario.setGoogleVinculado(true);
        Usuario usuarioGuardado = usuarioRepositorio.save(usuario);

        Cliente cliente = clienteRepositorio.findByUsuario(usuarioGuardado)
                .orElseGet(() -> crearClienteDesdeGoogle(usuarioGuardado, googleUserInfo));

        actualizarClienteDesdeGoogle(cliente, googleUserInfo);
        return usuarioGuardado;
    }

    private Usuario crearClienteDesdeGoogle(GoogleTokenVerifierServicio.GoogleUserInfo googleUserInfo) {
        Rol rolCliente = rolRespositorio.findByNombre("ROLE_CLIENTE")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El rol ROLE_CLIENTE no existe en la BD"));

        Usuario usuario = new Usuario();
        usuario.setEmail(googleUserInfo.email());
        usuario.setGoogleSubject(googleUserInfo.subject());
        usuario.setGoogleVinculado(true);
        usuario.getRoles().add(rolCliente);

        Usuario usuarioGuardado = usuarioRepositorio.save(usuario);
        crearClienteDesdeGoogle(usuarioGuardado, googleUserInfo);
        return usuarioGuardado;
    }

    private Cliente crearClienteDesdeGoogle(Usuario usuario, GoogleTokenVerifierServicio.GoogleUserInfo googleUserInfo) {
        Cliente cliente = new Cliente();
        cliente.setNombre(valorSeguro(googleUserInfo.nombre()));
        cliente.setApellido(valorSeguro(googleUserInfo.apellido()));
        cliente.setTelefono("");
        cliente.setDni("");
        cliente.setEmail(usuario.getEmail());
        cliente.setUsuario(usuario);
        return clienteRepositorio.save(cliente);
    }

    private void actualizarClienteDesdeGoogle(Cliente cliente, GoogleTokenVerifierServicio.GoogleUserInfo googleUserInfo) {
        boolean actualizado = false;

        if (estaVacio(cliente.getNombre()) && !estaVacio(googleUserInfo.nombre())) {
            cliente.setNombre(googleUserInfo.nombre());
            actualizado = true;
        }
        if (estaVacio(cliente.getApellido()) && !estaVacio(googleUserInfo.apellido())) {
            cliente.setApellido(googleUserInfo.apellido());
            actualizado = true;
        }
        if (estaVacio(cliente.getEmail())) {
            cliente.setEmail(googleUserInfo.email());
            actualizado = true;
        }

        if (actualizado) {
            clienteRepositorio.save(cliente);
        }
    }

    private boolean tieneRol(Usuario usuario, String rol) {
        return usuario.getRoles().stream().anyMatch(item -> rol.equals(item.getNombre()));
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String valorSeguro(String valor) {
        return valor == null ? "" : valor;
    }

    private Set<String> extraerRoles(Usuario usuario) {
        return usuario.getRoles().stream()
                .map(Rol::getNombre)
                .collect(Collectors.toSet());
    }
}
