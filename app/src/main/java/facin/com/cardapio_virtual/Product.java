package facin.com.cardapio_virtual;

import com.hp.hpl.jena.ontology.OntClass;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by Priscila on 28/02/2017.
 */

public class Product {

    private String nome;
    private double preco;
    private ArrayList<String> ingredientes;
    private int quantidade;
    private int preferencia;
    private OntClass ontClass;
    // Propriedades da Ontologia
    private boolean contavel;
    // Filtros
    private Map<String, Boolean> mapaRestricoes;


    // Sem ingredientes
    public Product(String nome, double preco, int quantidade, OntClass ontClass,
                   Map<String, Boolean> mapaRestricoes) {
        this(nome, preco, new ArrayList<String>(), quantidade, ontClass, mapaRestricoes);
    }

    // Sem quantidade
    public Product(String nome, double preco, ArrayList<String> ingredientes, OntClass ontClass,
                   Map<String, Boolean> mapaRestricoes) {
        this(nome, preco, ingredientes, 0, ontClass, mapaRestricoes);

    }

    // Produto intermediário
    public Product(String nome, OntClass ontClass,
                   Map<String, Boolean> mapaRestricoes) {
        this(nome, 0, new ArrayList<String>(), 0, ontClass, mapaRestricoes);
    }

    // Contrutor vazio
    public Product() {
        this("", 0, new ArrayList<String>(), 0, null, new HashMap<String, Boolean>());
    }

    // Contrutor completo
    public Product(String nome, double preco, ArrayList<String> ingredientes, int quantidade, OntClass ontClass,
                   Map<String, Boolean> mapaRestricoes) {
        this.nome = nome;
        this.preco = preco;
        this.ingredientes = ingredientes;
        this.quantidade = quantidade;
        preferencia = 0;
        this.ontClass = ontClass;
        // Propriedades da Ontologia
        contavel = true;
        // Filtros
        this.mapaRestricoes = mapaRestricoes;
    }

    public String getIngredientesAsString() {
        String ingredientesAsString = "";
        for (String ingr : ingredientes) {
            ingredientesAsString = ingredientesAsString + ingr.toLowerCase();
            if (!ingredientes.get(ingredientes.size() - 1).equals(ingr))
                ingredientesAsString = ingredientesAsString + ", ";
        }
        return ingredientesAsString;
    }

    // TODO: Verificar String format
    public String getPrecoAsString() {

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.GERMANY);
        DecimalFormat decimalFormat = (DecimalFormat) numberFormat;
        return "R$ " + decimalFormat.format(preco);
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public double getPreco() {
        return preco;
    }

    public void setPreco(double preco) {
        this.preco = preco;
    }

    public ArrayList<String> getIngredientes() {
        return ingredientes;
    }

    public void setIngredientes(ArrayList<String> ingredientes) {
        this.ingredientes = ingredientes;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public int getPreferencia() {
        return preferencia;
    }

    public void setPreferencia(int preferencia) {
        this.preferencia = preferencia;
    }

    public boolean isContavel() {
        return contavel;
    }

    public void setContavel(boolean contavel) {
        this.contavel = contavel;
    }

    public OntClass getOntClass() {
        return ontClass;
    }

    public void setOntClass(OntClass ontClass) {
        this.ontClass = ontClass;
    }

    public Map<String, Boolean> getMapaRestricoes() {
        return mapaRestricoes;
    }

    public void setMapaRestricoes(Map<String, Boolean> mapaRestricoes) {
        this.mapaRestricoes = mapaRestricoes;
    }

}
