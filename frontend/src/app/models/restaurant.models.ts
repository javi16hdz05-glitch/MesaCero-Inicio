export type EstadoMesa = 'disponible' | 'ocupada' | 'reservada';

export interface Mesa {
  id: number;
  numero: number;
  capacidad: number;
  estado: EstadoMesa;
}

export interface Producto {
  id: number;
  nombre: string;
  categoria: string;
  precio: number;
  disponible: boolean;
}

export interface ItemComanda {
  productoId: number;
  cantidad: number;
}

export interface Comanda {
  id: number;
  mesaId: number;
  items: ItemComanda[];
  total: number;
  fecha: string;
  estado: 'pendiente' | 'en_preparacion';
}
