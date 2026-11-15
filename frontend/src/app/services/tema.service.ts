import { Injectable, signal } from '@angular/core';
@Injectable({providedIn:'root'})
export class TemaService {
  readonly claro=signal(false);
  alternar():void {this.claro.update(v=>!v);}
}
