package br.com.jence.backend.domain.entity;

import lombok.Getter;

import java.util.UUID;

@Getter
public class PontoMapa {

    private final UUID id;
    private final TipoPonto tipo;
    private final String corredor;
    private final int coordenadaX;
    private final int coordenadaY;

    /**
     * Codigo digitavel do adesivo, na forma canonica. Nulo em todo ponto que nao seja
     * {@link TipoPonto#QR_CODE}.
     */
    private final String codigoCurto;

    public PontoMapa(UUID id, TipoPonto tipo, String corredor, int coordenadaX, int coordenadaY) {
        this(id, tipo, corredor, coordenadaX, coordenadaY, null);
    }

    public PontoMapa(UUID id, TipoPonto tipo, String corredor, int coordenadaX, int coordenadaY,
                     String codigoCurto) {
        this.id = id;
        this.tipo = tipo;
        this.corredor = corredor;
        this.coordenadaX = coordenadaX;
        this.coordenadaY = coordenadaY;
        // Normaliza na entrada, e nao so na consulta: assim o banco nunca guarda duas grafias
        // do mesmo codigo, e a unicidade da coluna significa de fato unicidade do codigo.
        this.codigoCurto = normalizarCodigo(codigoCurto);
    }

    /**
     * Reduz um codigo a sua forma canonica: maiusculas, sem separadores.
     * <p>
     * O codigo curto existe para ser digitado por alguem em pe num corredor, com o adesivo
     * rasgado ou a camera falhando. Se {@code tin-04}, {@code TIN 04} e {@code tin04} nao
     * levarem ao mesmo ponto, o plano B falha na primeira tentativa e o cliente desiste - que
     * e exatamente o caso que ele deveria resolver.
     *
     * @return a forma canonica, ou {@code null} se a entrada for nula ou nao sobrar caractere
     */
    public static String normalizarCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        String canonico = codigo.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return canonico.isEmpty() ? null : canonico;
    }

    public double calcularDistanciaPara(PontoMapa outro) {
        int deltaX = this.coordenadaX - outro.coordenadaX;
        int deltaY = this.coordenadaY - outro.coordenadaY;
        return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
    }
}
