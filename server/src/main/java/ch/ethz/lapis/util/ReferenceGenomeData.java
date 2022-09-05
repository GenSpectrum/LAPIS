package ch.ethz.lapis.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ReferenceGenomeData {

    private static ReferenceGenomeData instance;
    private final String nucleotideSequence;
    private final char[] nucleotideSequenceArr;
    private final Map<String, String> geneAASequences = new HashMap<>();
    private final Map<String, char[]> geneAASequencesArr = new HashMap<>();
    private final Map<String, String> geneNameUpperCaseToCorrect = new HashMap<>(); // E.g., ORF1A -> ORF1a

    private ReferenceGenomeData() {
        try {
            InputStream in = getClass().getResourceAsStream("/reference-genome.json");
            JSONObject json = (JSONObject) new JSONParser().parse(new InputStreamReader(in, StandardCharsets.UTF_8));
            nucleotideSequence = (String) json.get("nucleotide_sequence");
            nucleotideSequenceArr = nucleotideSequence.toCharArray();

            JSONArray genes = (JSONArray) json.get("genes");
            for (Object _gene : genes) {
                JSONObject gene = (JSONObject) _gene;
                String name = (String) gene.get("name");
                String sequence = (String) gene.get("sequence");
                geneAASequences.put(name, sequence);
                geneAASequencesArr.put(name, sequence.toCharArray());
                geneNameUpperCaseToCorrect.put(name.toUpperCase(), name);
            }
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized public static ReferenceGenomeData getInstance() {
        if (instance == null) {
            instance = new ReferenceGenomeData();
        }
        return instance;
    }

    public String getNucleotideSequence() {
        return nucleotideSequence;
    }


    public char[] getNucleotideSequenceArr() {
        return nucleotideSequenceArr;
    }


    public Map<String, String> getGeneAASequences() {
        return geneAASequences;
    }


    public Map<String, char[]> getGeneAASequencesArr() {
        return geneAASequencesArr;
    }


    public List<String> getGeneNames() {
        return new ArrayList<>(geneAASequences.keySet());
    }


    /**
     * @param position Starts with the index 1
     */
    public char getNucleotideBase(int position) {
        return nucleotideSequenceArr[position - 1];
    }


    /**
     * @param position Starts with the index 1
     */
    public char getGeneAABase(String gene, int position) {
        return geneAASequencesArr.get(getCorrectlyCapitalizedGeneName(gene))[position - 1];
    }


    public String getCorrectlyCapitalizedGeneName(String geneName) {
        return geneNameUpperCaseToCorrect.get(geneName.toUpperCase());
    }
}
