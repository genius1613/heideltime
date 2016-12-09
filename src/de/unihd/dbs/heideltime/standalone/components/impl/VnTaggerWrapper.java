package de.unihd.dbs.heideltime.standalone.components.impl;

import de.unihd.dbs.heideltime.standalone.components.PartOfSpeechTagger;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;

import java.util.Properties;

/**
 * Created by tienbm90 on 26/11/2016.
 */
public class VnTaggerWrapper implements PartOfSpeechTagger {

    private de.unihd.dbs.uima.annotator.vntaggerwrapper.VnTaggerWrapper vnTaggerWrapper =
            new de.unihd.dbs.uima.annotator.vntaggerwrapper.VnTaggerWrapper();

    @Override
    public void initialize(Properties settings) {
        StandaloneConfigContext aContext = new StandaloneConfigContext();

        // construct a context for the uima engine
        aContext.setConfigParameterValue(de.unihd.dbs.uima.annotator.jvntextprowrapper.JVnTextProWrapper.PARAM_ANNOTATE_TOKENS,
                (Boolean) settings.get(JVNTEXTPRO_ANNOTATE_TOKENS));
        aContext.setConfigParameterValue(de.unihd.dbs.uima.annotator.jvntextprowrapper.JVnTextProWrapper.PARAM_ANNOTATE_SENTENCES,
                (Boolean) settings.get(JVNTEXTPRO_ANNOTATE_SENTENCES));
        aContext.setConfigParameterValue(de.unihd.dbs.uima.annotator.jvntextprowrapper.JVnTextProWrapper.PARAM_ANNOTATE_PARTOFSPEECH,
                (Boolean) settings.get(JVNTEXTPRO_ANNOTATE_POS));
        aContext.setConfigParameterValue(de.unihd.dbs.uima.annotator.jvntextprowrapper.JVnTextProWrapper.PARAM_WORDSEGMODEL_PATH,
                (String) settings.get(JVNTEXTPRO_WORD_MODEL_PATH));
        aContext.setConfigParameterValue(de.unihd.dbs.uima.annotator.jvntextprowrapper.JVnTextProWrapper.PARAM_SENTSEGMODEL_PATH,
                (String) settings.get(JVNTEXTPRO_SENT_MODEL_PATH));
        aContext.setConfigParameterValue(de.unihd.dbs.uima.annotator.jvntextprowrapper.JVnTextProWrapper.PARAM_POSMODEL_PATH,
                (String) settings.get(JVNTEXTPRO_POS_MODEL_PATH));

        vnTaggerWrapper.initialize(aContext);
    }

    @Override
    public void process(JCas jCas) {
        try {
            vnTaggerWrapper.process(jCas);
        } catch (AnalysisEngineProcessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void reset() {

    }
}
