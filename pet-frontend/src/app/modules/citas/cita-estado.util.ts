import { EstadoCita } from '../../core/models/models';

export const ESTADO_LABELS: Record<EstadoCita, string> = {
  AGENDADA: 'Agendada',
  CONFIRMADA: 'Confirmada',
  EN_SALA_ESPERA: 'En sala de espera',
  EN_CONSULTORIO: 'En consultorio',
  COMPLETADA: 'Completada',
  CANCELADA: 'Cancelada',
  NO_ASISTIO: 'No asistió',
};

export const ESTADO_COLORS: Record<EstadoCita, string> = {
  AGENDADA: 'estado-agendada',
  CONFIRMADA: 'estado-confirmada',
  EN_SALA_ESPERA: 'estado-sala-espera',
  EN_CONSULTORIO: 'estado-consultorio',
  COMPLETADA: 'estado-completada',
  CANCELADA: 'estado-cancelada',
  NO_ASISTIO: 'estado-no-asistio',
};

export const ESTADO_ICONS: Record<EstadoCita, string> = {
  AGENDADA: 'event_note',
  CONFIRMADA: 'check_circle',
  EN_SALA_ESPERA: 'event_seat',
  EN_CONSULTORIO: 'medical_services',
  COMPLETADA: 'task_alt',
  CANCELADA: 'cancel',
  NO_ASISTIO: 'person_off',
};

const TRANSICIONES: Record<EstadoCita, EstadoCita[]> = {
  AGENDADA: ['CONFIRMADA', 'EN_SALA_ESPERA', 'CANCELADA', 'NO_ASISTIO'],
  CONFIRMADA: ['EN_SALA_ESPERA', 'CANCELADA', 'NO_ASISTIO'],
  EN_SALA_ESPERA: ['EN_CONSULTORIO', 'CANCELADA', 'NO_ASISTIO'],
  EN_CONSULTORIO: ['COMPLETADA'],
  COMPLETADA: [],
  CANCELADA: [],
  NO_ASISTIO: [],
};

export function getTransicionesPermitidas(estado: EstadoCita): EstadoCita[] {
  return TRANSICIONES[estado] || [];
}

/** Atajo para el flujo lineal de recepción */
export function getSiguienteEstadoRecepcion(estado: EstadoCita): EstadoCita | null {
  const flujo: Partial<Record<EstadoCita, EstadoCita>> = {
    AGENDADA: 'CONFIRMADA',
    CONFIRMADA: 'EN_SALA_ESPERA',
    EN_SALA_ESPERA: 'EN_CONSULTORIO',
    EN_CONSULTORIO: 'COMPLETADA',
  };
  return flujo[estado] || null;
}

export function esEstadoFinal(estado: EstadoCita): boolean {
  return estado === 'COMPLETADA' || estado === 'CANCELADA' || estado === 'NO_ASISTIO';
}

export function esEstadoActivo(estado: EstadoCita): boolean {
  return !esEstadoFinal(estado);
}
