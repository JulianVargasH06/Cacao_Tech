/** Hace que el navegador descargue un archivo recibido del backend (ej. el CSV de lotes). */
export function descargarArchivo(contenido: Blob, nombre: string): void {
    const url = URL.createObjectURL(contenido);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = nombre;
    enlace.click();
    URL.revokeObjectURL(url);
}

/** "lotes-20261016.csv" */
export function nombreConFecha(prefijo: string, extension: string): string {
    const hoy = new Date();
    const fecha = `${hoy.getFullYear()}${String(hoy.getMonth() + 1).padStart(2, '0')}${String(hoy.getDate()).padStart(2, '0')}`;
    return `${prefijo}-${fecha}.${extension}`;
}
