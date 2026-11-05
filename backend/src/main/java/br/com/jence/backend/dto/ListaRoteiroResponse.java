package br.com.jence.backend.dto;

import br.com.jence.backend.model.ListaRoteiro;

import java.util.List;
import java.util.UUID;

/** Espelha o schema {@code ListaRoteiroResponse} do contrato OpenAPI. */
public class ListaRoteiroResponse {

    private UUID id;
    private UUID sessaoId;
    private int quantidadeItens;
    private List<ItemRoteiroDetalhadoResponse> itens;

    public ListaRoteiroResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSessaoId() {
        return sessaoId;
    }

    public void setSessaoId(UUID sessaoId) {
        this.sessaoId = sessaoId;
    }

    public int getQuantidadeItens() {
        return quantidadeItens;
    }

    public void setQuantidadeItens(int quantidadeItens) {
        this.quantidadeItens = quantidadeItens;
    }

    public List<ItemRoteiroDetalhadoResponse> getItens() {
        return itens;
    }

    public void setItens(List<ItemRoteiroDetalhadoResponse> itens) {
        this.itens = itens;
    }

    public static ListaRoteiroResponse de(ListaRoteiro lista) {
        List<ItemRoteiroDetalhadoResponse> itens = lista.getItensParaExibicao().stream()
                .map(ItemRoteiroDetalhadoResponse::de)
                .toList();

        ListaRoteiroResponse resposta = new ListaRoteiroResponse();
        resposta.setId(lista.getId());
        resposta.setSessaoId(lista.getSessaoId());
        resposta.setQuantidadeItens(itens.size());
        resposta.setItens(itens);
        return resposta;
    }
}
