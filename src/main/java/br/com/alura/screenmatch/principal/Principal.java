package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.ISerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import javax.swing.*;
import javax.swing.text.html.Option;
import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = System.getenv("API_KEY_OMDB");

    private List<DadosSerie> dadosSeries = new ArrayList<>();
    private ISerieRepository repositorio;
    private List<Serie> series = new ArrayList<>();
    private Optional<Serie> serieBusca;


    public Principal(ISerieRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void exibeMenu() {
        var opcao = -1;
        while (opcao != 0) {
            var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar series buscadas
                    4 - Buscar serie pelo titulo
                    5 - Buscar serie por ator
                    6 - Top 5 series
                    7 - Buscar series por categoria
                    8 - Buscar series por numero maximo de temporadas e avaliacao
                    9 - Buscar episodio pelo trecho
                    10 - Buscar top episodios por serie
                    11 - Buscar episodio a partir de uma data
                                    
                    0 - Sair                                 
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarSeriePorCategoria();
                    break;
                case 8:
                    buscarSeriePorTemporadaEavaliacao();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosPorData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }




    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        //dadosSeries.add(dados);
        repositorio.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscas();
        System.out.println("Escolha uma serie pelo nome: ");
        var nomeSerie = leitura.nextLine();

        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serie.isPresent()) {
            var serieEncontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodioList(episodios);
            repositorio.save(serieEncontrada);
        }
        else{
            System.out.println("Serie nao encontrada!");
        }
    }

    private void listarSeriesBuscas(){
        series = repositorio.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma serie pelo nome: ");
        var nomeSerie = leitura.nextLine();

        serieBusca = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if(serieBusca.isPresent()){
            System.out.println("Dados da serie: " + serieBusca.get());
        } else{
            System.out.println("Serie nao encontrada!");
        }
    }

    private void buscarSeriePorAtor() {
        System.out.println("Qual nome do ator para busca? ");
        var nomeAtor = leitura.nextLine();
        System.out.println("Avaliacoes a partir de qual valor? ");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesEncontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);
        System.out.println("Series em que " + nomeAtor + " trabalhou");
        seriesEncontradas.forEach(s ->
                System.out.println(s.getTitulo() + ", avaliacao: " + s.getAvaliacao()));
    }
    private void buscarTop5Series(){
        List<Serie> seriesTop = repositorio.findTop5ByOrderByAvaliacaoDesc();
        seriesTop.forEach(s ->
                System.out.println(s.getTitulo() + ", avaliacao: " + s.getAvaliacao()));
    }
    private void buscarSeriePorCategoria(){
        System.out.println("Deseja buscar series de que categorias/genero");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesCategoria = repositorio.findByGenero(categoria);
        System.out.println("Series da categoria: " + categoria);
        seriesCategoria.forEach(s ->
                System.out.println(s.getTitulo() + ", avaliacao: " + s.getAvaliacao()));
    }
    private void buscarSeriePorTemporadaEavaliacao(){
        System.out.println("Digite a quantidade maxima de temporadas que uma serie deve ter: ");
        var qntTemp = leitura.nextInt();
        System.out.println("Agora a avaliacao minima das series: ");
        var avaliacao = leitura.nextDouble();
        List<Serie> seriesPorTemporadaAvaliacao = repositorio.seriePorTemporadaEAvaliacao(qntTemp, avaliacao);
        System.out.println("Series filtradas: ");
        seriesPorTemporadaAvaliacao.forEach(s->
                System.out.println(s.getTitulo() + ", avaliacao: " + s.getAvaliacao() + ", Total de Temporadas: " + s.getTotalTemporadas()));
    }
    private void buscarEpisodioPorTrecho(){
        System.out.println("Qual nome do episodio para busca: ");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodiosAchados = repositorio.episodiosPorTrecho(trechoEpisodio);
        episodiosAchados.forEach(e ->
                System.out.printf("Serie: %s Temporada %s - Episodio %s - %s\n",
                        e.getSerie().getTitulo(), e.getTemporada(),
                        e.getNumeroEpisodio(), e.getTitulo()));
    }
    private void topEpisodiosPorSerie(){
        buscarSeriePorTitulo();
        if(serieBusca.isPresent()){
           Serie serie = serieBusca.get();
           List<Episodio> topEpisodios = repositorio.topEpisodiosPorSerie(serie);
           topEpisodios.forEach(e ->
                   System.out.printf("Serie: %s Temporada %s - Episodio %s - %s Avaliacao %s\n",
                           e.getSerie().getTitulo(), e.getTemporada(),
                           e.getNumeroEpisodio(), e.getTitulo(), e.getAvaliacao()));
        }
    }
    private void buscarEpisodiosPorData(){
        buscarSeriePorTitulo();
        if(serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            System.out.println("Digite o ano limite de lancamento do episodio: ");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();
            List<Episodio> episodiosAno = repositorio.episodiosPorSerieEAno(serie, anoLancamento);
            episodiosAno.forEach(e ->
                    System.out.printf("Serie: %s Temporada %s - Episodio %s - %s Ano de lancamento %s\n",
                            e.getSerie().getTitulo(), e.getTemporada(),
                            e.getNumeroEpisodio(), e.getTitulo(), e.getDataLancamento().getYear()));
        }
    }
}
