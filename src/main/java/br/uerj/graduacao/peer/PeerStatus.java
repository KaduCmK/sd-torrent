package br.uerj.graduacao.peer;

public enum PeerStatus {
    CONECTANDO("Conectando..."),
    BAIXANDO("Baixando"),
    VERIFICANDO("Verificando..."),
    SEMEANDO("Semeando"),
    CORROMPIDO("Corrompido!");

    private final String descricao;

    PeerStatus(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return this.descricao;
    }
}
