package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.clients.BcbClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class BcbClientService {

    private static final Logger log = LoggerFactory.getLogger(BcbClientService.class);

    private final BcbClient bcbClient;

    public BcbClientService(BcbClient bcbClient) {
        this.bcbClient = bcbClient;
    }

    /**
     * Retorna a cotacao de venda (em BRL) da moeda na data informada,
     * ou null quando o BCB responde sem cotacao.
     * Resultado null nao e armazenado no cache (unless).
     * Falhas de comunicacao lancam excecao (nao sao cacheadas).
     */
    @Cacheable(
            value = "bcb-api",
            key = "#moeda + '-' + #dataCotacao",
            unless = "#result == null"
    )
    @Retry(name = "bcb-api")
    @CircuitBreaker(name = "bcb-api")
    public Double getCotacaoVenda(String moeda, String dataCotacao) {
        log.info("Consultando API do BCB: moeda={} dataCotacao={}", moeda, dataCotacao);

        var response = bcbClient.getCotacao(moeda, dataCotacao);

        if (response == null || response.value() == null || response.value().isEmpty()) {
            log.info("BCB respondeu sem cotacao: moeda={} dataCotacao={}", moeda, dataCotacao);
            return null;
        }

        // Se houver mais de um boletim no dia, usa o ultimo (fechamento)
        var cotacoes = response.value();
        return cotacoes.get(cotacoes.size() - 1).cotacaoVenda();
    }
}
