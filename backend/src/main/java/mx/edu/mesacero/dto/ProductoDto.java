package mx.edu.mesacero.dto;

import java.math.BigDecimal;

/** POJO / DTO: respuesta pública de un producto sin campos administrativos. */
public final class ProductoDto {
    private final long id;
    private final String nombre;
    private final String categoria;
    private final BigDecimal precio;
    private final boolean disponible;
    private final String descripcion;
    private final String foto;
    private final String etiqueta;

    public ProductoDto(long id, String nombre, String categoria, BigDecimal precio,
                       boolean disponible, String descripcion, String foto, String etiqueta) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.precio = precio;
        this.disponible = disponible;
        this.descripcion = descripcion;
        this.foto = foto;
        this.etiqueta = etiqueta;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public BigDecimal getPrecio() { return precio; }
    public boolean isDisponible() { return disponible; }
    public String getDescripcion() { return descripcion; }
    public String getFoto() { return foto; }
    public String getEtiqueta() { return etiqueta; }
}
