import de.tuebingen.uni.sfs.germanet.api.*;
import javafx.util.Pair;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SeedsWN {
    private File germanetDir;
    private GermaNet germaNet;
    private List<String> corpus;
    private List<String> corpusWithDuplicates;
    private Map<String, Long> corpusWordsWithFrequency;
    private Map<String, List<Integer>> wordWithFrequencyList;
    private List<String> stopWords;
    private List<List<Integer>> X;
    private List<List<Integer>> Y;
    private List<List<List<Integer>>> seedsX;
    private List<List<List<Integer>>> seedsY;

    SeedsWN(){
        try{
            this.germanetDir = new File("E:\\licenta\\GN_V140\\GN_V140_XML");
            this.germaNet = new GermaNet(germanetDir);

            // freq > 100
            this.stopWords = Arrays.asList("etwas", "in", "sich", "von", "mit", "eine", "der", "die", "zu", "einer",
                    "ein", "und", "auf", "einen", "für", "durch", "einem", "oder", "an", "ohne", "das", "etw.",
                    "nicht", "den", "aus", "dem", "eines", "werden", "als", "jemandem", "bestimmten", "sein",
                    "machen", "jmdn.", "im", "haben", "jemandem", "wird", "zur", "bei", "des", "zum", "jmdm.",
                    "über", "ist", "nach", "o.ä.", "jemand", "lassen", "bestimmte", "bringen", "dass", "um");

            clearSeedsVectors();

            /**
            //build words to write to xml
            computeCorpus();
            computeFrequencyForWords();
            writeWordsWithFrequencyToXMLFile("E:\\licenta\\GermaNet\\src\\main\\java\\words.xml", this.corpusWordsWithFrequency);**/

            //get map with words and word frequency from xml file
            loadCorpusFromXMLFile();
            //build corpus
            putWordsFromMapInList();
            //eliminate stop words from corpus
            computeWords() ;

            Map<Integer, Integer> result = seedWithWord("gut", 10, WordCategory.adj);
            seedsToVectors(result, true);
            /**Map<String, Long> newResult = new HashMap<>();
            for (Map.Entry<Integer, Integer> pair: result.entrySet()){
                newResult.put(this.germaNet.getSynsetByID(pair.getKey()).getLexUnits().get(0).getOrthForm(), (long) pair.getValue());
            }
            newResult = sortByValue(newResult);
            //result.forEach((x, y)->System.out.println(this.germaNet.getSynsetByID(x) + ": " + y));
            writeWordsWithFrequencyToXMLFile("E:\\licenta\\GermaNet\\src\\main\\java\\reachedWords.xml", newResult);**/
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    private void clearSeedsVectors() {
        this.seedsX = new ArrayList<>();
        this.seedsY = new ArrayList<>();
    }

    /**
     * Method that takes the words and their frequencies from xml file
     *  and build a Map<key, value>, where key = word, value = frequency
     *  of the word in the paraphrases.
     */
    public void loadCorpusFromXMLFile() {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(WordMap.class);
            File file = new File("E:\\licenta\\GermaNet\\src\\main\\java\\words.xml");

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            WordMap wordMap = (WordMap) jaxbUnmarshaller.unmarshal(file);

            this.corpusWordsWithFrequency = wordMap.getWordMap();
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mehod that takes all read data from xml and build a corpus
     *      with all the found words.
     */
    public void putWordsFromMapInList() {
        this.corpus = new ArrayList<>();
        this.corpus.addAll(this.corpusWordsWithFrequency.keySet());
    }

    /**
     * Recursive method that crosses through the related synsets of a given synset.
     *
     * @return
     * @throws Exception
     */
    public Map<Integer, Integer> bfwalk_with_depth(Integer synsetId, Integer steps, WordCategory categ, Map<Integer, Integer> visited) throws Exception {
        if (!(synsetId instanceof Integer)) {
            throw new Exception("Argument 'synset_id' has incorrect type, expected int, got" + synsetId);
        }
        if (germaNet.getSynsetByID(synsetId) == null) {
            throw new Exception("Synset with id '" + synsetId + "' is not in the wordnet");
        }

        Synset synset = this.germaNet.getSynsetByID(synsetId);
        visited.put(synsetId, steps);

        List<Synset> hypernyms = new ArrayList<Synset>();
        List<Synset> relations;
        relations = synset.getRelatedSynsets(ConRel.has_hypernym);
        hypernyms.addAll(relations);
        relations = synset.getRelatedSynsets(ConRel.has_hyponym);
        hypernyms.addAll(relations);

        for (Synset syn : hypernyms) {
            if (!visited.keySet().contains(syn.getId()) && (categ == null || syn.getWordCategory().equals(categ))) {
                if (steps > 1) {
                    bfwalk_with_depth(syn.getId(), steps - 1, categ, visited);
                }
                else {
                    visited.put(syn.getId(), 0);
                }
            }
        }

        return visited;
    }

    /**
     * For the given synset id, a function is called (bfwalk) which returns a list with all
     *  the synsets that were reached with "steps" steps.
     * @return
     * @throws Exception
     */
    public Map<Integer, Integer> seedWithId(Integer synsetId, Integer steps, WordCategory categ) throws Exception {
        Map<Integer, Integer> result = new HashMap<>();
        System.out.println("Travel breadth-first through wordnet starting with synset id " + synsetId);

        Map<Integer, Integer> visited = bfwalk_with_depth(synsetId, steps, categ, new HashMap<>());
        for (Map.Entry<Integer, Integer> pair: visited.entrySet()) {
            result.put(pair.getKey(), steps-(pair.getValue()));
        }

        return result;
    }

    /**
     * Method that receives a list with synsets of the starting word which as given.
     * For every synset in list, the program crosses through all related synsets with given depth (steps).
     * @return a list with all synsets and the min step where they occured
     * @throws Exception
     */
    public Map<Integer, Integer> seedWithIds(List<Synset> synsets, Integer steps, WordCategory categ) throws Exception {
        Map<Integer, Integer> result = new HashMap<>();
        Map<Integer, Integer> tmp;
        for ( Synset syn : synsets) {
            tmp = seedWithId(syn.getId(), steps, categ);
            for (Integer synsetId: tmp.keySet()) {
                //take the lowest value
                if (result.get(synsetId) != null) {
                    result.put(synsetId, result.get(synsetId) > tmp.get(synsetId) ? tmp.get(synsetId) : result.get(synsetId));
                } else {
                    result.put(synsetId, tmp.get(synsetId));
                }
            }
        }

        return result;
    }

    /**
     * Method that receives a starting word, a category for the next to be traveled words and a depth
     *  (how far from the given synset should the program go.
     * @param word -> given start word
     * @param steps -> the iteration depth (how far is the found synset from the given one
     * @param categ -> word category, can be : adj, verb, nomen
     * @return a list of synset id's and the minimum position where they were found
     * @throws Exception
     */
    public Map<Integer, Integer> seedWithWord(String word, Integer steps, WordCategory categ) throws Exception {
        if (steps < 0 || steps == null){
            steps = 100;
        }
        Map<Integer, Integer> result = new HashMap<>();
        //get all synsets
        List<Synset> synsets = this.germaNet.getSynsets(word);
        if (synsets.size() > 0){
            result = seedWithIds(synsets, steps, categ);
        }

        return result;
    }

    public List<Integer> phraseToVector(String paraphrase) {
        List<Integer> result = new ArrayList<>();
        for (int index = 0; index < this.corpus.size(); index++){
            result.add(0);
        }
        String[] splittedParaphrase = paraphrase.split("[\\s+,/]");

        for (String word: splittedParaphrase) {
            word = word.replace("(", "");
            word = word.replace(")", "");

            Integer index = this.corpus.indexOf(word);
            if (index <= result.size() && index >= 0){
                result.add(index, result.get(index) + 1);
            }
        }

        return result;
    }

    public List<Integer> synsetDefToVector(Integer synsetId) {
        List<Integer> result = Collections.nCopies(this.corpus.size(), 0);
        Synset synset = this.germaNet.getSynsetByID(synsetId);

        if (synset.getParaphrase() != "") {
            return phraseToVector(synset.getParaphrase());
        }

        return result;
    }

    public void seedsToVectors(Map<Integer, Integer> reachedSynsets, boolean positive) {
        this.X = new ArrayList<>();
        this.Y = new ArrayList<>();

        for (Integer key: reachedSynsets.keySet()) {
            this.X.add(synsetDefToVector(key));
        }

        this.Y = Collections.nCopies(X.size(), positive ? Arrays.asList(0) : Arrays.asList(1));
    }

    public void addSeedsVectors(List<List<Integer>> X, List<List<Integer>> Y) {
        this.seedsX.add(X);
        this.seedsY.add(Y);
    }

    //eliminate stop words from corpus
    public void computeWords(){
        this.corpus.stream().filter(x -> !this.stopWords.contains(x)).collect(Collectors.toList());
    }

    /**
     * Method that writes the words with their frequency to a xml file.
     * JAXB uses a class containing the map<word, wordFreq> called WordMap.
     * @throws JAXBException
     */
    public void writeWordsWithFrequencyToXMLFile(String filePath, Map<String, Long> map) throws JAXBException {
        WordMap wordMap = new WordMap();

        //write words from corpus to xml
        wordMap.setWordMap(map);

        JAXBContext jaxbContext = JAXBContext.newInstance(WordMap.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        jaxbMarshaller.marshal(wordMap, System.out);
        jaxbMarshaller.marshal(wordMap, new File(filePath));
    }

    /**
     * Corpus is a list of unique words.
     * The words are taken from the paraphrases of the GermaNet lex units.
     * In GermaNet a synset contains several LexUnits. Every lex unit contains an OrthForm (actual word).
     * A sysnset also contains a paraphrase, but a lex unit does not contain a paraphrase.
     */
    public void computeCorpus (){
        System.out.println("Number of words: " + germaNet.getLexUnits().size());
        this.corpus = new ArrayList<String>();
        List<Synset> synstes = this.germaNet.getSynsets();
        for (Synset s : synstes){
            // addLexUnitsToCorpus(s);
            String def = s.getParaphrase();
            if (!def.isEmpty()){
              addParaphraseToCorpus(def);
            }
        }
        corpusProcessing();

        System.out.println("Number of words in cropus: " + this.corpus.size());
        System.out.println("Number of words in cropus with d: " + this.corpusWithDuplicates.size());
    }

    /**
     * Count number of appearances for each word => wordsWithFrequency.
     * ! not corpus, corpus contains distinct words
     */
    public void computeFrequencyForWords(){
        this.corpusWordsWithFrequency = new HashMap<>();
        for (String word : this.corpus){
            Long counter = this.corpusWithDuplicates.stream().filter(w->w.equals(word)).count();
            this.corpusWordsWithFrequency.put(word, counter);
        }

        this.corpusWordsWithFrequency = sortByValue(this.corpusWordsWithFrequency);
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> unsortMap) {

        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>(unsortMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }

        return result;

    }

    /**
     * Method for editing the corpus list.
     */
    private void corpusProcessing() {
        this.corpusWithDuplicates = this.corpus;
        //eliminate duplicates
        this.corpus = this.corpus.stream().distinct().collect(Collectors.toList());
        //eliminate null
        this.corpus = this.corpus.stream().filter(x -> x != null || !x.equals(",")).collect(Collectors.toList());

        //!!! to lower is neccesary here??
    }

    /**
     * If paraphrase exists, than split it in words and add each word to the corpus.
     * @param def
     */
    private void addParaphraseToCorpus(String def) {
        String[] words = def.split("[\\s+,/]");        //nur Komma und Space erscheint
        for (int index = 0; index < words.length; index ++){
            String word = words[index].replace("(", "");
            word = word.replace(")", "");
            this.corpus.add(word);
        }
    }

    private void addLexUnitsToCorpus(Synset s) {
        for (LexUnit lu : s.getLexUnits()){
            //!!!! orth form kann auch von mehreren Worter gebaut sein, was macht man dann??
            //zB: id: 46889, orth form: Alte Jungfer, synset id: 34097
            this.corpus.add(lu.getOrthForm());
        }
    }

    public List<List<Integer>> getX() {
        return X;
    }

    public List<List<Integer>> getY() {
        return Y;
    }

    public List<List<List<Integer>>> getSeedsX() {
        return seedsX;
    }

    public List<List<List<Integer>>> getSeedsY() {
        return seedsY;
    }
}

//needed class for writing into xml file
@XmlRootElement(name="words")
@XmlAccessorType(XmlAccessType.FIELD)
class WordMap{
    private Map<String, Long> wordMap = new HashMap<>();

    public Map<String, Long> getWordMap() {
        return  wordMap;
    }

    public void setWordMap (Map<String, Long> wordMap){
        this.wordMap = wordMap;
    }
}

