package shared;

import de.tuebingen.uni.sfs.germanet.api.ConRel;
import de.tuebingen.uni.sfs.germanet.api.Synset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface SharedMethods {

    /**
     * Method that gets all the related synsets for the given synsets.
     * Methods, like antonymy, were not taken in consideration.
     * @param synset
     * @return
     */
    static List<Synset> getRelatedSynsetsDependingOnRelation(Synset synset) {
        List<Synset> hypernyms = new ArrayList<Synset>();
        List<Synset> relations = synset.getRelatedSynsets(ConRel.has_hypernym);
        hypernyms.addAll(relations);
        relations = synset.getRelatedSynsets(ConRel.has_hyponym);
        hypernyms.addAll(relations);
        relations = synset.getRelatedSynsets(ConRel.has_component_holonym);
        hypernyms.addAll(relations);
        relations = synset.getRelatedSynsets(ConRel.has_component_meronym);
        hypernyms.addAll(relations);

        return hypernyms;
    }

    /**
     * Method that takes the words and their frequencies from xml file
     *  and build a Map<key, value>, where key = word, value = frequency
     *  of the word in the paraphrases.
     */
    static Map<String, Long> loadCorpusFromXMLFile() {
        Map<String, Long> corpusWithFrequency = new HashMap<>();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(WordMap.class);
            File file = new File("E:\\licenta\\GermaNet\\src\\main\\java\\words.xml");

            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            WordMap wordMap = (WordMap) jaxbUnmarshaller.unmarshal(file);

            corpusWithFrequency = wordMap.getWordMap();
        } catch (JAXBException e) {
            e.printStackTrace();
        }

        return corpusWithFrequency;
    }
}
