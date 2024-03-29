package com.algaworks.algafood.domain.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.algaworks.algafood.domain.event.PedidoConfirmadoEvent;
import com.algaworks.algafood.domain.service.EnvioEmailService;
import com.algaworks.algafood.domain.service.EnvioEmailService.Mensagem;

@Component
public class NotificacaoClientePedidoConfirmadoListener {

    @Autowired
    private EnvioEmailService envioEmailService;

    // @EventListener
    @TransactionalEventListener
    public void pedidoConfirmato(PedidoConfirmadoEvent event) {
        var pedido = event.getPedido();
        var mensagem = Mensagem.builder().assunto(pedido.getRestaurante().getNome() + " - Pedido confirmado")
                .corpo("emails/pedido-confirmado.html").variavel("pedido", pedido).destinatario(pedido.getCliente().getEmail())
                .build();

        envioEmailService.enviar(mensagem);
    }

}
