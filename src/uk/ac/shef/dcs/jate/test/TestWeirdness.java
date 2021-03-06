package uk.ac.shef.dcs.jate.test;

import net.didion.jwnl.JWNLException;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.core.algorithm.AbstractFeatureWrapper;
import uk.ac.shef.dcs.jate.core.algorithm.Algorithm;
import uk.ac.shef.dcs.jate.core.algorithm.WeirdnessAlgorithm;
import uk.ac.shef.dcs.jate.core.algorithm.WeirdnessFeatureWrapper;
import uk.ac.shef.dcs.jate.core.feature.FeatureBuilderCorpusTermFrequency;
import uk.ac.shef.dcs.jate.core.feature.FeatureBuilderRefCorpusTermFrequency;
import uk.ac.shef.dcs.jate.core.feature.FeatureCorpusTermFrequency;
import uk.ac.shef.dcs.jate.core.feature.FeatureRefCorpusTermFrequency;
import uk.ac.shef.dcs.jate.core.feature.indexer.GlobalIndexBuilderMem;
import uk.ac.shef.dcs.jate.core.feature.indexer.GlobalIndexMem;
import uk.ac.shef.dcs.jate.core.extractor.NounPhraseExtractorOpenNLP;
import uk.ac.shef.dcs.jate.core.extractor.WordExtractor;
import uk.ac.shef.dcs.jate.io.ResultWriter2File;
import uk.ac.shef.dcs.jate.model.CorpusImpl;
import uk.ac.shef.dcs.jate.model.Term;
import uk.ac.shef.dcs.jate.util.control.Lemmatizer;
import uk.ac.shef.dcs.jate.util.control.StopList;
import uk.ac.shef.dcs.jate.util.counter.TermFreqCounter;
import uk.ac.shef.dcs.jate.util.counter.WordCounter;
import uk.ac.shef.dcs.jate.core.extractor.CandidateTermExtractor;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Author: Ziqi Zhang
 * Organisation: Department of Computer Science, University of Sheffield
 * Email: z.zhang@dcs.shef.ac.uk
 * Time: 13:58:47
 */


public class TestWeirdness {

	private Map<Algorithm, AbstractFeatureWrapper> _algregistry = new HashMap<Algorithm, AbstractFeatureWrapper>();
	private static Logger _logger = Logger.getLogger(AlgorithmTester.class.getName());

	public void registerAlgorithm(Algorithm a, AbstractFeatureWrapper f) {
		_algregistry.put(a, f);
	}

	public void execute(GlobalIndexMem index) throws JATEException, IOException {
		_logger.info("Initializing outputter, loading NP mappings...");
		ResultWriter2File writer = new ResultWriter2File(index);
		if (_algregistry.size() == 0) throw new JATEException("No algorithm registered!");
		_logger.info("Running NP recognition...");

		/*.extractNP(c);*/
		for (Map.Entry<Algorithm, AbstractFeatureWrapper> en : _algregistry.entrySet()) {
			_logger.info("Running feature store builder and ATR..." + en.getKey().toString());
			Term[] result = en.getKey().execute(en.getValue());
			writer.output(result, en.getKey().toString() + ".txt");
		}
	}

	public static void main(String[] args) throws IOException, JATEException, JWNLException {

		if (args.length < 3) {
			System.out.println("Usage: java TestWeirdness [path_to_corpus] [path_to_reference_corpus_stats] [output_folder]");
		} else {
			System.out.println("Started "+ TestWeirdness.class+"at: " + new Date() + "... For detailed progress see log file jate.log.");

			//creates instances of required processors and resources

			//stop word list
			StopList stop = new StopList(true);

			//lemmatiser
			Lemmatizer lemmatizer = new Lemmatizer();

			//noun phrase extractor, which produces candidate terms as noun phrases
			CandidateTermExtractor npextractor = new NounPhraseExtractorOpenNLP(stop, lemmatizer);
			//we also need a word extractor which produces each single word found in the corpus as required by the algorithm
			CandidateTermExtractor wordextractor = new WordExtractor(stop,lemmatizer);

			//counters
			TermFreqCounter npcounter = new TermFreqCounter();
			WordCounter wordcounter = new WordCounter();

			//create global resource index builder, which indexes global resources, such as documents and terms and their
			//relations
			GlobalIndexBuilderMem builder = new GlobalIndexBuilderMem();
			//build the global resource index of noun phrases
			GlobalIndexMem termDocIndex = builder.build(new CorpusImpl(args[0]), npextractor);
			//build the global resource index of words
			GlobalIndexMem wordDocIndex=builder.build(new CorpusImpl(args[0]), wordextractor);

			//build a feature store required by the tfidf algorithm, using the processors instantiated above
			FeatureCorpusTermFrequency termCorpusFreq =
					new FeatureBuilderCorpusTermFrequency(npcounter, wordcounter, lemmatizer).build(termDocIndex);
			FeatureCorpusTermFrequency wordFreq =
					new FeatureBuilderCorpusTermFrequency(npcounter,wordcounter,lemmatizer).build(wordDocIndex);
			FeatureRefCorpusTermFrequency bncRef =
					new FeatureBuilderRefCorpusTermFrequency(args[1]).build(null);

			AlgorithmTester tester = new AlgorithmTester();
			tester.registerAlgorithm(new WeirdnessAlgorithm(), new WeirdnessFeatureWrapper(wordFreq, termCorpusFreq, bncRef));
			tester.execute(termDocIndex, args[2]);
			System.out.println("Ended at: " + new Date());
		}
	}

}
