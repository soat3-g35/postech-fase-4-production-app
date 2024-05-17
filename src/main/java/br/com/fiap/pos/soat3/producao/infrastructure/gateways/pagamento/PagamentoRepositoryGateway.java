package br.com.fiap.pos.soat3.producao.infrastructure.gateways.pagamento;

import br.com.fiap.pos.soat3.producao.application.gateways.PagamentoGateway;
import br.com.fiap.pos.soat3.producao.application.gateways.RealizaPagamentoMockGateway;
import br.com.fiap.pos.soat3.producao.domain.entity.ItemPedido;
import br.com.fiap.pos.soat3.producao.domain.entity.Pagamento;
import br.com.fiap.pos.soat3.producao.domain.entity.Pedido;
import br.com.fiap.pos.soat3.producao.domain.entity.Produto;
import br.com.fiap.pos.soat3.producao.domain.entity.StatusPedido;
import br.com.fiap.pos.soat3.producao.infrastructure.gateways.pedido.PedidoRepositoryGateway;
import br.com.fiap.pos.soat3.producao.infrastructure.gateways.produto.ProdutoRepositoryGateway;
import br.com.fiap.pos.soat3.producao.infrastructure.integration.MVPResponse;
import br.com.fiap.pos.soat3.producao.infrastructure.persistence.pagamento.PagamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

public class PagamentoRepositoryGateway implements PagamentoGateway {

    private final Logger log = LoggerFactory.getLogger(PagamentoRepositoryGateway.class);
    private final PagamentoRepository pagamentoRepository;
    private final PedidoRepositoryGateway pedidoRepositoryGateway;
    private final ProdutoRepositoryGateway produtoRepositoryGateway;
    private final PagamentoEntityMapper pagamentoEntityMapper;
    private final RealizaPagamentoMockGateway realizaPagamentoMockGateway;

    public PagamentoRepositoryGateway(final PagamentoRepository pagamentoRepository,
                                      final PedidoRepositoryGateway pedidoRepositoryGateway,
                                      final ProdutoRepositoryGateway produtoRepositoryGateway,
                                      final PagamentoEntityMapper pagamentoEntityMapper,
                                      final RealizaPagamentoMockGateway realizaPagamentoMockGateway) {
        this.pagamentoRepository = pagamentoRepository;
        this.pedidoRepositoryGateway = pedidoRepositoryGateway;
        this.produtoRepositoryGateway = produtoRepositoryGateway;
        this.pagamentoEntityMapper = pagamentoEntityMapper;
        this.realizaPagamentoMockGateway = realizaPagamentoMockGateway;

    }

    @Override
    public Pagamento realizaPagamento(Pagamento pagamento) {

        Pedido pedido = pagamento.getPedido();
        pedido.setStatus(StatusPedido.GERADO);
        pedido.setTotalPedido(getTotal(pedido.getItensPedido()));

        pagamento.setPedido(pedidoRepositoryGateway.cadastraPedido(pedido));

        MVPResponse response = realizaPagamentoMockGateway.realizaPagamentoMVP(pagamento.getPedido().getId(), pagamento.getId());
        pagamento.setQrCode(response.getCode());
        pagamento.setWebhook(response.getUrl());

        var pagamentoEntity = pagamentoEntityMapper.toEntity(pagamento);
        pagamento.setId(pagamentoRepository.save(pagamentoEntity).getId());

        pagamento.getPedido().setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        log.info(String.format("Lanchonete: Pagamento do pedido  %s gerado", pagamento.getPedido().getId()));
        return pagamento;
    }

    private String getTotal(List<ItemPedido> itensPedido) {
        BigDecimal total = BigDecimal.ZERO;

        for (ItemPedido itemPedido : itensPedido) {
            Produto produto = produtoRepositoryGateway.buscaProduto(itemPedido.getProduto().getId());
            String valorUnitario = String.valueOf(produto.getValor());
            BigDecimal valor = new BigDecimal(valorUnitario.replaceAll("\\.", "").replace(",", "."));

            BigDecimal multiplicador = new BigDecimal(itemPedido.getQuantidade());

            total = total.add(valor.multiply(multiplicador));
        }
        return String.valueOf(total);
    }
}
