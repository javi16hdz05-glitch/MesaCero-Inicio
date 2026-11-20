package mx.mesacero.model;

/** POJO de una reservación. fecha = yyyy-MM-dd, hora = HH:mm. */
public class Reserva {
    private int id;
    private int mesaId;
    private String fecha;
    private String hora;
    private int personas;
    private String zona;

    public Reserva() { }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMesaId() { return mesaId; }
    public void setMesaId(int mesaId) { this.mesaId = mesaId; }
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }
    public String getHora() { return hora; }
    public void setHora(String hora) { this.hora = hora; }
    public int getPersonas() { return personas; }
    public void setPersonas(int personas) { this.personas = personas; }
    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }
}
