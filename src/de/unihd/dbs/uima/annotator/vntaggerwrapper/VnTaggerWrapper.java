package de.unihd.dbs.uima.annotator.vntaggerwrapper;

import de.unihd.dbs.uima.annotator.heideltime.utilities.Logger;
import de.unihd.dbs.uima.types.heideltime.Token;
import jmaxent.Classification;
import jvnpostag.POSContextGenerator;
import jvnpostag.POSDataReader;
import jvnsegmenter.CRFSegmenter;
import jvnsensegmenter.JVnSenSegmenter;
import jvntextpro.JVnTextPro;
import jvntextpro.conversion.CompositeUnicode2Unicode;
import jvntextpro.data.DataReader;
import jvntextpro.data.Sentence;
import jvntextpro.data.TWord;
import jvntextpro.data.TaggingData;
import jvntextpro.util.StringUtils;
import jvntokenizer.PennTokenizer;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import vn.hus.nlp.sd.SentenceDetector;
import vn.hus.nlp.sd.SentenceDetectorFactory;
import vn.hus.nlp.tokenizer.VietTokenizer;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by tienbm90 on 27/11/2016.
 */

public class VnTaggerWrapper extends JCasAnnotator_ImplBase {
    private Class<?> component = this.getClass();

    // definitions of what names these parameters have in the wrapper's descriptor file
    public static final String PARAM_SENTSEGMODEL_PATH = "sent_model_path";
    public static final String PARAM_WORDSEGMODEL_PATH = "word_model_path";
    public static final String PARAM_POSMODEL_PATH = "pos_model_path";
    public static final String PARAM_ANNOTATE_TOKENS = "annotate_tokens";
    public static final String PARAM_ANNOTATE_SENTENCES = "annotate_sentences";
    public static final String PARAM_ANNOTATE_PARTOFSPEECH = "annotate_partofspeech";

    // switches for annotation parameters
    private Boolean annotate_tokens = false;
    private Boolean annotate_sentences = false;
    private Boolean annotate_partofspeech = false;
    private String sentModelPath = null;
    private String wordModelPath = null;
    private String posModelPath = null;

    // private jvntextpro objects
    JVnSenSegmenter vnSenSegmenter = new JVnSenSegmenter();
    CRFSegmenter vnSegmenter = new CRFSegmenter();
    DataReader reader = new POSDataReader();
    TaggingData dataTagger = new TaggingData();
    Classification classifier = null;

    /**
     * initialization method where we fill configuration values and check some prerequisites
     */
    public void initialize(UimaContext aContext) {
        // get configuration from the descriptor
        annotate_tokens = (Boolean) aContext.getConfigParameterValue(PARAM_ANNOTATE_TOKENS);
        annotate_sentences = (Boolean) aContext.getConfigParameterValue(PARAM_ANNOTATE_SENTENCES);
        annotate_partofspeech = (Boolean) aContext.getConfigParameterValue(PARAM_ANNOTATE_PARTOFSPEECH);
        sentModelPath = (String) aContext.getConfigParameterValue(PARAM_SENTSEGMODEL_PATH);
        wordModelPath = (String) aContext.getConfigParameterValue(PARAM_WORDSEGMODEL_PATH);
        posModelPath = (String) aContext.getConfigParameterValue(PARAM_POSMODEL_PATH);

        if (sentModelPath != null)
            if (!vnSenSegmenter.init(sentModelPath)) {
                Logger.printError(component, "Error initializing the sentence segmenter model: " + sentModelPath);
                System.exit(-1);
            }

        if (wordModelPath != null)
            try {
                vnSegmenter.init(wordModelPath);
            } catch (Exception e) {
                Logger.printError(component, "Error initializing the word segmenter model: " + wordModelPath);
                System.exit(-1);
            }

        if (posModelPath != null)
            try {
                dataTagger.addContextGenerator(new POSContextGenerator(posModelPath + File.separator + "featuretemplate.xml"));
                classifier = new Classification(posModelPath);
            } catch (Exception e) {
                Logger.printError(component, "Error initializing the POS tagging model: " + posModelPath);
                System.exit(-1);
            }
    }

    /**
     * Method that gets called to process the documents' cas objects
     */
    public void process(JCas jcas) throws AnalysisEngineProcessException {
        CompositeUnicode2Unicode convertor = new CompositeUnicode2Unicode();
        String origText = jcas.getDocumentText();
        VietTokenizer vietTokenizer = new VietTokenizer("tokenizer.properties");

        final String convertedText = convertor.convert(origText);
        vietTokenizer.turnOnSentenceDetection();
        final String senSegmentedText = vnSenSegmenter.senSegment(convertedText).trim();
//        final String senSegmentedText = vietTokenizer.segment(convertedText).trim();
//
//        final String tokenizedText = PennTokenizer.tokenize(senSegmentedText).trim();

//		final String segmentedText = vnSegmenter.segmenting(tokenizedText);
        //use vn tagger
        final String segmentedText = vietTokenizer.segment(origText);


//        final String postProcessedString = (new JVnTextPro()).postProcessing(segmentedText).trim();
        final String postProcessedString = segmentedText;

//        List<Sentence> posSentences = jvnTagging(postProcessedString);
//        LinkedList<TWord> posWords = new LinkedList<TWord>();
//        for (jvntextpro.data.Sentence sent : posSentences)
//            for (Integer i = 0; i < sent.size(); ++i)
//                posWords.add(sent.getTWordAt(i));



		/*
         * annotate sentences
		 */
        if (annotate_sentences) {
            Integer offset = 0;
//            String[] sentences = senSegmentedText.split("\n");
            String[] sentences = null;

//            SentenceDetector sentenceDetector = SentenceDetectorFactory.create("vietnamese");
            try {
                SentenceDetector sentenceDetector = new SentenceDetector("/home/tienbm90/IdeaProjects/v2/heideltime/models/sentDetection/VietnameseSD.bin.gz");
                sentences = sentenceDetector.detectSentences(new StringReader(origText));
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (String sentence : sentences) {
                de.unihd.dbs.uima.types.heideltime.Sentence s = new de.unihd.dbs.uima.types.heideltime.Sentence(jcas);
                sentence = sentence.trim();
                Integer sentOffset = origText.indexOf(sentence, offset);

                if (sentOffset >= 0) {
                    s.setBegin(sentOffset);
                    offset = sentOffset + sentence.length();
                    s.setEnd(offset);
                    s.addToIndexes();
                } else {
                    sentence = sentence.substring(0, sentence.length() - 1).trim();
                    sentOffset = origText.indexOf(sentence, offset);
                    if (sentOffset >= 0) {
                        s.setBegin(sentOffset);
                        offset = sentOffset + sentence.length();
                        s.setEnd(offset);
                        s.addToIndexes();
                    } else {
                        System.err.println("Sentence \"" + sentence + "\" was not found in the original text.");
                    }
                }
            }
        }

		/*
         * annotate tokens
		 */
        if (annotate_tokens) {
            Integer offset = 0;
            String[] tokens = postProcessedString.split("\\s+");
            for (Integer i = 0; i < tokens.length; ++i) {
                final String token = tokens[i].trim();
                String thisPosTag = null;

                Integer tokenOffset = origText.indexOf(token, offset);

                Token t = new Token(jcas);

                if (tokenOffset >= 0) {
                    /*
                     * first, try to find the string in the form the tokenizer returned it
					 */
                    t.setBegin(tokenOffset);
                    offset = tokenOffset + token.length();
                    t.setEnd(offset);
                    t.addToIndexes();
                } else {
                    /*
					 * straight up token not found.
					 * assume that it is a compound word (e.g. some_thing)
					 * and try to find it in the original text again; first using
					 * a "_" -> " " replacement, then try just removing the underscore.
					 */
                    String underscoreToSpaceToken = token.replaceAll("_", " ");
                    Integer spaceOffset = origText.indexOf(underscoreToSpaceToken, offset);
                    String underscoreRemovedToken = token.replaceAll("_", "");
                    Integer removedOffset = origText.indexOf(underscoreRemovedToken, offset);

					/*
					 * offsets are the same. can't think of a good example where this could
					 * possibly happen, but maybe there is one.
					 */
                    if (removedOffset >= 0 && spaceOffset >= 0) {
                        if (removedOffset >= spaceOffset) {
                            t.setBegin(spaceOffset);
                            offset = spaceOffset + underscoreToSpaceToken.length();
                            t.setEnd(offset);
                            t.addToIndexes();
                        } else {
                            t.setBegin(removedOffset);
                            offset = removedOffset + underscoreRemovedToken.length();
                            t.setEnd(offset);
                        }
                    }
					/*
					 * underscore removed was found, underscore replaced to space was not
					 */
                    else if (removedOffset >= 0 && spaceOffset == -1) {
                        t.setBegin(removedOffset);
                        offset = removedOffset + underscoreRemovedToken.length();
                        t.setEnd(offset);

                        t.addToIndexes();
                    }
					/*
					 * underscore removed was not found, underscore replaced was found
					 */
                    else if (removedOffset == -1 && spaceOffset >= 0) {
                        t.setBegin(spaceOffset);
                        offset = spaceOffset + underscoreToSpaceToken.length();
                        t.setEnd(offset);

                        t.addToIndexes();
                    }
					/*
					 * there is no hope of finding this token
					 */
                    else {
                        System.err.println("Token \"" + token + "\" was not found in the original text.");
                    }
                }
            }
        }
    }


    /**
     * Taken from the JVnTextPro package and adapted to not output a string
     *
     * @param instr input string to be tagged
     * @return tagged text
     */
    public List<jvntextpro.data.Sentence> jvnTagging(String instr) {
        List<jvntextpro.data.Sentence> data = reader.readString(instr);
        for (int i = 0; i < data.size(); ++i) {

            jvntextpro.data.Sentence sent = data.get(i);
            for (int j = 0; j < sent.size(); ++j) {
                String[] cps = dataTagger.getContext(sent, j);
                String label = classifier.classify(cps);

                if (label.equalsIgnoreCase("Mrk")) {
                    if (StringUtils.isPunc(sent.getWordAt(j)))
                        label = sent.getWordAt(j);
                    else label = "X";
                }

                sent.getTWordAt(j).setTag(label);
            }
        }

        return data;
    }
}
