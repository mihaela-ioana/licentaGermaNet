import de.tuebingen.uni.sfs.germanet.api.*;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

import java.io.*;
import java.util.*;

public class MyApplication {

    public static void main(String[] args) {
        //tutorial();
        //SeedsWN wn = new SeedsWN();
        SparkConf sparkConf = new SparkConf().setAppName("MyApplication").setMaster("local[2]").set("spark.executor.memory","1g");
        JavaSparkContext jsc = new JavaSparkContext(sparkConf);
        Train train = new Train(jsc);
        train.train();
    }

    static void tutorial () {
        try{
            Scanner keyboard = new Scanner(System.in);
            String destName;
            File gnetDir;
            String word;
            int depth;
            Writer dest;
            System.out.println("HypernymGraph creates a GraphViz graph " +
                    "description of hypernyms and hyponyms of a GermaNet" +
                    "concept up to a given depth.");
            System.out.println("Enter <word> <depth> <outputFile> " +
                    "[eg: Automobil 2 auto.dot]: ");
            word = keyboard.next();
            depth = keyboard.nextInt();
            destName = keyboard.nextLine().trim();
            gnetDir = new File("E:\\licenta\\GN_V140\\GN_V140_XML");
            GermaNet gnet = new GermaNet(gnetDir);
            List<Synset> synsets;
            synsets = gnet.getSynsets(word);
            if (synsets.size() == 0) {
                System.out.println(word + " not found in GermaNet");
                System.exit(0);
            }
            String dotCode = "";
            dotCode += "graph G {\n";
            dotCode += "overlap=false\n";
            dotCode += "splines=true\n";
            dotCode += "orientation=landscape\n";
            dotCode += "size=\"13,15\"\n";
            HashSet<Synset> visited = new HashSet<Synset>();
            System.out.println(synsets.size());
            for (Synset syn : synsets) {
                System.out.println(syn.getLexUnits().get(0));
                dotCode += printHypernyms(syn, depth, visited);
            }
            dotCode += "}";
            dest = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(new File(destName)), "UTF-8"));
            dest.write(dotCode);
            dest.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(0);
        }
    }

    static String printHypernyms(Synset synset, int depth,
                                 HashSet<Synset> visited) {
        String rval = "";
        List<LexUnit> lexUnits;
        String orthForm = "";
        List<Synset> hypernyms = new ArrayList<Synset>();
        List<Synset> relations;
        String hypOrthForm;
        visited.add(synset);
        lexUnits = synset.getLexUnits();
        orthForm = lexUnits.get(0).getOrthForm();
        rval += "\"" + orthForm + "\" [fontname=Helvetica,fontsize=10]\n";
        relations = synset.getRelatedSynsets(ConRel.has_hypernym);
        hypernyms.addAll(relations);
        relations = synset.getRelatedSynsets(ConRel.has_hyponym);
        hypernyms.addAll(relations);
        for (Synset syn : hypernyms) {
            if (!visited.contains(syn)) {
                hypOrthForm = syn.getLexUnits().get(0).getOrthForm();
                rval += "\"" + orthForm + "\" -- \"" + hypOrthForm + "\";\n";
                if (depth > 1) {
                    rval += printHypernyms(syn, depth - 1, visited);
                } else {
                    rval += "\"" + hypOrthForm +
                            "\"[fontname=Helvetica,fontsize=8]\n";
                }
            }
        }
        // return the graph string generated
        return rval;
    }
}
