package br.com.fiap.pos.soat3.producao.infrastructure.controllers.produto;

public record ProdutoRequest(String nome, String descricao, String imagem, String valor, Long categoriaId) {
}
