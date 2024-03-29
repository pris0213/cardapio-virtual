package facin.com.cardapio_virtual;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import facin.com.cardapio_virtual.auxiliares.FiltroInterface;
import facin.com.cardapio_virtual.auxiliares.Nodo;
import facin.com.cardapio_virtual.auxiliares.Restricao;
import facin.com.cardapio_virtual.auxiliares.SimpleDividerItemDecoration;
import facin.com.cardapio_virtual.auxiliares.VerificadorDeRestricao;
import facin.com.cardapio_virtual.data.DatabaseContract;

import static com.hp.hpl.jena.ontology.OntModelSpec.OWL_MEM;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class ProductFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;
    private static List<Product> produtos = new ArrayList<>();
    private List<Product> produtosDoFragmento;
    private List<Product> produtosAExibir;

    // Filtros e Ordem
    private static List<FiltroInterface> filtros = new ArrayList<>();
    private static int ordem = 0;

    private RecyclerView recyclerView;
    private ProgressDialog progressDialog;

    // Ontology
    private static OntModel ontModel;
    private File lanchesFile;
    private File fileDir;
    private static String fileName;
    private List<Individual> individuos;

    // Controles (flags)
    static boolean ontologiaLida = false;
    static boolean bancoInicializado = false;
    static boolean ontologiaInicializada = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ProductFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static ProductFragment newInstance(int columnCount) {
        ProductFragment fragment = new ProductFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileDir = getActivity().getApplicationContext().getFilesDir();
        individuos = null;
        produtosDoFragmento = new ArrayList<>();
        // criaArquivoMetodo2();
        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    public void configuraProdutosAExibir() {
        produtosAExibir = ordenaProdutos(filtraProdutos(produtosDoFragmento));
    }

    protected void criaArquivoMetodo2() {
        try {

            String content = criaFileName().toString();
            FileOutputStream outputLanches;
            outputLanches = getActivity().openFileOutput(fileName, Context.MODE_PRIVATE);
            outputLanches.write(content.getBytes());
            outputLanches.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected InputStream criaFileName() {
        try {
            for (String nomeArquivo : getActivity().getApplicationContext().getAssets().list("")) {
                if (nomeArquivo.contains(MenuActivity.nomeRestauranteArquivo)) {
                    fileName = nomeArquivo;
                }
            }
            InputStream content = getActivity().getApplicationContext().getAssets().open(fileName);
            return content;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            recyclerView.addItemDecoration(new SimpleDividerItemDecoration(getContext()));
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            progressDialog = ProgressDialog.show(getActivity(), getResources().getText(R.string.progress_dialog_product_title),
                    getResources().getText(R.string.progress_dialog_product_message), true, false);
            Log.d("2", "Chamando FetchOntologyTask");
            new FetchOntologyTask().execute((Void) null);
        }
        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        void onListFragmentInteraction(Product product);
    }

    public List<Product> filtraProdutos(List<Product> produtos) {
        List<Product> produtosFiltrados = new ArrayList<>();
        produtosFiltrados.addAll(produtos);

        for (FiltroInterface filtro : filtros) {
            produtosFiltrados = filtro.filtra(produtosFiltrados);
        }

        return produtosFiltrados;
    }

    public void atualizaListaDeProdutos() {
        recyclerView.setAdapter(new ProductRecyclerViewAdapter(produtosAExibir, mListener));
    }

    public static void setFiltros(List<FiltroInterface> novosFiltros) {
        filtros = novosFiltros;
    }

    public static List<FiltroInterface> getFiltros() {
        return filtros;
    }

    public List<Product> ordenaProdutos(List<Product> produtos) {
        switch (ordem) {
            case 0:
                return ordenaAlfabeticamente(produtos);
            case 1:
                return ordenaPorAcesso(produtos);
            default:
                return ordenaAlfabeticamente(produtos);
        }
    }

    public List<Product> ordenaAlfabeticamente(List<Product> produtos) {
        List<Product> listaOrdenada = new ArrayList<>();
        listaOrdenada.addAll(produtos);

        Collections.sort(listaOrdenada, new Comparator<Product>() {
            @Override
            public int compare(Product p1, Product p2) {
                // Se p1 tem filhas e p2 NÃO tem filhas:
                if (!p1.getOntClass().listSubClasses().toList().isEmpty()
                        && p2.getOntClass().listSubClasses().toList().isEmpty()) {
                    return -1;
                }
                // Se p1 NÃO tem filhas e p2 tem filhas:
                else if (p1.getOntClass().listSubClasses().toList().isEmpty()
                        && !p2.getOntClass().listSubClasses().toList().isEmpty()) {
                    return 1;
                }
                // Se os dois estão na mesma categoria (ambos têm filhas ou ambos não têm filhas):
                else {
                    String nomeP1 = p1.getNome();
                    String nomeP2 = p2.getNome();
                    return nomeP1.compareTo(nomeP2);
                }
            }
        });

        return listaOrdenada;
    }

    public List<Product> ordenaPorAcesso(List<Product> produtos) {
        List<String> nomesProdutosMaes = new ArrayList<>();
        List<String> nomesProdutosFilhas = new ArrayList<>();
        final List<Product> produtosOrdenados = new ArrayList<>();
        produtosOrdenados.addAll(produtos);

        for (Product p : produtosOrdenados) {
            if (!p.getOntClass().listSubClasses().toList().isEmpty()) {
                nomesProdutosMaes.add(p.getNome());
            } else {
                nomesProdutosFilhas.add(p.getNome());
            }
        }

        // Adiciona acessos à classe Produto
        Log.d("Ordem","FetchOrdemTask!!!!!!!!!!!!!!!!!!!!!!!!!");
        try {
            new FetchOrderTask().execute(nomesProdutosMaes.toArray(new String[0]),
                    nomesProdutosFilhas.toArray(new String[0])).get();
        } catch (Exception e) {
            e.printStackTrace();
            return produtosOrdenados;
        }

        // Compara produtos
        Log.d("Ordem","Sort!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        Collections.sort(produtosOrdenados, new Comparator<Product>() {
            @Override
            public int compare(Product p1, Product p2) {
                // Se p1 tem filhas e p2 NÃO tem filhas:
                if (!p1.getOntClass().listSubClasses().toList().isEmpty()
                        && p2.getOntClass().listSubClasses().toList().isEmpty()) {
                    return -1;
                }
                // Se p1 NÃO tem filhas e p2 tem filhas:
                else if (p1.getOntClass().listSubClasses().toList().isEmpty()
                        && !p2.getOntClass().listSubClasses().toList().isEmpty()) {
                    return 1;
                }
                // Se os dois estão na mesma categoria (ambos têm filhas ou ambos não têm filhas):
                else {
                    // Ordenar pelo número de acessos
                    // Se acessos p1 > acessos p2
                    if (p1.getAcessos() > p2.getAcessos()) {
                        return -1;
                    }
                    // Se acessos p1 < acessos p2
                    else if (p1.getAcessos() < p2.getAcessos()) {
                        return 1;
                    }
                    // Se acessos p1 = acessos p2
                    else {
                        String nomeP1 = p1.getNome();
                        String nomeP2 = p2.getNome();
                        return nomeP1.compareTo(nomeP2);
                    }
                }
            }
        });

        return produtosOrdenados;
    }

    private class FetchOrderTask extends AsyncTask<String[], Void, Boolean> {

        @Override
        protected Boolean doInBackground(String[]... params) {
            try {
                for (String classeMae : params[0]) {
                    Cursor maesCursor = getActivity().getContentResolver().query(
                            DatabaseContract.MaesFilhasEntry.CONTENT_URI_JOIN,
                            null,
                            null,
                            new String[]{classeMae},
                            null
                    );
                    if (maesCursor != null) {
                        maesCursor.moveToFirst();
                        do {
                            for (Product p : produtosDoFragmento) {
                                Log.d("Order",maesCursor.getString(0) == null ? ":/" : maesCursor.getString(0));
                                if (p.getNome().equals(maesCursor.getString(0))) {
                                    p.setAcessos(Integer.valueOf(maesCursor.getString(1)));
                                }
                            }
                        } while (maesCursor.moveToNext());
                        maesCursor.close();
                    }
                }
                for (String classeFilha : params[1]) {
                    Cursor filhasCursor = getActivity().getContentResolver().query(
                            DatabaseContract.LogsEntry.CONTENT_URI,
                            null,
                            DatabaseContract.LogsEntry.COLUMN_PRODUTO + " = ?",
                            new String[]{classeFilha},
                            null
                    );
                    if (filhasCursor != null) {
                        filhasCursor.moveToFirst();
                        do {
                            for (Product p : produtosDoFragmento) {
                                if (p.getNome().equals(filhasCursor.getString(1))) {
                                    p.setAcessos(Integer.valueOf(filhasCursor.getString(2)));
                                }
                            }
                        } while (filhasCursor.moveToNext());
                        filhasCursor.close();
                    }
                }
                Log.d("Ordem","Saiu FetchOrdemTask");
                return true;
            } catch (UnsupportedOperationException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean result) {

        }

    }

    private class FetchOntologyTask extends AsyncTask<Void, Void, Boolean> {
        OntProperty temIngrediente;
        OntProperty preco;
        Map<OntClass, Integer> relacaoClasseIndividuo = new HashMap<>();

        OntProperty contavel;
        OntProperty gluten;
        OntProperty lactose;
        OntProperty vegetariano;
        OntProperty temGorduras;
        OntProperty temSal;
        Map<String, Map<Restricao, Boolean>> restricoesOntologia = new HashMap<>();

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // Caminhos dos arquivos
                InputStream assetFile;
                if (fileName == null)
                    assetFile = criaFileName();
                else {
                    assetFile = getActivity().getApplicationContext().getAssets().open(fileName);
                }
                String outputFilePath = fileDir + "/" + fileName;
                String protocol = "file:/";
                assetFile.mark(0);
                String SOURCE = getSource(assetFile);
                String NS = SOURCE + "#";

                // Carrega o arquivo dos assets para a pasta de arquivos do aplicativo
                assetFile.reset();
                carregaArquivoInicial(assetFile, outputFilePath);

                // Lê a ontologia
                if (!ontologiaLida) {
                    ontModel = ModelFactory.createOntologyModel(OWL_MEM);
                    ontModel.read(new FileInputStream(outputFilePath), "OWL");
                    ontologiaLida = true;
                }

                // Cria propriedades
                temIngrediente = ontModel.createOntProperty(NS + "temIngrediente");
                preco = ontModel.createOntProperty(NS + "preco");

                // Cria restrições
                contavel = ontModel.createOntProperty(NS + "contavel");
                gluten = ontModel.createOntProperty(NS + "gluten");
                lactose = ontModel.createOntProperty(NS + "lactose");
                vegetariano = ontModel.createOntProperty(NS + "vegetariano");
                temGorduras = ontModel.createOntProperty(NS + "temGorduras");
                temSal = ontModel.createOntProperty(NS + "temSal");

                // Pega indivíduos
                Log.d("6", "Carrega individuos");
                individuos = ontModel.listIndividuals().toList();
                relacaoClasseIndividuo = pegaClassesAPartirDeIndividuos(individuos);
                Log.d("7", "Carregou individuos");

                // Inicializa Banco de Dados
                if (!bancoInicializado) {
                    inicializaBancoDeProdutos(ontModel.getOntClass(NS + "Produto"));
                    bancoInicializado = true;
                }
                // Inicializa Ontologia
                if (!ontologiaInicializada) {
                    // inicializaOntologia(ontModel.getOntClass(NS + "Produto"));
                    inicializaOntologiaUmaNovaEsperanca(ontModel.getOntClass(NS + "Produto"));
                    ontologiaInicializada = true;
                }

                // Transforma as OntClasses em Products e popula a lista com produtos
                //HashMap<OntClass, Integer> classesContaveis = pegaClassesAPartirDeIndividuos(ontModel.listIndividuals().toSet());
                //populaListaProdutos(classesContaveis);
                //ArrayList<OntClass> classesNaoContaveis = pegaClassesPorAtributo(ontModel.listClasses().toSet(), contavel, false);
                // populaListaProdutos(classesNaoContaveis);

                // populaListaDeProdutos(pegaFilhasDaRaiz(ontModel.getOntClass(((MenuActivity) getActivity()).getIntentOntClassURI())));
                populaListaDeProdutosAExibir(pegaFilhasDaRaiz(ontModel.getOntClass(((MenuActivity) getActivity()).getIntentOntClassURI())));
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        private String getSource(InputStream arquivo) throws IOException {
            BufferedReader br = new BufferedReader(new InputStreamReader(arquivo));
            String linha = null;
            // xml:base="http://www.semanticweb.org/priscila/ontologies/2017/3/untitled-ontology-3"
            while ((linha = br.readLine()) != null) {
                if (linha.contains("xml:base")) {
                    Pattern pattern = Pattern.compile("\"(.+)\"");
                    Matcher matcher = pattern.matcher(linha);
                    matcher.find();
                    return matcher.group(1);
                }
            }

            throw new IOException("XML:BASE não encontrado em \"" + arquivo.toString() + "\"");
        }

        private void inicializaBancoDeProdutos(OntClass raiz) {
            List<OntClass> nodosFolhas = new ArrayList<>();
            List<OntClass> nodosMaes = new ArrayList<>();

            // Caminha na árvore
            Deque<OntClass> filinha = new ArrayDeque<>();
            filinha.add(raiz);
            while (!filinha.isEmpty()) {
                OntClass nodoAtual = filinha.pop();
                if (!nodoAtual.listSubClasses().toList().isEmpty()) {
                    nodosMaes.add(nodoAtual);
                    filinha.addAll(nodoAtual.listSubClasses().toList());
                } else {
                    nodosFolhas.add(nodoAtual);
                }
            }
            Cursor produtosCursor;
            produtosCursor = getActivity().getContentResolver().query(
                    DatabaseContract.LogsEntry.CONTENT_URI,
                    null,
                    null,
                    null,
                    null
            );
            if (produtosCursor != null) {
                if (produtosCursor.getCount() != nodosFolhas.size()) {
                    for (OntClass oc : nodosFolhas) {
                        ContentValues folha = new ContentValues();
                        folha.put("_id", (byte[]) null);
                        folha.put("produto", oc.getLabel("pt"));
                        folha.put("acessos", 0);
                        getActivity().getContentResolver().insert(DatabaseContract.LogsEntry.CONTENT_URI, folha);
                    }
                }
//                produtosCursor = getActivity().getContentResolver().query(
//                        DatabaseContract.LogsEntry.CONTENT_URI,
//                        null,
//                        null,
//                        null,
//                        null
//                );
//                if (produtosCursor != null) {
//                    produtosCursor.moveToFirst();
//                    do {
//                        Log.d("Logs", produtosCursor.getString(0) + "/" + produtosCursor.getString(1) + "/" + produtosCursor.getString(2));
//                    } while (produtosCursor.moveToNext());
//                }
            }
            produtosCursor = getActivity().getContentResolver().query(
                    DatabaseContract.MaesFilhasEntry.CONTENT_URI,
                    new String[]{"DISTINCT " + DatabaseContract.MaesFilhasEntry.COLUMN_MAE},
                    null,
                    null,
                    null
            );
            if (produtosCursor != null) {
                if (produtosCursor.getCount() != nodosMaes.size()) {
                    for (OntClass oc : nodosMaes) {
                        filinha.clear();
                        filinha.add(oc);
                        while (!filinha.isEmpty()) {
                            OntClass classeAtual = filinha.pop();
                            if (classeAtual.listSubClasses().toList().isEmpty()) {
                                Cursor cursorJr = getActivity().getContentResolver().query(
                                        DatabaseContract.LogsEntry.CONTENT_URI,
                                        new String[]{DatabaseContract.LogsEntry._ID},
                                        DatabaseContract.LogsEntry.COLUMN_PRODUTO + " = ?",
                                        new String[]{classeAtual.getLabel("pt")},
                                        null
                                );
                                if (cursorJr != null) {
                                    cursorJr.moveToFirst();
                                    ContentValues mae = new ContentValues();
                                    mae.put("_id", (byte[]) null);
                                    mae.put("nome_mae", oc.getLabel("pt"));
                                    mae.put("id_filha", cursorJr.getString(0));
                                    cursorJr.close();
                                    getActivity().getContentResolver().insert(DatabaseContract.MaesFilhasEntry.CONTENT_URI, mae);
                                }
                            } else {
                                filinha.addAll(classeAtual.listSubClasses().toList());
                            }
                        }
                    }
                }
//                produtosCursor = getActivity().getContentResolver().query(
//                        DatabaseContract.MaesFilhasEntry.CONTENT_URI,
//                        null,
//                        null,
//                        null,
//                        null
//                );
//                if (produtosCursor != null) {
//                    produtosCursor.moveToFirst();
//                    do {
//                        Log.d("MaeFilhas", produtosCursor.getString(0) + "/" + produtosCursor.getString(1) + "/" + produtosCursor.getString(2));
//                    } while (produtosCursor.moveToNext());
//                    produtosCursor.close();
//                }
                produtosCursor.close();
            }
        }

        private void inicializaOntologiaUmaNovaEsperanca(OntClass raiz) {
            List<Nodo> nodos = new ArrayList<>();
            // Nodo raiz é a classe Produto, que não entra no cardápio. Por isso, a raiz da árvore será nula.
            Nodo nodoRaiz = new Nodo(raiz);
            Deque<Nodo> filinha = new ArrayDeque<>();
            Stack<Nodo> pilhinha = new Stack<>();
            filinha.add(nodoRaiz);
            // Criando a árvore de cima pra baixo
            while (!filinha.isEmpty()) {
                Nodo nodoAtual = filinha.pop();
                pilhinha.add(nodoAtual);
                List<OntClass> filhosOntClass = nodoAtual.getOntClass().listSubClasses().toList();
                for (OntClass oc : filhosOntClass) {
                    Nodo filho = new Nodo(oc, individuos, nodoAtual);
                    nodoAtual.getFilhos().add(filho);
                    filinha.add(filho);
                }
                if (nodoAtual != nodoRaiz)
                    nodos.add(nodoAtual);
            }
            // Complementação da árvore de baixo pra cima
            while (!pilhinha.isEmpty()) {
                Nodo nodoAtual = pilhinha.pop();
                for (Nodo filho : nodoAtual.getFilhos()) {
                    nodoAtual.intercalaRestricoesParaBaixo(filho.getMapaRestricoes(), filho.isTemQuantidade());
                }
            }
            populaListaDeProdutosAOntologiaContraAtaca(nodos);
        }

        private void populaListaDeProdutosAOntologiaContraAtaca(List<Nodo> nodos) {
            int counter = 1;
            for (Nodo nodo : nodos) {
                Boolean contavel = nodo.getMapaRestricoes().get(Restricao.CONTAVEL);
                if (isNotContavel(contavel) || nodo.isTemQuantidade()) {
                    // Se não é nodo folha e é contável, verifica se tem indivídios e cria um Produto intermediário
                    if (!nodo.getFilhos().isEmpty() && contavel) {
                        if (individuos != null) {
                            produtos.add(transformaOntClassEmProdutoIntermediario(nodo));
                        }
                    }
                    // Se é nodo folha e contável, verifica indivíduos e cria um produto contável
                    else if (nodo.getFilhos().isEmpty() && contavel) {
                        if (individuos != null) {
                            if (nodo.isTemQuantidade())
                                produtos.add(transformaOntClassEmProdutoContavel(nodo));
                        }
                    }
                    // Se não é folha e não é contável, cria intermediário
                    else if (!nodo.getFilhos().isEmpty() && !contavel) {
                        produtos.add(transformaOntClassEmProdutoIntermediario(nodo));
                    }
                    // Se é folha e não é contável...
                    else if (nodo.getFilhos().isEmpty() && !contavel) {
                        produtos.add(transformaOntClassEmProdutoNaoContavel(nodo));
                    }
                }
            }
            if (individuos != null) {
                Log.d("Individuos", relacaoClasseIndividuo.toString());
            }
        }

        private void inicializaOntologia(OntClass raiz) {
            List<OntClass> nodos = new ArrayList<>();
            Deque<OntClass> filinha = new ArrayDeque<>();
            filinha.addAll(pegaFilhasDaRaiz(raiz));
            while (!filinha.isEmpty()) {
                OntClass nodoAtual = filinha.pop();
                if (!nodoAtual.listSubClasses().toList().isEmpty()) {
                    for (OntClass filho : nodoAtual.listSubClasses().toList()) {
                        filinha.addLast(filho);
                    }
                }
                nodos.add(nodoAtual);
            }
            Log.d("TotalProdutos", String.valueOf(nodos.size()));
            populaListaDeProdutos(nodos);
            Log.d("TotalProdutos2", String.valueOf(produtos.size()));
        }

        private void populaListaDeProdutosAExibir(List<OntClass> nodosFilhos) {
            produtosDoFragmento = new ArrayList<>();
            for (OntClass oc : nodosFilhos) {
                for (Product p : produtos) {
                    if (p.getNome().equals(oc.getLabel("pt"))) {
                        produtosDoFragmento.add(p);
                        break;
                    }
                }
            }
        }

        private List<OntClass> pegaFilhasDaRaiz(OntClass raiz) {
            return raiz.listSubClasses().toList();
        }


        @Override
        protected void onPostExecute(final Boolean result) {
            if (result) {
                configuraProdutosAExibir();
                progressDialog.dismiss();
                recyclerView.setAdapter(new ProductRecyclerViewAdapter(produtosAExibir, mListener));
            }
        }

        private void carregaArquivoInicial(InputStream inputStream, String outputPath) throws IOException {
            BufferedReader br = null;

            BufferedWriter bw = null;
            try {
                br = new BufferedReader(new InputStreamReader(inputStream));
                bw = new BufferedWriter(new FileWriter(outputPath));
                String inputLine;
                while ((inputLine = br.readLine()) != null) {
                    bw.write(inputLine);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                if (br != null)
                    br.close();
                if (bw != null)
                    bw.close();
            }
        }

        private Map<OntClass, Integer> pegaClassesAPartirDeIndividuos(List<Individual> individuals) {
            Map<OntClass, Integer> mapa = new HashMap<>();
            for (Individual i : individuals) {
                Boolean contemClasse = false;
                for (Map.Entry<OntClass, Integer> kv : mapa.entrySet()) {
                    if (kv.getKey().equals(i.getOntClass())) {
                        contemClasse = true;
                        kv.setValue(kv.getValue() + 1);
                    }
                    if (contemClasse)
                        break;
                }
                if (!contemClasse)
                    mapa.put(i.getOntClass(), 1);

            }
            return mapa;
        }

        private Double temRestricaoPreco(OntClass ontClass) {
            for (Iterator<OntClass> superClasses = ontClass.listSuperClasses(); superClasses.hasNext(); ) {
                OntClass superClasse = superClasses.next();
                if (superClasse.isRestriction()) {
                    Restriction restriction = superClasse.asRestriction();
                    if (restriction.isHasValueRestriction()) {
                        if (restriction.getOnProperty().equals(preco)) {
                            return restriction.asHasValueRestriction().getHasValue().as(Literal.class).getDouble();
                        }
                    }
                }
            }
            return null;
        }

        private boolean temFilhasComQuantidade(OntClass ontClass) {
            Deque<OntClass> filinha = new ArrayDeque<>();
            filinha.add(ontClass);
            OntClass classeAtual;
            while (!filinha.isEmpty()) {
                classeAtual = filinha.pop();
                if (classeAtual.listSubClasses().toList().isEmpty()) {
                    if (individuos != null) {
                        for (Individual i : individuos) {
                            if (i.getOntClass().equals(classeAtual)) {
                                return true;
                            }
                        }
                    }
                } else {
                    filinha.addAll(classeAtual.listSubClasses().toList());
                }
            }
            return false;
        }

        private OntClass pegaSuperClasse(OntClass ontClass) {
            for (Iterator<OntClass> superClasses = ontClass.listSuperClasses(); superClasses.hasNext(); ) {
                OntClass superClasse = superClasses.next();
                if (!superClasse.isRestriction() && superClasse.getLabel("pt") != null) {
                    return superClasse;
                }
            }
            return null;
        }

        private Map<Restricao, Boolean> verificaRestricoesDentreSuperClasses(OntClass ontClass) {
            Map<Restricao, Boolean> mapaRetorno = restricoesOntologia.get(ontClass.getLabel("pt"));
            if (mapaRetorno != null) {
                return mapaRetorno;
            } else {
                mapaRetorno = new HashMap<>();
                for (Restricao r : Restricao.values()) {
                    Boolean valorDaRestricao = VerificadorDeRestricao.verificaRestricao(ontClass, r);
                    mapaRetorno.put(r, valorDaRestricao);
                }
                return mapaRetorno;
            }
        }

        // Se o valor for nulo ou for, por exemplo, true para glúten, ele é indesejado.
        private boolean temValorIndesejado(Map<Restricao, Boolean> mapaRestricoes) {
            boolean taTodoMundoCerto = true;
            for (Map.Entry<Restricao, Boolean> kv : mapaRestricoes.entrySet()) {
                if (kv.getValue() == null) {
                    return true;
                } else {
                    taTodoMundoCerto &= verificaConjuntoRestricao(kv.getKey(), kv.getValue());
                }
                if (!taTodoMundoCerto) {
                    return true;
                }
            }
            return !taTodoMundoCerto;
        }

        private boolean verificaConjuntoRestricao(Restricao restricao, Boolean valor) {
            switch (restricao) {
                case CONTAVEL:
                    return !valor;
                case GLUTEN:
                    return !valor;
                case LACTOSE:
                    return !valor;
                case PRECO:
                    return valor;
                case TEMGORDURAS:
                    return !valor;
                case TEMSAL:
                    return !valor;
                case VEGETARIANO:
                    return valor;
                default:
                    return false;
            }
        }

        private void intercalaRestricoesParaBaixo(Map<Restricao, Boolean> mapaOrigem,
                                                  Map<Restricao, Boolean> mapaSecundario) {
            for (Map.Entry<Restricao, Boolean> kvOrigem : mapaOrigem.entrySet()) {
                // Se no mapaOrigem o valor for nulo, substitui pelo valor do novo vetor
                if (kvOrigem.getValue() == null) {
                    mapaOrigem.put(kvOrigem.getKey(), mapaSecundario.get(kvOrigem.getKey()));
                } else if (mapaSecundario.get(kvOrigem.getKey()) != null) {
                    // Se o valor não for nulo, mas for indesejado...
                    if (verificaConjuntoRestricao(kvOrigem.getKey(), mapaSecundario.get(kvOrigem.getKey()))) {
                        mapaOrigem.put(kvOrigem.getKey(), mapaSecundario.get(kvOrigem.getKey()));
                    }
                }
            }
        }

        private void intercalaRestricoesParaCima(Map<Restricao, Boolean> mapaOrigem,
                                                 Map<Restricao, Boolean> mapaSecundario) {
            for (Map.Entry<Restricao, Boolean> kvOrigem : mapaOrigem.entrySet()) {
                // Se no mapaOrigem o valor for nulo, substitui pelo valor do novo vetor
                if (kvOrigem.getValue() == null) {
                    mapaOrigem.put(kvOrigem.getKey(), mapaSecundario.get(kvOrigem.getKey()));
                }
            }
        }

        private void intercalaSeTemQuantidadeParaBaixo(boolean temFilhasComQuantidadeOrigem,
                                                       boolean temFilhasComQuantidadeSecundaria) {

        }

        private void olhaParaCima(OntClass ontClass, Map<Restricao, Boolean> mapaRestricoes) {
            while (ontClass != null && temValorIndesejado(mapaRestricoes)) {
                ontClass = pegaSuperClasse(ontClass);
                if (ontClass != null) {
                    Map<Restricao, Boolean> mapaSecundario = verificaRestricoesDentreSuperClasses(ontClass);
                    intercalaRestricoesParaCima(mapaRestricoes, mapaSecundario);
                }
            }
        }

        private void olhaParaBaixo(OntClass ontClass, Map<Restricao, Boolean> mapaRestricoes, boolean temFilhasComQuantidade) {
            Deque<OntClass> filinha = new ArrayDeque<>();
            filinha.add(ontClass);
            OntClass classeAtual;
            while (!filinha.isEmpty() && temValorIndesejado(mapaRestricoes)) {
                classeAtual = filinha.pop();
                Map<Restricao, Boolean> mapaSecundario = verificaRestricoesDentreSuperClasses(classeAtual);
                if (isNotContavel(mapaSecundario.get(Restricao.CONTAVEL)) ||
                        isNotContavel(mapaRestricoes.get(Restricao.CONTAVEL)) ||
                        temFilhasComQuantidade) {
                    intercalaRestricoesParaBaixo(mapaRestricoes, mapaSecundario);
                    if (!classeAtual.listSubClasses().toList().isEmpty()) {
                        filinha.addAll(classeAtual.listSubClasses().toList());
                    }
                }
            }
        }

        private void populaListaDeProdutos(List<OntClass> ontClasses) {
            int counter = 1;
            for (OntClass oc : ontClasses) {
                // Verifica se tem filhas com quantidade
                boolean temFilhasComQuantidade = temFilhasComQuantidade(oc);
                // Verifica se tem ou não restrições de filtragem e seus valores
                Map<Restricao, Boolean> mapaRestricoes = verificaRestricoesDentreSuperClasses(oc);
                // Verifica restricoes nas classes superiores
                olhaParaCima(oc, mapaRestricoes);
                // Verifica restricoes nas classes inferiores
                olhaParaBaixo(oc, mapaRestricoes, temFilhasComQuantidade);
                // Verifica se é ou não contável
                Boolean contavel = mapaRestricoes.get(Restricao.CONTAVEL);

                Log.d("mapa", counter++ + ". " + oc.getLabel("pt") + "/" + mapaRestricoes.toString());
                if (isNotContavel(contavel) || temFilhasComQuantidade) {
                    // Insere mapa de restrições no mapão
                    restricoesOntologia.put(oc.getLabel("pt"), mapaRestricoes);

                    // Se não é nodo folha e é contável, verifica se tem indivídios e cria um Produto intermediário
                    if (!oc.listSubClasses().toList().isEmpty() && contavel) {
                        if (individuos != null) {
                            // produtos.add(transformaOntClassEmProdutoIntermediario(oc, mapaRestricoes));
                        }
                    }
                    // Se é nodo folha e contável, verifica indivíduos e cria um produto contável
                    else if (oc.listSubClasses().toList().isEmpty() && contavel) {
                        if (individuos != null) {
                            for (Map.Entry<OntClass, Integer> kv : relacaoClasseIndividuo.entrySet()) {
                                if (kv.getKey().equals(oc)) {}
                                    //produtos.add(transformaOntClassEmProdutoContavel(oc, kv.getValue(), mapaRestricoes, null));
                            }
                        }
                    }
                    // Se não é folha e não é contável, cria intermediário
                    else if (!oc.listSubClasses().toList().isEmpty() && !contavel) {
                        // produtos.add(transformaOntClassEmProdutoIntermediario(oc, mapaRestricoes));
                    }
                    // Se é folha e não é contável...
                    else if (oc.listSubClasses().toList().isEmpty() && !contavel) {
                     //   produtos.add(transformaOntClassEmProdutoNaoContavel(oc, mapaRestricoes, null));
                    }
                }
            }
            if (individuos != null) {
                Log.d("Individuos", relacaoClasseIndividuo.toString());
            }
        }

        private boolean isNotContavel(Boolean contavel) {
            if (contavel != null) {
                if (!contavel) {
                    return true;
                }
            }
            return false;
        }

        private Product transformaOntClassEmProdutoContavel(Nodo nodo) {
            return new Product(
                    nodo.getOntClass().getLabel("pt"),
                    nodo.getPreco(),
                    nodo.getIngredientes(),
                    nodo.getQuantidade(),
                    nodo.getOntClass(),
                    nodo.getMapaRestricoes()
            );
        }

        private Product transformaOntClassEmProdutoNaoContavel(Nodo nodo) {
            return new Product(
                    nodo.getOntClass().getLabel("pt"),
                    nodo.getPreco(),
                    nodo.getIngredientes(),
                    nodo.getOntClass(),
                    nodo.getMapaRestricoes()
            );
        }

        private Product transformaOntClassEmProdutoIntermediario(Nodo nodo) {
            return new Product(
                    nodo.getOntClass().getLabel("pt"),
                    nodo.getOntClass(),
                    nodo.getMapaRestricoes()
            );
        }

        private double getPreco(OntClass ontClass) {
            OntClass classeAtual = ontClass;
            Double restricaoPreco = temRestricaoPreco(classeAtual);
            if (restricaoPreco != null)
                return restricaoPreco;
            while (restricaoPreco == null && classeAtual != null) {
                classeAtual = classeAtual.getSuperClass();
                if (classeAtual != null) {
                    restricaoPreco = temRestricaoPreco(classeAtual);
                }
            }

            return restricaoPreco != null ? restricaoPreco : 0.0;
        }

        private ArrayList<String> populaIngredientes(OntClass ontClass) {
            ArrayList<String> ingredientes = new ArrayList<>();

            for (Iterator<OntClass> superClasses = ontClass.listSuperClasses(); superClasses.hasNext(); ) {
                String ingrediente = displayType(superClasses.next(), temIngrediente);
                if (ingrediente != null) {
                    ingredientes.add(ingrediente);
                }
            }
            return ingredientes;
        }

        @Nullable
        private String displayType(OntClass superClass, OntProperty property) {
            if (superClass.isRestriction()) {
                return displayRestriction(superClass.asRestriction(), property);
            }
            return null;
        }

        @Nullable
        private String displayRestriction(Restriction restriction, OntProperty property) {
            if (restriction.isSomeValuesFromRestriction()) {
                if (restriction.getOnProperty().equals(property)) {
                    return restriction.asSomeValuesFromRestriction()
                            .getSomeValuesFrom()
                            .as(OntClass.class)
                            .getLabel("pt");
                }
            } else if (restriction.isAllValuesFromRestriction()) {
                return displayRestriction("all",
                        restriction.getOnProperty(),
                        restriction.asAllValuesFromRestriction().getAllValuesFrom());
            }
            return null;
        }

        @Nullable
        private String displayRestriction(String qualifier, OntProperty ontProperty, Resource
                constraint) {
            return null;
        }
    }

    public static int getOrdem() {
        return ordem;
    }

    public static void setOrdem(int ordem) {
        ProductFragment.ordem = ordem;
    }
}
