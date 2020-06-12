package type1;

import de.tuebingen.uni.sfs.germanet.api.*;
import shared.SharedMethods;
import shared.StopWords;
import shared.WordMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class SeedsWN {
    private File germanetDir;
    private GermaNet germaNet;
    private List<String> corpus;
    private List<String> corpusWithDuplicates;
    private Map<String, Long> corpusWordsWithFrequency;
    private Map<String, List<Integer>> wordWithFrequencyList;
    private List<String> stopWords;
    private List<Map<Integer, Integer>> X;
    private List<List<Integer>> Y;

    SeedsWN(){
        try{
            this.germanetDir = new File("E:\\licenta\\GN_V140\\GN_V140_XML");
            this.germaNet = new GermaNet(germanetDir);

            // freq > 100
            this.stopWords = StopWords.stopWords;


            //build words to write to xml
//            computeCorpus();
//            computeFrequencyForWords();
//            computeWords();
//            writeWordsWithFrequencyToXMLFile("E:\\licenta\\GermaNet\\src\\main\\java\\words.xml", this.corpusWordsWithFrequency);

            //get map with words and word frequency from xml file
            this.corpusWordsWithFrequency = SharedMethods.loadCorpusFromXMLFile();
            //build corpus
            putWordsFromMapInList();
            //eliminate stop words from corpus
            computeWords() ;

            String data = "";
            Map<Integer, Integer> result = seedWithWord("gut", 2, WordCategory.adj);
            seedsToVectors(result, true);
            //write seeds to file
            data += this.createDataForLIBSVMFile(this.X, true);
            Map<Integer, Integer> result2 = seedWithWord("schlecht", 2, WordCategory.adj);
            seedsToVectors(result2, false);
            data += this.createDataForLIBSVMFile(this.X, false);
            System.out.println("number of init seeds " + result.entrySet().size() + " " + result2.entrySet().size());

            this.writeSeedsToSVMFile(data);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * Method that takes the words and their frequencies from xml file
     *  and builds a Map<key, value>, where key = word, value = frequency
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
     * Mehod that takes all read data from xml and builds a corpus
     *      with all the found words.
     */
    public void putWordsFromMapInList() {
        this.corpus = new ArrayList<>();
        this.corpus.addAll(this.corpusWordsWithFrequency.keySet());
    }

    //eliminate stop words from corpus
    public void computeWords(){
        System.out.println("Corpus size: " + this.corpus.size());
        this.corpus = this.corpus.stream()
                .filter(x -> (!this.stopWords.contains(x) && x.matches("[a-zA-Z\\-]{3}[a-zA-Z\\-]*") && !x.isEmpty()))
                .collect(Collectors.toList());
        System.out.println("Corpus size after removing stop words: " + this.corpus.size());
    }

    /**
     * Recursive method that crosses through the related synsets of a given synset.
     *
     * @return
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

        for (Synset syn : SharedMethods.getRelatedSynsetsDependingOnRelation(synset)) {
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
     */
    public Map<Integer, Integer> seedWithId(Integer synsetId, Integer steps, WordCategory categ) {
        Map<Integer, Integer> result = new HashMap<>();
        System.out.println("Travel breadth-first through germanet starting with synset id " + synsetId);

        try{
            Map<Integer, Integer> visited = bfwalk_with_depth(synsetId, steps, categ, new HashMap<>());
            for (Map.Entry<Integer, Integer> pair: visited.entrySet()) {
                result.put(pair.getKey(), steps-(pair.getValue()));
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }


        return result;
    }

    /**
     * Method that receives a list with synsets of the starting word which as given.
     * For every synset in list, the program crosses through all related synsets with given depth (steps).
     * @return a list with all synsets and the min step where they occured
     */
    public Map<Integer, Integer> seedWithIds(List<Synset> synsets, Integer steps, WordCategory categ) {
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
     */
    public Map<Integer, Integer> seedWithWord(String word, Integer steps, WordCategory categ) {
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

    /**
     * Start the seeding process from multiple initial seeds.
     * This method takes a list with words from which the trasersing the relational graph begins.
     * @param list
     * @param steps
     * @param categ
     * @return
     */
    public Map<Integer, Integer> seedWithWordList(List<String> list, Integer steps, WordCategory categ, Map<Integer, Integer> existingResult) throws Exception {
        for (String word : list) {
            Map<Integer, Integer> resultForWord = seedWithWord(word, steps, categ);
            for (Integer synsetId: resultForWord.keySet()) {
                //take the lowest depth if the synset already exists in map
                if (existingResult.get(synsetId) != null) {
                    existingResult.put(synsetId, existingResult.get(synsetId) > resultForWord.get(synsetId) ? resultForWord.get(synsetId) : existingResult.get(synsetId));
                } else {
                    existingResult.put(synsetId, resultForWord.get(synsetId));
                }
            }
        }

        return existingResult;
    }

    /**
     * In order to make the features vector, we have to get for every word from the synset definition the corpus index.
     * We also get the frequency for every word from the synset definition (paraphrase).
     * @param paraphrase
     * @return
     */
    public Map<Integer, Integer> phraseToVector(String paraphrase) {
        Map<Integer, Integer> result = new HashMap<>();
        String[] splittedParaphrase = paraphrase.split("[\\s+,/()<>.?!^%`~\\[\\]\\-{};:]");

        for (String word: splittedParaphrase) {
            word = word.toLowerCase();

            Integer index = this.corpus.indexOf(word);
            if (index >= 0){
                result.put(index, result.containsKey(index) ? result.get(index) + 1 : 1);
            }
        }

        return result;
    }

    /**
     * For the synset id, the feature vector is returned.
     * @param synsetId
     * @return
     */
    public Map<Integer, Integer> synsetDefToVector(Integer synsetId) {
        Map<Integer, Integer> result = new HashMap<>();
        Synset synset = this.germaNet.getSynsetByID(synsetId);

        if (synset.getParaphrase() != "") {
            return phraseToVector(synset.getParaphrase());
        }

        return result;
    }

    /**
     * Method that builds for every reached synset the seed: label && feature vector.
     * @param reachedSynsets
     * @param positive
     * @return
     */
    public void seedsToVectors(Map<Integer, Integer> reachedSynsets, boolean positive) {
        this.X = new ArrayList<>();
        this.Y = new ArrayList<>();

        for (Integer key: reachedSynsets.keySet()) {
            Map<Integer, Integer> defToVector = synsetDefToVector(key);
            if (!defToVector.isEmpty()) {
                this.X.add(defToVector);
            }
        }

        this.Y = Collections.nCopies(X.size(), positive ? Arrays.asList(0) : Arrays.asList(1));
    }

    /**
     * Method that transforms a seed vector to data that can be written in svm file.
     * In order to use spark's naiveBayes we need JavaRDD<LabelPoint> => svm file converted in javaRDD
     * A SVM File row format:  label  index:value index2:value2 ... , where value, value2, ... != 0.
     * @param X
     * @param positive
     * @return
     */
    public String createDataForLIBSVMFile(List<Map<Integer, Integer>> X, boolean positive) {
        String data = "";
        for (Map<Integer, Integer> row : X) {
            Map<Integer, Integer> treeMap = new TreeMap<>(row);
            data += positive ? (1 + " ") : (0 + " ");
            for (Map.Entry<Integer, Integer> pair : treeMap.entrySet()) {
                data += pair.getKey() + ":" + pair.getValue() + " ";
            }
            data += "\n";
        }

        return data;
    }

    /**
     * Writes reached seeds to SVM file.
     * @param data
     */
    public void writeSeedsToSVMFile(String data) {
        try {
            File statText = new File("E:\\licenta\\GermaNet\\src\\main\\java\\seeds.txt");
            FileOutputStream is = new FileOutputStream(statText);
            OutputStreamWriter osw = new OutputStreamWriter(is);
            Writer w = new BufferedWriter(osw);
            w.write(data);
            w.close();
        } catch (IOException e) {
            System.err.println("Problem writing to the file statsTest.txt");
        }
    }

    /**
     * Corpus is a list of unique words.
     * The words are taken from the paraphrases of the GermaNet lex units.
     * In GermaNet a synset contains several LexUnits. Every lex unit contains an OrthForm (actual word).
     * A sysnset also contains a paraphrase, but a lex unit does not contain a paraphrase.
     */
    public void computeCorpus (){
        this.corpus = new ArrayList<>();
        this.corpusWithDuplicates = new ArrayList<>();
        List<Synset> synstes = this.germaNet.getSynsets();
        for (Synset s : synstes){
            String def = s.getParaphrase();
            if (!def.isEmpty()){
              addParaphraseToCorpus(def);
            }
        }
        corpusProcessing();

        System.out.println("Number of words in cropus: " + this.corpus.size());
        System.out.println("Number of words in cropus with duplicates: " + this.corpusWithDuplicates.size());
    }

    /**
     * If paraphrase exists, than split it in words and add each word to the corpus.
     * @param def
     */
    private void addParaphraseToCorpus(String def) {
        String[] words = def.split("[\\s+,/()<>.?!^%`~\\[\\]\\-{};]");
        for (int index = 0; index < words.length; index ++){
            String word = words[index].toLowerCase();
            if(!word.isEmpty()){
                this.corpusWithDuplicates.add(word);
            }
        }
    }

    /**
     * Method for editing the corpus list.
     */
    private void corpusProcessing() {
        this.corpus = this.corpusWithDuplicates;
        //eliminate duplicates
        this.corpus = this.corpus.stream().distinct().collect(Collectors.toList());
        //eliminate null
        this.corpus = this.corpus.stream().filter(x -> x != null || !x.matches("\\d+[.-]\\d*")).collect(Collectors.toList());
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

    /**
     * Method that writes the words with their frequency to a xml file.
     * JAXB uses a class containing the map<word, wordFreq> called type1.WordMap.
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
     * Method for sorting Map.
     * @param unsortMap
     * @param <K>
     * @param <V>
     * @return sorted map
     */
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
}


