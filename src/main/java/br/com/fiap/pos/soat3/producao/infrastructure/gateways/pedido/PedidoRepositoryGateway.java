package br.com.fiap.pos.soat3.producao.infrastructure.gateways.pedido;

import br.com.fiap.pos.soat3.producao.application.gateways.PedidoGateway;
import br.com.fiap.pos.soat3.producao.infrastructure.config.exception.EntityNotFoundException;
import br.com.fiap.pos.soat3.producao.domain.entity.Pedido;
import br.com.fiap.pos.soat3.producao.domain.entity.StatusPedido;
import br.com.fiap.pos.soat3.producao.infrastructure.persistence.itempedido.ItemPedidoEntity;
import br.com.fiap.pos.soat3.producao.infrastructure.persistence.itempedido.ItemPedidoRepository;
import br.com.fiap.pos.soat3.producao.infrastructure.persistence.pedido.PedidoEntity;
import br.com.fiap.pos.soat3.producao.infrastructure.persistence.pedido.PedidoRepository;

import java.util.List;
import java.util.stream.Collectors;

public class PedidoRepositoryGateway implements PedidoGateway {

    private final PedidoRepository pedidoRepository;

    private final ItemPedidoRepository itemPedidoRepository;
    private final PedidoEntityMapper pedidoEntityMapper;

    public PedidoRepositoryGateway(PedidoRepository pedidoRepository,
                                   ItemPedidoRepository itemPedidoRepository,
                                   PedidoEntityMapper pedidoEntityMapper) {
        this.pedidoRepository = pedidoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
        this.pedidoEntityMapper = pedidoEntityMapper;
    }

    @Override
    public Pedido cadastraPedido(Pedido pedido) {
        PedidoEntity pedidoEntity = pedidoEntityMapper.toEntity(pedido);
        pedido.setId(pedidoRepository.save(pedidoEntity).getId());
        salvaItemPedido(pedidoEntity);
        return pedido;
    }

    private void salvaItemPedido(PedidoEntity pedidoEntity) {
        for (ItemPedidoEntity itemPedidoEntity : pedidoEntity.getItensPedido()) {
            itemPedidoEntity.setPedido(pedidoEntity);
            itemPedidoRepository.save(itemPedidoEntity);
        }
    }

    @Override
    public List<Pedido> listaPedidos() {
        try {
            var pedidosEntity = pedidoRepository.findByStatusNot(StatusPedido.FINALIZADO.name());
            return pedidosEntity
                    .stream()
                    .map(pedidoEntityMapper::toDomain)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new EntityNotFoundException("Pedido", "");
        }
    }

    @Override
    public String consultaStatusPedido(Long pedidoId) {
        if (pedidoRepository.existsById(pedidoId)) {
            return pedidoRepository.getReferenceById(pedidoId).getStatus();
        } else {
            throw new EntityNotFoundException("Pedido não existe", pedidoId.toString());
        }
    }

    @Override
    public Pedido atualizaStatusPedido(Long pedidoId, String status) {
        try {
            if (pedidoRepository.existsById(pedidoId)) {
                PedidoEntity pedidoEntity = pedidoRepository.getReferenceById(pedidoId);
                pedidoEntity.setStatus(status);
                pedidoRepository.save(pedidoEntity);
                return pedidoEntityMapper.toDomain(pedidoEntity);
            } else {
                throw new EntityNotFoundException("Pedido não existe", pedidoId.toString());
            }
        } catch (Exception e) {
            throw new EntityNotFoundException("Erro ao atualizar status pedido", pedidoId.toString());
        }
    }
}
