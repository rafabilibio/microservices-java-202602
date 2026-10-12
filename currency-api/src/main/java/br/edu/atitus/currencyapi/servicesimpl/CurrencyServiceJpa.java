package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import br.edu.atitus.currencyapi.services.CurrencyService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CurrencyServiceJpa implements CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyServiceJpa.class);

    private final CurrencyRepository repository;
    private final BcbClientService bcbClientService;

    public CurrencyServiceJpa(
            CurrencyRepository repository,
            BcbClientService bcbClientService) {
        this.repository = repository;
        this.bcbClientService = bcbClientService;
    }

    @Value("${server.port:8100}")
    private String serverPort;

    // Data fixa (MM-dd-yyyy) de um dia util com cotacao no BCB
    @Value("${app.bcb.data-cotacao:01-02-2024}")
    private String dataCotacao;

    @Override
    public CurrencyResponse findBySourceCurrencyAndTargetCurrency(String sourceCurrency, String targetCurrency) throws Exception {
        String source = sourceCurrency.toUpperCase();
        String target = targetCurrency.toUpperCase();
        String environment = "Currency API running in Port: " + serverPort;

        // 1) Tenta a API externa (Banco Central)
        Double rate = getRateFromBcb(source, target);
        if (rate != null) {
            log.info("Cotacao {}->{} obtida do BCB: {}", source, target, rate);
            return new CurrencyResponse(source, target, rate, environment);
        }

        // 2) Fallback: banco de dados local (falha externa OU ausencia de cotacao)
        log.info("Usando cotacao do banco local para {}->{}", source, target);
        var currency = repository.findBySourceCurrencyAndTargetCurrency(source, target)
                .orElseThrow(() -> new EntityNotFoundException("Cotação não encontrada"));

        return CurrencyResponse.fromEntity(currency, environment);
    }

    /**
     * O BCB informa quanto vale 1 unidade da moeda em BRL.
     * Para qualquer par, taxa = (origem em BRL) / (destino em BRL).
     * Retorna null se o BCB nao tiver cotacao ou se a consulta falhar.
     */
    private Double getRateFromBcb(String source, String target) {
        try {
            Double sourceInBrl = getRateInBrl(source);
            Double targetInBrl = getRateInBrl(target);
            if (sourceInBrl == null || targetInBrl == null || targetInBrl == 0) {
                return null;
            }
            return sourceInBrl / targetInBrl;
        } catch (Exception e) {
            log.warn("Falha ao consultar o BCB para {}->{}: {}", source, target, e.toString());
            return null;
        }
    }

    private Double getRateInBrl(String currency) {
        if ("BRL".equals(currency)) {
            return 1.0;
        }
        return bcbClientService.getCotacaoVenda(currency, dataCotacao);
    }
}
