import { Component } from '@angular/core';

import { PageHeader } from '../../../shared/page-header/page-header';

@Component({
    selector: 'app-recepcion',
    imports: [PageHeader],
    template: `
    <section class="page">
      <app-page-header
        titulo="Recepción de lotes"
        descripcion="Registra los lotes de cacao que llegan a la asociación"
        historia="HU01"
        rol="Operador"
      />
    </section>
  `
})
export class Recepcion { }