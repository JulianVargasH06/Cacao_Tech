import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

interface Funcionalidad {
    icono: string;
    titulo: string;
    descripcion: string;
}

/**
 * Página de inicio informativa (ruta raíz). Es estática: no hace peticiones al backend.
 * Lleva al login del equipo y, para los compradores, a la consulta pública del producto.
 */
@Component({
    selector: 'app-landing',
    imports: [RouterLink, ButtonModule, CardModule],
    templateUrl: './landing.html',
    styleUrl: './landing.css'
})
export class Landing {
    protected readonly anio = new Date().getFullYear();
    protected readonly version = '1.0.0';

    protected readonly funcionalidades: Funcionalidad[] = [
        {
            icono: 'pi pi-inbox',
            titulo: 'Gestión de lotes y recepción',
            descripcion: 'Registro detallado del origen, las fechas y el ingreso de la materia prima de cada productor.'
        },
        {
            icono: 'pi pi-sync',
            titulo: 'Transformación y mermas',
            descripcion: 'Control de pérdidas operativas, subproductos y eficiencia de cada etapa del procesamiento.'
        },
        {
            icono: 'pi pi-shield',
            titulo: 'Seguridad e historial',
            descripcion: 'Monitoreo transparente respaldado por autenticación segura, roles y auditoría de cada cambio.'
        }
    ];

    protected readonly beneficios: string[] = [
        'Reducción de desperdicios en planta',
        'Visibilidad completa desde la recepción hasta el producto final',
        'Datos confiables para la toma de decisiones'
    ];
}
