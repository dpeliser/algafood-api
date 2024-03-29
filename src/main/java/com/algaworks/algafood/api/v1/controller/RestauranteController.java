package com.algaworks.algafood.api.v1.controller;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.algaworks.algafood.api.v1.assembler.RestauranteApenasNomeModelAssembler;
import com.algaworks.algafood.api.v1.assembler.RestauranteBasicoModelAssembler;
import com.algaworks.algafood.api.v1.model.RestauranteApenasNomeModel;
import com.algaworks.algafood.api.v1.model.RestauranteBasicoModel;
import com.algaworks.algafood.api.v1.model.RestauranteModel;
import com.algaworks.algafood.api.v1.model.input.RestauranteInput;
import com.algaworks.algafood.api.v1.openapi.controller.RestauranteControllerOpenApi;
import com.algaworks.algafood.core.security.CheckSecurity;
import com.algaworks.algafood.domain.exception.CidadeNaoEncontradaException;
import com.algaworks.algafood.domain.exception.CozinhaNaoEncontradaException;
import com.algaworks.algafood.domain.exception.NegocioException;
import com.algaworks.algafood.domain.exception.RestauranteNaoEncontradoException;
import com.algaworks.algafood.domain.model.Cidade;
import com.algaworks.algafood.domain.model.Cozinha;
import com.algaworks.algafood.domain.model.Restaurante;
import com.algaworks.algafood.domain.repository.RestauranteRepository;
import com.algaworks.algafood.domain.service.CadastroRestauranteService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

//@CrossOrigin(origins = "http://www.algafood.local:8000")
//@CrossOrigin(maxAge = 10)
@RestController
@RequestMapping("/v1/restaurantes")
public class RestauranteController implements RestauranteControllerOpenApi {

    @Autowired
    private RestauranteRepository repository;

    @Autowired
    private CadastroRestauranteService service;

    @Autowired
    private SmartValidator validator;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private RestauranteBasicoModelAssembler restauranteBasicoModelAssembler;

    @Autowired
    private RestauranteApenasNomeModelAssembler restauranteApenasNomeModelAssembler;

//    @GetMapping
//    public MappingJacksonValue listar(@RequestParam(required = false) String projecao) {
//        List<Restaurante> restaurantes = repository.findAll();
//        List<RestauranteDTO> restaurantesDtos = restaurantes.stream()
//                .map(entity -> modelMapper.map(entity, RestauranteDTO.class)).collect(Collectors.toList());
//        MappingJacksonValue wrapper = new MappingJacksonValue(restaurantesDtos);
//        if ("apenas-nome".equals(projecao)) {
//            wrapper.setSerializationView(RestauranteView.ApenasNome.class);
//        } else if ("completo".equals(projecao)) {
//            wrapper.setSerializationView(null);
//        } else {
//            wrapper.setSerializationView(RestauranteView.Resumo.class);
//        }
//        return wrapper;
//    }

//    @GetMapping
//    public ResponseEntity<List<RestauranteDTO>> listar() {
//        final List<RestauranteDTO> list = repository.findAll().stream()
//                .map(entity -> modelMapper.map(entity, RestauranteDTO.class)).collect(Collectors.toList());
//        return ResponseEntity.ok().header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://www.algafood.local:8000")
//                .body(list);
//    }

    @CheckSecurity.Restaurantes.PodeConsultar
    @Override
//  @JsonView(RestauranteView.Resumo.class)
    @GetMapping
    public CollectionModel<RestauranteBasicoModel> listar() {
        return restauranteBasicoModelAssembler.toCollectionModel(repository.findAll());
    }

//    @JsonView(RestauranteView.Resumo.class)
//    @GetMapping(params = "projecao=resumo")
//    public List<RestauranteDTO> listarResumido() {
//        return listar();
//    }

    @CheckSecurity.Restaurantes.PodeConsultar
    @Override
//  @JsonView(RestauranteView.ApenasNome.class)
    @GetMapping(params = "projecao=apenas-nome")
    public CollectionModel<RestauranteApenasNomeModel> listarApenasNomes() {
        return restauranteApenasNomeModelAssembler.toCollectionModel(repository.findAll());
    }

    @CheckSecurity.Restaurantes.PodeConsultar
    @GetMapping("/{id}")
    public RestauranteModel buscar(@PathVariable Long id) {
        final Restaurante restaurante = service.buscar(id);
        return modelMapper.map(restaurante, RestauranteModel.class);
    }

    @CheckSecurity.Restaurantes.PodeGerenciarCadastro
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestauranteModel adicionar(@RequestBody @Valid RestauranteInput entityDto) {
        try {
            final Restaurante entity = modelMapper.map(entityDto, Restaurante.class);
            return modelMapper.map(service.salvar(entity), RestauranteModel.class);
        } catch (CozinhaNaoEncontradaException | CidadeNaoEncontradaException e) {
            throw new NegocioException(e.getMessage(), e);
        }
    }

    @CheckSecurity.Restaurantes.PodeGerenciarCadastro
    @PutMapping("/{id}")
    public RestauranteModel atualizar(@PathVariable Long id, @RequestBody @Valid RestauranteInput entity) {
        Restaurante restaurante = service.buscar(id);
        // Evitar: org.springframework.orm.jpa.JpaSystemException: identifier of an
        // instance of com.algaworks.algafood.domain.model.Cozinha was altered from 1 to
        // 2;
        restaurante.setCozinha(new Cozinha());
        restaurante.getEndereco().setCidade(new Cidade());
        modelMapper.map(entity, restaurante);
//        BeanUtils.copyProperties(entity, restaurante, "id", "formasPagamento", "endereco", "dataCadastro", "produtos");
        try {
            return modelMapper.map(service.salvar(restaurante), RestauranteModel.class);
        } catch (CozinhaNaoEncontradaException | CidadeNaoEncontradaException e) {
            throw new NegocioException(e.getMessage(), e);
        }
    }

    @CheckSecurity.Restaurantes.PodeGerenciarCadastro
    @PatchMapping("/{id}")
    public RestauranteModel atualizarParcial(@PathVariable Long id, @RequestBody Map<String, Object> campos,
            HttpServletRequest request) {
        Restaurante restaurante = service.buscar(id);
        merge(campos, restaurante, request);
        validate(restaurante, "restaurante");
        return atualizar(id, modelMapper.map(restaurante, RestauranteInput.class));
    }

    @CheckSecurity.Restaurantes.PodeGerenciarCadastro
    @Override
    @PutMapping("/{restauranteId}/ativo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> ativar(@PathVariable Long restauranteId) {
        service.ativar(restauranteId);

        return ResponseEntity.noContent().build();
    }

    @CheckSecurity.Restaurantes.PodeGerenciarCadastro
    @PutMapping("/ativacoes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ativar(@RequestBody List<Long> ids) {
        try {
            service.ativar(ids);
        } catch (RestauranteNaoEncontradoException e) {
            throw new NegocioException(e.getMessage(), e);
        }
    }

    @CheckSecurity.Restaurantes.PodeGerenciarCadastro
    @Override
    @DeleteMapping("/{restauranteId}/ativo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> inativar(@PathVariable Long restauranteId) {
        service.inativar(restauranteId);

        return ResponseEntity.noContent().build();
    }

    @CheckSecurity.Restaurantes.PodeGerenciarCadastro
    @DeleteMapping("/ativacoes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@RequestBody List<Long> ids) {
        try {
            service.inativar(ids);
        } catch (RestauranteNaoEncontradoException e) {
            throw new NegocioException(e.getMessage(), e);
        }
    }

    @CheckSecurity.Restaurantes.PodeGerenciarFuncionamento
    @Override
    @PutMapping("/{restauranteId}/abertura")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> abrir(@PathVariable Long restauranteId) {
        service.abrir(restauranteId);

        return ResponseEntity.noContent().build();
    }

    @CheckSecurity.Restaurantes.PodeGerenciarFuncionamento
    @Override
    @PutMapping("/{restauranteId}/fechamento")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> fechar(@PathVariable Long restauranteId) {
        service.fechar(restauranteId);

        return ResponseEntity.noContent().build();
    }

    private void validate(Restaurante restaurante, String objectName) {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(restaurante, objectName);
        validator.validate(repository, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidacaoException(bindingResult);
        }
    }

    private void merge(Map<String, Object> camposOrigem, Restaurante restauranteDestino, HttpServletRequest request) {
        try {
            ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, true)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            Restaurante restauranteOrigem = mapper.convertValue(camposOrigem, Restaurante.class);
            camposOrigem.forEach((campo, valor) -> {
                Field field = ReflectionUtils.findField(Restaurante.class, campo);
                field.setAccessible(true);
                Object novoValor = ReflectionUtils.getField(field, restauranteOrigem);
                ReflectionUtils.setField(field, restauranteDestino, novoValor);
            });
        } catch (IllegalArgumentException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            ServletServerHttpRequest servletServerHttpRequest = new ServletServerHttpRequest(request);
            throw new HttpMessageNotReadableException(e.getMessage(), rootCause, servletServerHttpRequest);
        }
    }

}
