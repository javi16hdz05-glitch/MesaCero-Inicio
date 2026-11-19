package mx.edu.mesacero.dto;

/** POJO / DTO: estado operativo de una mesa para la UI. */
public final class MesaEstadoDto {
    private final long id;
    private final int numero;
    private final int capacidad;
    private final String zona;
    private final boolean fueraServicio;
    private final String estado;

    public MesaEstadoDto(long id, int numero, int capacidad, String zona,
                         boolean fueraServicio, String estado) {
        this.id = id;
        this.numero = numero;
        this.capacidad = capacidad;
        this.zona = zona;
        this.fueraServicio = fueraServicio;
        this.estado = estado;
    }

    public long getId() { return id; }
    public int getNumero() { return numero; }
    public int getCapacidad() { return capacidad; }
    public String getZona() { return zona; }
    public boolean isFueraServicio() { return fueraServicio; }
    public String getEstado() { return estado; }
}
