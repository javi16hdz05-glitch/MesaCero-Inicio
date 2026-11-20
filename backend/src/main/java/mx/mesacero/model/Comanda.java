package mx.mesacero.model;

import java.util.ArrayList;
import java.util.List;

/** POJO de una comanda (pedido de una mesa). */
public class Comanda {
    private int id;
    private int mesaId;
    private List<ItemComanda> items = new ArrayList<>();
    private double total;
    private String fecha;
    private String estado;

    public Comanda() { }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMesaId() { return mesaId; }
    public void setMesaId(int mesaId) { this.mesaId = mesaId; }
    public List<ItemComanda> getItems() { return items; }
    public void setItems(List<ItemComanda> items) { this.items = items; }
    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
