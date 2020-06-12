package type3;

import de.tuebingen.uni.sfs.germanet.api.*;
import shared.MultipleInitSeeds;
import shared.SharedMethods;

import java.io.*;
import java.util.*;


public class SeedsWN {
    private File germanetDir;
    private GermaNet germaNet;
    private List<Seed> negSeeds;
    private List<Seed> posSeeds;
    private List<String> posAdj = MultipleInitSeeds.posAdj;
    private List<String> posSubst = MultipleInitSeeds.posSubst;
    private List<String> posVerbs = MultipleInitSeeds.posVerbs;
    private List<String> negAdj = MultipleInitSeeds.negAdj;
    private List<String> negSubst = MultipleInitSeeds.negSubst;
    private List<String> negVerbs = MultipleInitSeeds.negVerbs;
    private List<String> posAll = MultipleInitSeeds.posAll;
    private List<String> negAll = MultipleInitSeeds.negAll;

    public SeedsWN(){
        try{
            this.germanetDir = new File("E:\\licenta\\GN_V140\\GN_V140_XML");
            this.germaNet = new GermaNet(germanetDir);

            String data = "";
            Map<Integer, Integer> posResult = seedWithWord("gut", 2, WordCategory.adj);
            this.posSeeds = seedsToVectors(posResult, 1.0);

            Map<Integer, Integer> negResult = seedWithWord("schlecht", 2, WordCategory.adj);
            this.negSeeds = seedsToVectors(negResult, 0.0);

//            Map<Integer, Integer> posResult = seedWithWordList(this.posSubst, 2, WordCategory.nomen);
//            this.posSeeds = seedsToVectors(posResult, 1.0);
//
//            Map<Integer, Integer> negResult = seedWithWordList(this.negSubst, 2, WordCategory.nomen);
//            this.negSeeds = seedsToVectors(negResult, 0.0);

            this.posSeeds = eliminateDuplicateEntires(this.posSeeds);
            this.negSeeds = eliminateDuplicateEntires(this.negSeeds);

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
     *  the synsets that were reached with "steps" steps.
     * @return
     */
    public Map<Integer, Integer> seedWithId(Integer synsetId, Integer steps, WordCategory categ) {
        Map<Integer, Integer> result = new HashMap<>();
        System.out.println("Travel breadth-first through wordnet starting with synset id " + synsetId);

       try {
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
     * Recursive method that finds all the related synsets for the given synset up to depth 2.
     * This relational graph travesing is for the feature vectors.
     * @return
     * @throws Exception
     */
    public Map<Integer, Integer> getRelatedSynsets(Integer synsetId, Integer steps, Map<Integer, Integer> relatedSynsets) throws Exception {
        if (germaNet.getSynsetByID(synsetId) == null) {
            throw new Exception("Synset with id '" + synsetId + "' is not in the germanet");
        }

        Synset synset = this.germaNet.getSynsetByID(synsetId);
        //if the synsetId already exists in map, we increase the frequency
        relatedSynsets.put(synset.getId()+1, relatedSynsets.containsKey(synset.getId())? relatedSynsets.get(synset.getId()) + 1 : 1);

        for (Synset syn : SharedMethods.getRelatedSynsetsDependingOnRelation(synset)) {
            if (steps > 0){
                getRelatedSynsets(syn.getId(), steps -1, relatedSynsets);
            } else {
                relatedSynsets.put(syn.getId()+1, relatedSynsets.containsKey(syn.getId())? relatedSynsets.get(syn.getId()) + 1 : 1);
            }
        }

        return relatedSynsets;
    }

    /**
     * In order to build the feature vector for the given synset, we have to find the to it related synset up to depth 2.
     * @param synsetId
     * @param sign
     * @return
     * @throws Exception
     */
    public Seed relatedSynsetsToSeed(Integer synsetId, Double sign) throws Exception {
        Synset synset = this.germaNet.getSynsetByID(synsetId);

        //get all related synsets to synsetId in depth 2
        Map<Integer, Integer> result = getRelatedSynsets(synsetId, 1, new HashMap<>());

        if (!result.isEmpty()) {
            return new Seed(sign, result);
        }

        return null;
    }

    /**
     * For every synset id, the method builds the feature vector.
     * @param reachedSynsets
     * @param sign
     * @return
     * @throws Exception
     */
    public List<Seed> seedsToVectors(Map<Integer, Integer> reachedSynsets, Double sign) throws Exception {
        List<Seed> reachedSeeds = new ArrayList<>();

        for (Integer key: reachedSynsets.keySet()) {
            Seed defToVector = relatedSynsetsToSeed(key, sign);
            if (defToVector != null) {
                reachedSeeds.add(defToVector);
            }
        }

        return reachedSeeds;
    }

    /**
     * Method that checks if the feature vectors of two seeds are the same.
     * If they are the same, means that there is a duplicate in the Seed list.
     * @param searchedSeed
     * @param seeds
     * @return
     */
    public Integer searchSeedInList(Seed searchedSeed, List<Seed> seeds) {
        for (Integer index=0; index<seeds.size(); index++ ) {
            if (seeds.get(index).getFeatures().equals(searchedSeed.getFeatures())){
                return index;
            }
        }

        return -1;
    }

    /**
     * Method that returns new list that does not contain duplicate seeds.
     * @param seeds
     * @return
     */
    public List<Seed> eliminateDuplicateEntires(List<Seed> seeds) {
        List<Seed> newSeedList = new ArrayList<>();

        for(Seed seed: seeds) {
            if( searchSeedInList(seed, newSeedList) < 0) {
                newSeedList.add(seed);
            }
        }

        return newSeedList;
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
     * Write the reached seeds with label and feature vector to SVM file.
     * @param data
     */
    public void writeSeedsToSVMFile(String data) {
        try {
            File statText = new File("E:\\licenta\\GermaNet\\src\\main\\java\\type3\\seedsFiles\\seeds.txt");
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



