export interface Alerta {
    id: number;
    loteId: number;
    identificador?: string;
    porcentajeMerma: number;
    umbral: number;
    causa: string;
    costoUnitario?: number;
    margen?: number;
    fecha?: string;
}
