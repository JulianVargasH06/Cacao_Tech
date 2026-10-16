import { HttpParams } from '@angular/common/http';

/** Convierte un objeto de filtros en parámetros de URL, omitiendo los vacíos: { estado: 'BLOQUEADO', codigo: '' } → ?estado=BLOQUEADO */
export function paramsDe(filtros: object = {}): HttpParams {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtros)) {
        if (valor !== null && valor !== undefined && valor !== '') params = params.set(clave, String(valor));
    }
    return params;
}
