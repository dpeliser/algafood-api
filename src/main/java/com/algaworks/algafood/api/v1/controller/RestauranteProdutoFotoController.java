package com.algaworks.algafood.api.v1.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.validation.Valid;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.algaworks.algafood.api.v1.model.FotoProdutoModel;
import com.algaworks.algafood.api.v1.model.input.FotoProdutoInput;
import com.algaworks.algafood.api.v1.openapi.controller.RestauranteProdutoFotoControllerOpenApi;
import com.algaworks.algafood.core.security.CheckSecurity;
import com.algaworks.algafood.domain.exception.EntidadeNaoEncontradaException;
import com.algaworks.algafood.domain.model.FotoProduto;
import com.algaworks.algafood.domain.service.CadastroProdutoService;
import com.algaworks.algafood.domain.service.CatalogoFotoProdutoService;
import com.algaworks.algafood.domain.service.FotoStorageService;
import com.algaworks.algafood.domain.service.FotoStorageService.FotoRecuperada;

@RestController
@RequestMapping(path = "/v1/restaurantes/{restauranteId}/produtos/{produtoId}/foto", produces = MediaType.APPLICATION_JSON_VALUE)
public class RestauranteProdutoFotoController implements RestauranteProdutoFotoControllerOpenApi {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private CatalogoFotoProdutoService service;

    @Autowired
    private CadastroProdutoService cadastroProdutoService;

    @Autowired
    private FotoStorageService fotoStorageService;

    @CheckSecurity.Restaurantes.PodeGerenciarFuncionamento
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FotoProdutoModel upload(@PathVariable Long restauranteId, @PathVariable Long produtoId,
            @Valid FotoProdutoInput dto, @RequestPart(required = true) MultipartFile arquivo) throws IOException {
//        var arquivo = dto.getArquivo();
        var produto = cadastroProdutoService.buscar(restauranteId, produtoId);
        var fotoProduto = new FotoProduto();
        fotoProduto.setProduto(produto);
        fotoProduto.setDescricao(dto.getDescricao());
        fotoProduto.setContentType(arquivo.getContentType());
        fotoProduto.setTamanho(arquivo.getSize());
        fotoProduto.setNomeArquivo(arquivo.getOriginalFilename());

        fotoProduto = service.salvar(fotoProduto, arquivo.getInputStream());

        return modelMapper.map(fotoProduto, FotoProdutoModel.class);
    }

    @CheckSecurity.Restaurantes.PodeConsultar
    @GetMapping
    public FotoProdutoModel buscar(@PathVariable Long restauranteId, @PathVariable Long produtoId) {
        FotoProduto fotoProduto = service.buscar(restauranteId, produtoId);
        return modelMapper.map(fotoProduto, FotoProdutoModel.class);
    }

    @GetMapping(produces = MediaType.ALL_VALUE)
    public ResponseEntity<?> download(@PathVariable Long restauranteId, @PathVariable Long produtoId,
            @RequestHeader(name = "Accept") String acceptHeader) throws HttpMediaTypeNotAcceptableException {
        try {
            FotoProduto fotoProduto = service.buscar(restauranteId, produtoId);

            List<MediaType> mediaTypesAceitas = MediaType.parseMediaTypes(acceptHeader);

            MediaType mediaTypeFoto = MediaType.parseMediaType(fotoProduto.getContentType());

            verificarMediaType(mediaTypeFoto, mediaTypesAceitas);

            FotoRecuperada fotoRecuperada = fotoStorageService.recuperar(fotoProduto.getNomeArquivo());

            if (fotoRecuperada.temUrl()) {
                return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, fotoRecuperada.getUrl())
                        .build();
            }

            InputStream arquivo = fotoRecuperada.getArquivo();
            return ResponseEntity.ok().contentType(mediaTypeFoto).body(new InputStreamResource(arquivo));
        } catch (EntidadeNaoEncontradaException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private void verificarMediaType(MediaType mediaType, List<MediaType> mediaTypesAceitas)
            throws HttpMediaTypeNotAcceptableException {
        boolean compativel = mediaTypesAceitas.stream()
                .anyMatch(mediaTypeAceita -> mediaTypeAceita.isCompatibleWith(mediaType));
        if (!compativel) {
            throw new HttpMediaTypeNotAcceptableException(mediaTypesAceitas);
        }
    }

    @CheckSecurity.Restaurantes.PodeGerenciarFuncionamento
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long restauranteId, @PathVariable Long produtoId) {
        service.excluir(restauranteId, produtoId);
    }

}
