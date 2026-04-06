package com.veterinaria.controladores;

import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.veterinaria.dtos.ConsentimientoRequestDTO;
import com.veterinaria.dtos.ConsentimientoResponseDTO;
import com.veterinaria.servicios.ConsentimientoServicio;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import com.lowagie.text.Document;
import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/api/consentimientos")
@RequiredArgsConstructor
public class ConsentimientoController {

    private final ConsentimientoServicio consentimientoServicio;

    @PostMapping("/generar")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ConsentimientoResponseDTO> generarConsentimiento(
            @Valid @RequestBody ConsentimientoRequestDTO requestDTO) {
        ConsentimientoResponseDTO response = consentimientoServicio.generarConsentimiento(requestDTO);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/aceptar")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ConsentimientoResponseDTO> aceptarConsentimiento(@PathVariable Long id) {
        ConsentimientoResponseDTO response = consentimientoServicio.aceptarConsentimiento(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cirugia/{cirugiaId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIO', 'RECEPCIONISTA')")
    public ResponseEntity<ConsentimientoResponseDTO> obtenerPorCirugia(@PathVariable Long cirugiaId) {
        ConsentimientoResponseDTO response = consentimientoServicio.obtenerPorCirugia(cirugiaId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        ConsentimientoResponseDTO dto = consentimientoServicio.obtenerPorCirugia(id);

        // Lógica rápida de OpenPDF
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("CONSENTIMIENTO INFORMADO"));
        document.add(new Paragraph(dto.getTextoLegal()));
        document.close();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "consentimiento.pdf");

        return new ResponseEntity<>(out.toByteArray(), headers, HttpStatus.OK);
    }
}
