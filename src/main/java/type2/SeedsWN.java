package type2;

import de.tuebingen.uni.sfs.germanet.api.*;
import shared.MultipleInitSeeds;
import shared.SharedMethods;
import shared.StopWords;

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
    private List<String> posAdj = MultipleInitSeeds.posAdj;
    private List<String> posSubst = MultipleInitSeeds.posSubst;
    private List<String> posVerbs = MultipleInitSeeds.posVerbs;
    private List<String> negAdj = MultipleInitSeeds.negAdj;
    private List<String> negSubst = MultipleInitSeeds.negSubst;
    private List<String> negVerbs = MultipleInitSeeds.negVerbs;
    private List<String> posAll = MultipleInitSeeds.posAll;
    private List<String> negAll = MultipleInitSeeds.negAll;
    private List<Seed> negSeeds;
    private List<Seed> posSeeds;

    public SeedsWN(){
        try{
            this.germanetDir = new File("E:\\licenta\\GN_V140\\GN_V140_XML");
            this.germaNet = new GermaNet(germanetDir);

            // freq > 100
            this.stopWords = StopWords.stopWords;

            //get map with words and word frequency from xml file
            this.corpusWordsWithFrequency = SharedMethods.loadCorpusFromXMLFile();
            //build corpus
            putWordsFromMapInList();
            //eliminate stop words from corpus
            computeWords() ;

            String data = "";
            Map<Integer, Integer> posResult = seedWithWord("gut", 4, WordCategory.adj);
            this.posSeeds = seedsToVectors(posResult, 1.0);

            Map<Integer, Integer> negResult = seedWithWord("schlecht", 4, WordCategory.adj);
            this.negSeeds = seedsToVectors(negResult, -1.0);

//            Map<Integer, Integer> posResult = seedWithWordList(this.posVerbs, 4, WordCategory.verben);
//            this.posSeeds = seedsToVectors(posResult, 1.0);
//
//            Map<Integer, Integer> negResult = seedWithWordList(this.negVerbs, 4, WordCategory.verben);
//            this.negSeeds = seedsToVectors(negResult, -1.0);

//            eliminateAndEditDuplicates();
            this.posSeeds = roundLabels(this.posSeeds);
            this.negSeeds = roundLabels(this.negSeeds);

            //write seeds to file
            data += this.createDataForLIBSVMFile(this.posSeeds);
            data += this.createDataForLIBSVMFile(this.negSeeds);

            this.writeSeedsToSVMFile(data);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
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

    //eliminate stop words from corpus
    public void computeWords(){
        System.out.println("Corpus size: " + this.corpus.size());
        this.corpus = this.corpus.stream()
                .filter(x -> (!this.stopWords.contains(x) && x.matches("[a-zA-Z\\-]{3}[a-zA-Z\\-]*") && !x.isEmpty()))
                .collect(Collectors.toList());
        System.out.println("Corpus size after removing stop words: " + this.corpus.size());
    }

    /**
     * Recursive method that crosses through the related synsets of a given synset and up to a given depth.
     *
     * @return
     * @throws Exception
     */
    public Map<Integer, Integer> bfwalk_with_depth(Integer synsetId, Integer steps, WordCategory categ, Map<Integer, Integer> visited) throws Exception {
        if (!(synsetId instanceof Integer)) {
            throw new Exception("Argument 'synset_id' has incorrect type, expected int, got" + synsetId);
        }
        if (germaNet.getSynsetByID(synsetId) == null) {
            throw new Exception("Synset with id '" + synsetId + "' is not in the germanet");
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
     *  the synsets that were reached up to depth "steps".
     * @return
     */
    public Map<Integer, Integer> seedWithId(Integer synsetId, Integer steps, WordCategory categ) {
        Map<Integer, Integer> result = new HashMap<>();
        System.out.println("Travel breadth-first through wordnet starting with synset id " + synsetId);

        try{
            Map<Integer, Integer> visited = bfwalk_with_depth(synsetId, steps, categ, new HashMap<>());
            for (Map.Entry<Integer, Integer> pair: visited.entrySet()) {
                result.put(pair.getKey(), steps-(pair.getValue()));
            }
        } catch (Exception ex){
            System.out.println(ex.getMessage());
        }

        return result;
    }

    /**
     * Method that receives a list with synsets of the starting word which as given.
     * For every synset in list, the program crosses through all related synsets with given depth (steps),
     * in order the expand the seed list.
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
    public Map<Integer, Integer> seedWithWordList(List<String> list, Integer steps, WordCategory categ) {

        Map<Integer, Integer> entireResult = new HashMap<>();
        for (String word : list) {
            Map<Integer, Integer> resultForWord = seedWithWord(word, steps, categ);
            for (Integer synsetId: resultForWord.keySet()) {
                //if already exists in list, take the min step where the synset was found
                if (entireResult.get(synsetId) != null) {
                    entireResult.put(synsetId, entireResult.get(synsetId) > resultForWord.get(synsetId) ? resultForWord.get(synsetId) : entireResult.get(synsetId));
                } else {
                    entireResult.put(synsetId, resultForWord.get(synsetId));
                }
            }
        }

        return entireResult;
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
     * For a given synset id, the label is calculated:
     *      sign = -1 (for negative seeds) / 1 (for positive seeds)
     *      foundAt = the depth where the synset was found.
     *
     * @param synsetId
     * @param foundAt
     * @param sign
     * @return a new Seed that has a label and a feature vector
     */
    public Seed synsetDefToVector(Integer synsetId, Integer foundAt, Double sign) {
        Synset synset = this.germaNet.getSynsetByID(synsetId);

        if (synset.getParaphrase() != "") {
            return new Seed( Math.floor((sign/(foundAt+1))*100)/100, phraseToVector(synset.getParaphrase()));
        }

        return null;
    }

    /**
     * Method that builds for every reached synset the seed: label && feature vector.
     * @param reachedSynsets
     * @param sign
     * @return
     */
    public List<Seed> seedsToVectors(Map<Integer, Integer> reachedSynsets, Double sign) {
        List<Seed> reachedSeeds = new ArrayList<>();

        for (Integer key: reachedSynsets.keySet()) {
            Seed defToVector = synsetDefToVector(key, reachedSynsets.get(key), sign);
            if (defToVector != null && defToVector.getFeatures().size() > 0) {
                reachedSeeds.add(defToVector);
            }
        }

        return reachedSeeds;
    }

    /**
     * Method that check whether there are duplicates in the seeds list:
     *      duplicates are considered two seeds hat have the same feature vectors.
     * @param searchedSeed
     * @return
     */
    public Integer searchSeedInList(Seed searchedSeed) {
        for (Integer index=0; index<negSeeds.size(); index++ ) {
            if (negSeeds.get(index).getFeatures().equals(searchedSeed.getFeatures())){
                return index;
            }
        }

        return -1;
    }

    /**
     * For every seed that was reached in both positive and negative class,
     *      we make an average from their labels and eliminate one of the two seeds.
     */
    public void eliminateAndEditDuplicates() {
        for (Seed seed : posSeeds) {
            Integer similarSeedIndex = searchSeedInList(seed);
            if (similarSeedIndex >= 0) {
                Integer posIndex = posSeeds.indexOf(seed);
                seed.setLabel( Math.floor( ((seed.getLabel() + this.negSeeds.get(similarSeedIndex).getLabel()) / 2)*100)/100);
                posSeeds.set(posIndex, seed);
                negSeeds.remove(negSeeds.get(similarSeedIndex));
            }
        }
    }

    /**
     * The labels for our seeds are in R. In order to have a classification problem, we only need 3 label.
     * This is why we round the labels. We will only hae three labels:
     *      -1.0 for negative
     *      0.0 neutral
     *      1.0 positive
     * @param seeds
     * @return
     */
    public List<Seed> roundLabels (List<Seed> seeds) {
        for (Seed seed: seeds) {
            double label = 0.0;
            if (seed.getLabel() >= 0.2){
                label = 1.0;
            }
            else if (seed.getLabel() <= -0.2) {
                label = -1.0;
            }

            Integer seedIndex = seeds.indexOf(seed);
            seed.setLabel(label);
            seeds.set(seedIndex, seed);
        }

        return seeds;
    }

    /**
     * Method that transforms a seed vector to data that can be written in svm file.
     * In order to use spark's naiveBayes we need JavaRDD<LabelPoint> => svm file converted in javaRDD
     * A SVM File row format:  label  index:value index2:value2 ... , where value, value2, ... != 0.
     * @param X
     * @param
     * @return
     */
    public String createDataForLIBSVMFile(List<Seed> X) {
        String data = "";
        for (Seed row : X) {
            Map<Integer, Integer> treeMap = new TreeMap<>(row.getFeatures());
            data += row.getLabel() + " ";
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
            File statText = new File("E:\\licenta\\GermaNet\\src\\main\\java\\type2\\seedsFiles\\seeds.txt");
            FileOutputStream is = new FileOutputStream(statText);
            OutputStreamWriter osw = new OutputStreamWriter(is);
            Writer w = new BufferedWriter(osw);
            w.write(data);
            w.close();
        } catch (IOException e) {
            System.err.println("Problem writing to the file statsTest.txt");
        }
    }
}


