package com.example.radiobook;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreQuote;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.example.radiobook.Speaker.Gender.*;

public class QuotePreprocessor {

    private CoreDocument document;
    private Annotation annotation;
    private int lambda = 3; // lambda an estimation of a phrase about 3 or more words
    private boolean isTag = false;
    private String defaultSpeaker = "narrator";
    private final String TOKENIZER_REGEX = " ";
    public static ArrayList<Quote> quoteList = new ArrayList<>();
    private static HashMap<String, Speaker> speakerMap = new HashMap<>();

    public QuotePreprocessor(CoreDocument coreDocument) {
        this.document = coreDocument;
    }

    public QuotePreprocessor(Annotation annotation) {
        this.annotation = annotation;
    }

    // Classification of quotes
    //
    public Quote[] classify() {
        String currentSentence = "", prevSpeaker = "", currentSpeaker;
        int prevQuoteLastIdx = 0, lastQuoteSentenceIdx = 0, prevSentenceIdx = 0, currentSentenceIdx = 0, quoteStartingIdx, quoteLength;
        List<CoreQuote> quotes = document.quotes();
        for (CoreQuote quote : quotes) {
            currentSentence = quote.sentences().get(0).text();
            currentSentenceIdx = quote.sentences().get(0).coreMap().get(CoreAnnotations.SentenceIndexAnnotation.class);
            quoteStartingIdx = getQuoteStartingIdx(currentSentence, quote.text());

            if (quote.hasSpeaker) {
                currentSpeaker = quote.speaker().get();
                speakerMapAnnotator(quote.speakerTokens().get().get(0));
            } else if (quote.hasCanonicalSpeaker) {
                currentSpeaker = quote.canonicalSpeaker().get();
                speakerMapAnnotator(quote.canonicalSpeakerTokens().get().get(0));
            } else {
                currentSpeaker = defaultSpeaker;
            }
            quoteLength = quoteStartingIdx + quote.text().length();

            if ((currentSentenceIdx - prevSentenceIdx) != 1 || quoteStartingIdx != 0) {
                createQuote(prevSentenceIdx, currentSentenceIdx, prevQuoteLastIdx, quoteStartingIdx, prevSpeaker, false);
            }

            if (quote.sentences().size() > 1) {
                int noOfQuoteSentences = quote.sentences().size();
                createQuote(currentSentenceIdx, currentSentenceIdx + noOfQuoteSentences - 1, quoteStartingIdx, quoteLength, currentSpeaker, true);
            } else {
                createQuote(currentSentenceIdx, currentSentenceIdx, quoteStartingIdx, quoteLength, currentSpeaker, true);
            }

            lastQuoteSentenceIdx = currentSentenceIdx + quote.sentences().size() - 1;
            prevSentenceIdx = currentSentenceIdx;
            prevQuoteLastIdx = quoteStartingIdx + quote.text().length();
            prevSpeaker = currentSpeaker;
        }
        int lastDocumentSentenceIdx = document.sentences().size() - 1;
        if (lastQuoteSentenceIdx < lastDocumentSentenceIdx) {
            createQuote(currentSentenceIdx, lastDocumentSentenceIdx, prevQuoteLastIdx, 0, defaultSpeaker, false);
        }
        return null;
    }

    // Checks whether a quote is a tag and if so returns true otherwise false
    // Sentence :
    // tagStartingIdx:
    // tagEndingIdx:
    private boolean isQuoteTag(String sentence) {
        sentence = StringUtils.strip(sentence);
        String[] words = sentence.split(TOKENIZER_REGEX);
        return words.length <= lambda;
    }

    // Create a quote
    // startingSentenceIdx: index of the Quote starting sentence
    // endingSentenceIdx: index of the Quote ending sentence
    // startingQuoteIdx: starting index of the Quote in starting sentence
    // endingQuoteIdx: last index of the Quote in ending sentence
    // quote
    // speaker: quote’s speaker
    // tag: is a quote attribution tag
    private void createQuote(int startingSentenceIdx, int endingSentenceIdx, int startingQuoteIdx, int endingQuoteIdx, String speaker, boolean isQuote) {
        //String startingSentence = getSentenceByIndex(startingSentenceIdx);
        String endingSentence = getSentenceByIndex(endingSentenceIdx);
        StringBuffer strBuffer = new StringBuffer();

        if (isQuote) {
            if (startingSentenceIdx == endingSentenceIdx) {
                strBuffer.append(endingSentence, startingQuoteIdx, endingQuoteIdx);
            } else {
                StringBuffer tempBuffer = new StringBuffer();
                for (int i = startingSentenceIdx; i <= endingSentenceIdx; i++) {
                    tempBuffer.append(document.sentences().get(i).text()).append(" ");
                }
                strBuffer.append(tempBuffer.substring(startingQuoteIdx));
            }
            quoteList.add(new Quote(strBuffer.toString(), speaker, false));
        } else {
            if (startingSentenceIdx == endingSentenceIdx) {
                strBuffer.append(endingSentence, startingQuoteIdx, endingQuoteIdx);
            } else {
                StringBuffer tempBuffer = new StringBuffer();
                for (int i = startingSentenceIdx; i < endingSentenceIdx; i++) {
                    tempBuffer.append(document.sentences().get(i).text()).append(" ");
                }
                strBuffer.append(tempBuffer.substring(startingQuoteIdx));

                if (endingQuoteIdx > 0)
                    strBuffer.append(endingSentence, 0, endingQuoteIdx);
            }
            String[] sentences = strBuffer.toString().split("\\.");
            for (String sentence : sentences) {
                if (!StringUtils.isBlank(sentence)) {
                    tagAnnotator(sentence, speaker);
                    speaker = isTag ? speaker : defaultSpeaker;
                    quoteList.add(new Quote(sentence, speaker, isTag));
                }
            }
        }
    }

    private Speaker.Gender getGender(String speaker) {
        Speaker tempSpeaker = speakerMap.get(speaker);
        if (tempSpeaker != null) {
            if (tempSpeaker.getUnknownGenderCount() >= (tempSpeaker.getFemaleGenderCount() + tempSpeaker.getMaleGenderCount())) {
                float malePronounFreq = (tempSpeaker.getMalePronounCount() / (float) tempSpeaker.getSpeakerCount());
                float femalePronounFreq = (tempSpeaker.getFemalePronounCount() / (float) tempSpeaker.getSpeakerCount());
                if (malePronounFreq == femalePronounFreq) {
                    return UNKNOWN;
                } else if (malePronounFreq > femalePronounFreq) {
                    return MALE;
                } else if (femalePronounFreq > malePronounFreq){
                    return FEMALE;
                } else {
                    return UNKNOWN;
                }
            } else if (tempSpeaker.getMaleGenderCount() > tempSpeaker.getFemaleGenderCount()) {
                return MALE;
            } else {
                return FEMALE;
            }
        }
        return UNKNOWN;
    }

    private void tagAnnotator(String sentence, String speaker) {
        sentence = StringUtils.strip(sentence);
        String[] words = sentence.split(TOKENIZER_REGEX);
        isTag = words.length <= lambda;

        if (isTag) {
            Speaker tempSpeaker;
            if (speakerMap.containsKey(speaker)) {
                tempSpeaker = speakerMap.get(speaker);
            } else {
                tempSpeaker = new Speaker();
            }

            if (StringUtils.lowerCase(sentence).contains("he")) {
                tempSpeaker.incrementMalePronounCount();
            } else if (StringUtils.lowerCase(sentence).contains("she")) {
                tempSpeaker.incrementFemalePronounCount();
            }
            speakerMap.put(speaker, tempSpeaker);
        }
    }

    private void speakerMapAnnotator(CoreLabel speakerToken) {
        Speaker tempSpeaker;
        String gender;

        if(speakerToken != null)
            gender = speakerToken.get(CoreAnnotations.GenderAnnotation.class);
        else
            gender = UNKNOWN.name();

        if(gender == null)
            gender = UNKNOWN.name();

        if (speakerMap.containsKey(speakerToken.value())) {
            tempSpeaker = speakerMap.get(speakerToken.value());
        } else {
            tempSpeaker = new Speaker();
        }
        tempSpeaker.incrementSpeakerCount();
        switch (gender) {
            case "Female":
                tempSpeaker.incrementFemaleGenderCount();
                break;
            case "Male":
                tempSpeaker.incrementMaleGenderCount();
                break;
            default:
                tempSpeaker.incrementUnknownGenderCount();
                break;
        }
        speakerMap.put(speakerToken.value(), tempSpeaker);
        System.out.println(speakerToken.value() + ": " + gender);
    }

    // Returns the starting index of a quote in a sentence
    private int getQuoteStartingIdx(String sentence, String quote) {
        return Math.max(sentence.indexOf(quote), 0);
    }

    private String getSentenceByIndex(int index) {
        if (document != null)
            return document.sentences().get(index).text();
        else if (annotation != null)
            return new CoreDocument(annotation).sentences().get(index).text();
        else
            return null;
    }

    private Speaker.Gender getPronounGender(String pronoun) {
        if (StringUtils.lowerCase(pronoun).equals("he")) {
            return MALE;
        } else if (StringUtils.lowerCase(pronoun).equals("she")) {
            return FEMALE;
        } else {
            return UNKNOWN;
        }
    }

    public void printSpeakerGenders() {

    }

    public void printQuotes() {
        for (Quote quote : quoteList) {
            if (quote != null) {
                System.out.println("Quote: " + quote.getQuote());
                System.out.println("Speaker: " + quote.getSpeaker());
                System.out.println("Improvised Gender: " + getGender(quote.getSpeaker()).name());
                System.out.println("IsTag: " + quote.isQuoteAttributionTag());
                System.out.println();
            }
        }
    }
}
