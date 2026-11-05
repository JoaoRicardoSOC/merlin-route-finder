package br.com.jence.backend;

import br.com.jence.backend.config.Configuracao;
import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.PontoMapaDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dao.RestricaoDeEnumDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.scheduler.AgendadorDeExpiracao;
import br.com.jence.backend.seed.CarregadorDadosIniciais;
import br.com.jence.backend.service.ExpirarSessoesInativasUseCase;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.TimeZone;

/**
 * Sobe a aplicacao: carrega a massa de dados, liga o agendador e abre o servidor HTTP.
 * <p>
 * Segue a {@code Main} do material, com tres diferencas:
 * <ul>
 *   <li>escuta em {@code 0.0.0.0}, e nao em {@code localhost}: dentro do container,
 *       {@code localhost} so aceita conexoes do proprio container, e o Render nao chegaria;</li>
 *   <li>a porta vem da variavel {@code PORT}, que o Render define;</li>
 *   <li>nao espera o {@code System.in.read()}: no container nao ha teclado, a leitura
 *       terminaria na hora e o servidor cairia logo depois de subir. A thread principal
 *       espera para sempre, e o desligamento vem do sinal do sistema.</li>
 * </ul>
 */
public class Main {

    /** Onde estao os Resources que o Jersey registra. */
    private static final String PACOTE_DOS_RESOURCES = "br.com.jence.backend.resource";

    /**
     * Abre o servidor na porta dada, com os Resources do pacote {@code resource}. Os testes
     * tambem sobem a aplicacao por aqui.
     */
    public static HttpServer startServer(int porta) {
        final ResourceConfig rc = new ResourceConfig().packages(PACOTE_DOS_RESOURCES);
        URI endereco = URI.create("http://0.0.0.0:" + porta + "/");
        return GrizzlyHttpServerFactory.createHttpServer(endereco, rc);
    }

    public static void main(String[] args) throws Exception {
        // Datas gravadas e lidas sempre em UTC, qualquer que seja o fuso da maquina.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        Configuracao configuracao = Configuracao.doAmbiente();

        /*
         * A carga vem antes do servidor: assim nenhuma requisicao encontra o catalogo pela
         * metade. Ela tambem refaz as restricoes de enum do banco, na ordem que precisa (ver
         * CarregadorDadosIniciais).
         */
        if (configuracao.isCargaDeDadosLigada()) {
            new CarregadorDadosIniciais(new ProdutoDao(), new PontoMapaDao(), new RestricaoDeEnumDao())
                    .executar();
        }

        final AgendadorDeExpiracao agendador = new AgendadorDeExpiracao(
                new ExpirarSessoesInativasUseCase(new SessaoDao(), new ListaRoteiroDao()));
        if (configuracao.isAgendadorLigado()) {
            agendador.iniciar(configuracao.getAgendadorAtrasoInicialMs(),
                    configuracao.getAgendadorIntervaloMs());
        }

        final HttpServer server = startServer(configuracao.getPorta());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Desligando o servidor...");
            server.shutdownNow();
            agendador.parar();
        }));

        System.out.println(String.format("Servidor no ar na porta %s.", configuracao.getPorta()));
        Thread.currentThread().join();
    }
}
