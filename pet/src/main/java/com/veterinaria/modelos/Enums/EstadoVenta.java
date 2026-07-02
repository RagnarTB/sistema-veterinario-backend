package com.veterinaria.modelos.Enums;

public enum EstadoVenta {
    ACTIVA,          // Creada, pendiente de pago total
    PAGADA_PARCIAL,  // Tiene pagos pero no cubre el total
    PAGADA,          // Totalmente pagada
    ANULADA          // Anulada por admin
}
