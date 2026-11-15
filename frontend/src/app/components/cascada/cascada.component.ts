import { CurrencyPipe } from '@angular/common';
import { Component, ElementRef, Input, QueryList, ViewChildren, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Producto } from '../../models/restaurant.models';
import { RestauranteService } from '../../services/restaurante.service';

@Component({
  selector: 'app-cascada',
  standalone: true,
  imports: [CurrencyPipe, RouterLink],
  template: `
    <section class="cascada" aria-label="Selección del chef en movimiento">
      <div class="cascada-controles">
        <button type="button" (click)="alternarPausa()" [attr.aria-pressed]="pausada()">
          {{ pausada() ? '▶ Reanudar' : 'Ⅱ Pausar' }}
        </button>
        <button type="button" (click)="invertirDireccion()" aria-label="Invertir dirección de la cascada">
          ↕ Invertir
        </button>
      </div>

      <div class="cascada-ventana"
           (mouseenter)="pausarInteraccion()"
           (mouseleave)="reanudarInteraccion()"
           (focusin)="pausarInteraccion()"
           (focusout)="reanudarInteraccion()"
           (touchstart)="iniciarDeslizamiento($event)"
           (touchend)="terminarDeslizamiento($event)">

        @for (columna of columnas; track $index; let i = $index) {
          <div class="cascada-columna" [class.desfasada]="i === 1">
            <div #pista class="cascada-pista" [class.sentido-contrario]="i === 1"
                 [style.--duracion]="(i === 0 ? 24 : 28) + 's'">

              @for (copia of [0, 1]; track copia) {
                <div class="cascada-grupo" [attr.aria-hidden]="copia === 1 ? 'true' : null">
                  @for (producto of columna; track producto.id) {
                    <article class="cascada-tarjeta featured-card">
                      <a class="featured-image" [routerLink]="['/detalle', producto.id]"
                         [attr.tabindex]="copia === 1 ? -1 : null">
                        <img [src]="producto.foto" [alt]="producto.nombre" loading="lazy">
                        @if (producto.etiqueta) {
                          <span class="ribbon">{{ producto.etiqueta }}</span>
                        }
                      </a>
                      <div class="featured-info">
                        <h3>{{ producto.nombre }}</h3>
                        <div class="featured-actions">
                          <span>{{ producto.precio | currency:'MXN':'symbol':'1.0-0' }} MXN</span>
                          <button type="button" (click)="agregar(producto.id)"
                                  [attr.aria-label]="'Agregar ' + producto.nombre + ' al carrito'"
                                  [attr.tabindex]="copia === 1 ? -1 : null">+</button>
                        </div>
                      </div>
                    </article>
                  }
                </div>
              }
            </div>
          </div>
        }
      </div>
      <p class="cascada-ayuda">Pasa el cursor o toca un platillo para detener el movimiento.</p>
    </section>
  `,
  styleUrl: './cascada.component.css'
})
export class CascadaComponent {
  private readonly restaurante = inject(RestauranteService);
  @ViewChildren('pista') private pistas!: QueryList<ElementRef<HTMLElement>>;

  columnas: Producto[][] = [[], []];
  readonly pausada = signal(false);
  private interactuando = false;
  private inicioY: number | null = null;

  @Input({ required: true }) set productos(lista: Producto[]) {
    this.columnas = [
      lista.filter((_, indice) => indice % 2 === 0),
      lista.filter((_, indice) => indice % 2 === 1)
    ];
    if (lista.length === 1) this.columnas[1] = [...this.columnas[0]];
  }

  agregar(id: number): void {
    this.restaurante.cambiarCantidad(id, 1);
  }

  private animaciones(): Animation[] {
    return this.pistas?.toArray().flatMap(p => p.nativeElement.getAnimations()) ?? [];
  }

  private actualizarReproduccion(): void {
    const detener = this.pausada() || this.interactuando;
    for (const animacion of this.animaciones()) {
      if (detener) animacion.pause();
      else animacion.play();
    }
  }

  alternarPausa(): void {
    this.pausada.update(valor => !valor);
    this.actualizarReproduccion();
  }

  invertirDireccion(): void {
    for (const animacion of this.animaciones()) {
      animacion.updatePlaybackRate(-(animacion.playbackRate || 1));
    }
    this.actualizarReproduccion();
  }

  pausarInteraccion(): void {
    this.interactuando = true;
    this.actualizarReproduccion();
  }

  reanudarInteraccion(): void {
    this.interactuando = false;
    this.actualizarReproduccion();
  }

  iniciarDeslizamiento(evento: TouchEvent): void {
    this.inicioY = evento.touches[0]?.clientY ?? null;
    this.pausarInteraccion();
  }

  terminarDeslizamiento(evento: TouchEvent): void {
    const finY = evento.changedTouches[0]?.clientY;
    if (this.inicioY !== null && finY !== undefined && Math.abs(finY - this.inicioY) > 55) {
      this.invertirDireccion();
    }
    this.inicioY = null;
    this.reanudarInteraccion();
  }
}
