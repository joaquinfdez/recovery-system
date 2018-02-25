/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package practicaluceneconsultas;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.text.Document;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.facet.DrillDownQuery;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.LabelAndValue;
import org.apache.lucene.facet.sortedset.DefaultSortedSetDocValuesReaderState;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesFacetCounts;
import org.apache.lucene.facet.sortedset.SortedSetDocValuesReaderState;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldCollector;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author JFL
 */
public class PracticaLuceneConsultas {
    
    private Path path;
    private Directory directory;
    private IndexReader ireader;
    private IndexSearcher isearcher;
    
    public Analyzer analyzer = new SpanishAnalyzer();
    
    private Path pathTaxo = FileSystems.getDefault().getPath("C:\\Users\\JFL\\Desktop\\RI\\Practicas\\P3\\facetas");
    private Directory taxoDirectory = FSDirectory.open(pathTaxo);
    private TaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoDirectory);
    
    
    
    PracticaLuceneConsultas() throws IOException
    {
        path= FileSystems.getDefault().getPath("C:\\Users\\JFL\\Desktop\\RI\\Practicas\\P3\\indice");
        directory = FSDirectory.open(path);
        ireader = DirectoryReader.open(directory);
        isearcher = new IndexSearcher(ireader);
        
        FacetsCollector fc = new FacetsCollector();

    }
    
    Map<String,String> busquedaBooleanaIntervencionMateria(String texto, String materia) throws ParseException, IOException
    {
        Map<String, String> resultado = new HashMap<String, String>();
        BooleanQuery.Builder Constructor = new BooleanQuery.Builder();
        QueryParser parser1 = new QueryParser("intervencion", analyzer);
        QueryParser parser2 = new QueryParser("materia", analyzer);
        Query query1 = parser1.parse(texto);
        Query query2 = parser2.parse(materia);
        
        
        Constructor.add(query1, BooleanClause.Occur.MUST);
        Constructor.add(query2, BooleanClause.Occur.FILTER);
        BooleanQuery q = Constructor.build();
        ScoreDoc[] hits = isearcher.search(q, null, 20).scoreDocs;
        
        for(int i=0; i<hits.length; i++)
        {
            org.apache.lucene.document.Document hitDoc = isearcher.doc(hits[i].doc);
            resultado.put(hitDoc.get("nombreDoc"), hitDoc.get("extracto").toString());
        }
        return resultado;
        
    } 
    
    Map<String,String> busquedaIntervencion(String consulta, boolean resultadoOrdenado) throws ParseException, IOException
    {
        Map<String, String> resultado = new HashMap<String, String>();
        QueryParser parser = new QueryParser("intervencion", analyzer);
        Query query = parser.parse(consulta);
        ScoreDoc[] hits;
        
        if (resultadoOrdenado) {
             hits = isearcher.search(query, 20, new Sort(new SortField("nombreOrdenado", SortField.Type.STRING))).scoreDocs;
        }
        else
        {
            hits = isearcher.search(query, null, 20).scoreDocs;
        }

        for(int i=0; i<hits.length; i++)
        {
            org.apache.lucene.document.Document hitDoc = isearcher.doc(hits[i].doc);
            resultado.put(hitDoc.get("nombreDoc"), hitDoc.get("extracto").toString());
        }
        return resultado;
        
    }
    
    Map<String,String> busquedaFecha(int fecha_inicio, int fecha_fin, boolean resultadoOrdenado) throws IOException
    {
        Map<String, String> resultado = new HashMap<String, String>();
        if (fecha_inicio <= fecha_fin)
        {
            ScoreDoc[] hits;
            Query query = NumericRangeQuery.newIntRange("fecha", fecha_inicio, fecha_fin, true, true);
            if(!resultadoOrdenado){
                
                 hits = isearcher.search(query, null, 1000).scoreDocs;
            }
            else{
                hits = isearcher.search(query, 1000, new Sort(new SortField("nombreOrdenado", SortField.Type.STRING))).scoreDocs;
            }

            for(int i=0; i<hits.length; i++)
            {
                org.apache.lucene.document.Document hitDoc = isearcher.doc(hits[i].doc);
                resultado.put(hitDoc.get("nombreDoc"), hitDoc.get("fecha").toString());
            }
        }
        
        return resultado;
    }
    
    Map<String,String> recorreFechas() throws IOException{
        Map<String, String> resultado = new HashMap<String, String>();
        Query query = NumericRangeQuery.newIntRange("fecha", Integer.MIN_VALUE, Integer.MAX_VALUE, true, true);
        ScoreDoc[] hits = isearcher.search(query, null, 1000).scoreDocs;
        System.out.println("Tamanio map: " + hits.length);
        
        for(int i=0; i<hits.length; i++)
            {
                org.apache.lucene.document.Document hitDoc = isearcher.doc(hits[i].doc);
                String fechaResultado = hitDoc.get("fecha");
                resultado.put(hitDoc.get("nombreDoc"), fechaResultado);
            }
        return resultado;
        
    }
    
    Map<String,String> busquedaTama(int tam_inicio, int tam_fin, boolean resultadoOrdenado) throws IOException
    {
        Map<String, String> resultado = new HashMap<String, String>();
        if (tam_inicio <= tam_fin) {
            Query query = NumericRangeQuery.newIntRange("tamanio", tam_inicio, tam_fin, true, true);
            ScoreDoc[] hits;
            if(!resultadoOrdenado){
            hits = isearcher.search(query, null, 1000).scoreDocs;
            }
            else{
                hits = isearcher.search(query, 1000, new Sort(new SortField("nombreOrdenado", SortField.Type.STRING))).scoreDocs;
            }

            for(int i=0; i<hits.length; i++)
            {
                org.apache.lucene.document.Document hitDoc = isearcher.doc(hits[i].doc);
                resultado.put(hitDoc.get("nombreDoc"), hitDoc.get("tamanio").toString());
            }
        }
        
        return resultado;
    }
    
    Map<String,String> busquedaProximidad(String campo, String consulta, int proximidad) throws IOException, ParseException
    {
        Map<String, String> resultado = new HashMap<String, String>();
        QueryParser parser = new QueryParser(campo, analyzer);
        String comilla = "\"";
        String prox = "~" + Integer.toString(proximidad);
        Query query= parser.parse(comilla + consulta + comilla + prox);
        TopDocs hits = isearcher.search(query,  20); 
        
        //System.out.println(query);
        for(int i = 0; i< hits.scoreDocs.length; i++) {
            
            org.apache.lucene.document.Document hitDoc = isearcher.doc(hits.scoreDocs[i].doc);
            resultado.put(hitDoc.get("nombreDoc"), hitDoc.get("extracto").toString());
         }
         
        return resultado;
    }
    
    Map<String,String> busquedaFacetas(String faceta, String consulta) throws IOException, ParseException
    {
        Map<String, String> resultado = new HashMap<String, String>();
        FacetsConfig configfaceta = new FacetsConfig();      
        FacetsCollector fc = new FacetsCollector();
        QueryParser parser = new QueryParser("intervencion", analyzer);
        Query q = parser.parse(consulta);
        DrillDownQuery query = new DrillDownQuery(configfaceta,q);
        query.add("categoria", faceta);
        TopDocs hits= FacetsCollector.search(isearcher, query, 10, fc);

        for(int i = 0; i< hits.scoreDocs.length; i++) {
            
            org.apache.lucene.document.Document hitDoc = isearcher.doc(hits.scoreDocs[i].doc);
            resultado.put(hitDoc.get("nombreDoc"), hitDoc.get("extracto"));
         }
        
        Facets facetsFolder = new FastTaxonomyFacetCounts("categoria", taxoReader, configfaceta, fc);
        System.out.println(facetsFolder.toString());
        
        return resultado;
    }
    
    
    
    
    public static void main(String[] args) throws IOException, ParseException {
        
        /*PracticaLuceneConsultas prueba = new PracticaLuceneConsultas();
        
        String consulta = "procedimiento establecido";
        String consulta2_materia = "Gobierno";
        String consulta2_texto = "nueva estructura del Consejo";
        //int fecha_inicio = s;
        int fecha_fin = 20130717;
        
        //Map<String,String> result = prueba.busquedaIntervencion(consulta, true);
        //Map<String,String> result = prueba.busquedaBooleanaIntervencionMateria(consulta2_texto, consulta2_materia);
        //Map<String,String> result = prueba.busquedaTama(20, 50);
        //Map<String,String> result = prueba.busquedaFecha(fecha_inicio, fecha_fin);
        //Map<String,String> result = prueba.recorreFechas();
        Map<String,String> result = prueba.busquedaProximidad("intervencion", "gobierno andalucía", 6);
        //Map<String,String> result = prueba.busquedaFacetas("Política de empleo", "empleo");
        
        Iterator it = result.keySet().iterator();
        while(it.hasNext()){
          String key = (String) it.next();
          System.out.println("Clave: " + key + " -> Valor: " + result.get(key));
        }
        */
        
        PracticaLuceneConsultas prueba = new PracticaLuceneConsultas();
        GUI interfaz = new GUI();
        
        interfaz.setGUI(prueba);
        interfaz.setVisible(true);
    }
    
}
