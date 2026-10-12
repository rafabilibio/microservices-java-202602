package br.edu.atitus.currencyapi.clients;

import br.edu.atitus.currencyapi.dtos.BcbCotacaoResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface BcbClient {

    @GetExchange("/CotacaoMoedaDia(moeda=@moeda,dataCotacao=@dataCotacao)?@moeda='{moeda}'&@dataCotacao='{dataCotacao}'&$format=json")
    BcbCotacaoResponse getCotacao(
            @PathVariable("moeda") String moeda,
            @PathVariable("dataCotacao") String dataCotacao
    );
}
