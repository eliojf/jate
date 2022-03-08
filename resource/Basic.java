package test.uk.ac.shef.dcs.jate;

import uk.ac.shef.dcs.jate.core.algorithm.TFIDFAlgorithm;
import uk.ac.shef.dcs.jate.core.algorithm.TFIDFFeatureWrapper;
import uk.ac.shef.dcs.jate.core.extractor.NounPhraseExtractorOpenNLP;
import uk.ac.shef.dcs.jate.core.extractor.WordExtractor;
import uk.ac.shef.dcs.jate.core.feature.FeatureBuilderCorpusTermFrequencyMultiThread;
import uk.ac.shef.dcs.jate.core.feature.FeatureCorpusTermFrequency;
import uk.ac.shef.dcs.jate.core.feature.indexer.GlobalIndexBuilderMem;
import uk.ac.shef.dcs.jate.core.feature.indexer.GlobalIndexMem;
import uk.ac.shef.dcs.jate.model.CorpusImpl;
import uk.ac.shef.dcs.jate.model.Term;
import uk.ac.shef.dcs.jate.util.control.Lemmatizer;
import uk.ac.shef.dcs.jate.util.control.StopList;
import uk.ac.shef.dcs.jate.util.counter.WordCounter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class Basic extends TestCase
{
    public static Test suite()
    {
        return new TestSuite(Basic.class);
    }
    
    public void testJate()
    {
        try {
            String locale = "fr";
            
            Lemmatizer lemmatizer = new Lemmatizer("wordnet_dict");
            StopList stop = new StopList(true, locale + "-stoplist.txt");
            
            NounPhraseExtractorOpenNLP npextractor = new NounPhraseExtractorOpenNLP(stop, lemmatizer);
            WordExtractor wordextractor = new WordExtractor(stop, lemmatizer);

            //get corpus
            CorpusImpl corpus = new CorpusImpl("G:\\Programmation\\Web\\jate\\sample\\" + locale);
            
            //extract terms from global corpus
            GlobalIndexBuilderMem builder = new GlobalIndexBuilderMem();
            GlobalIndexMem termDocIndex = builder.build(corpus, npextractor);
            GlobalIndexMem wordDocIndex = builder.build(corpus, wordextractor);
            
            if (termDocIndex.getTermsCanonical().size() == 0) {
                termDocIndex = wordDocIndex;
            }
            
            //calculate terms frequences for each terms, used by algorithms
            WordCounter wordcounter = new WordCounter();
            FeatureCorpusTermFrequency termCorpusFreq = new FeatureBuilderCorpusTermFrequencyMultiThread(wordcounter, lemmatizer).build(termDocIndex);
            
            TFIDFAlgorithm algorithm = new TFIDFAlgorithm();
            Term[] result = algorithm.execute(new TFIDFFeatureWrapper(termCorpusFreq));
            for (Term r : result) {
                System.err.println(r.getConcept() + " (" + r.getConfidence() + ")");
            }
            
        } catch (Exception e) {
            System.err.println("Error keyword : " + e.getMessage());
            e.printStackTrace();
        }
    }
}