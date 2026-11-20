package mx.mesacero.model;

/** Un renglón de la comanda: qué producto y cuántos. */
public class ItemComanda {
    private int productoId;
    private int cantidad;

    public ItemComanda() { }

    public ItemComanda(int productoId, int cantidad) {
        this.productoId = productoId;
        this.cantidad = cantidad;
    }

    public int getProductoId() { return productoId; }
    public void setProductoId(int productoId) { this.productoId = productoId; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
