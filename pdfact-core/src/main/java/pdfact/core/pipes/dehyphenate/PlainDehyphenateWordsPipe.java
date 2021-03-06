package pdfact.core.pipes.dehyphenate;

import static pdfact.core.util.lexicon.CharacterLexicon.HYPHENS;
import static pdfact.core.util.lexicon.CharacterLexicon.LETTERS;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import pdfact.core.model.Character;
import pdfact.core.model.Paragraph;
import pdfact.core.model.PdfDocument;
import pdfact.core.model.Word;
import pdfact.core.util.PdfActUtils;
import pdfact.core.util.counter.ObjectCounter;
import pdfact.core.util.counter.ObjectCounter.ObjectCounterFactory;
import pdfact.core.util.exception.PdfActException;
import pdfact.core.util.list.ElementList;
import pdfact.core.util.list.ElementList.ElementListFactory;
import pdfact.core.util.log.InjectLogger;
import pdfact.core.util.normalize.WordNormalizer;
import pdfact.core.util.normalize.WordNormalizer.WordNormalizerFactory;

/**
 * A plain implementation of {@link DehyphenateWordsPipe}.
 * 
 * @author Claudius Korzen
 */
public class PlainDehyphenateWordsPipe implements DehyphenateWordsPipe {
  /**
   * The logger.
   */
  @InjectLogger
  protected static Logger log;

  /**
   * The factory to create lists of characters.
   */
  protected ElementListFactory<Character> characterListFactory;

  /**
   * The factory to create lists of words.
   */
  protected ElementListFactory<Word> wordListFactory;

  /**
   * The factory to create instances of {@link WordNormalizer}.
   */
  protected WordNormalizer wordNormalizer;

  /**
   * The index of all words which do not include a hyphen (normal words).
   */
  protected ObjectCounter<String> normalWordsIndex;

  /**
   * The index of all words with an inner hyphen (compound words).
   */
  protected ObjectCounter<String> compoundWordsIndex;

  /**
   * The index of all prefixes of compound words.
   */
  protected ObjectCounter<String> prefixesIndex;

  /**
   * The total number of words in the PDF document.
   */
  protected int numWords;

  /**
   * The total number of processed words on dehyphenation.
   */
  protected int numProcessedWords;

  /**
   * The total number of dehyphenated words.
   */
  protected int numDehyphenatedWords;

  /**
   * The number of dehyphenated words, which resulted in normal words.
   */
  protected int numNormalWords;

  /**
   * The number of dehyphenated words, which resulted in compound words.
   */
  protected int numCompoundWords;

  /**
   * Creates a new pipe that dehyphenates words.
   * 
   * @param characterListFactory
   *        The factory to create lists of characters.
   * @param wordListFactory
   *        The factory to create lists of words.
   * @param wordNormalizerFactory
   *        The factory to create instances of {@link WordNormalizer}.
   * @param objectCounterFactory
   *        The factory to create instances of {@link ObjectCounter}.
   */
  @Inject
  public PlainDehyphenateWordsPipe(
      ElementListFactory<Character> characterListFactory,
      ElementListFactory<Word> wordListFactory,
      WordNormalizerFactory wordNormalizerFactory,
      ObjectCounterFactory<String> objectCounterFactory) {
    this.characterListFactory = characterListFactory;
    this.wordListFactory = wordListFactory;
    this.normalWordsIndex = objectCounterFactory.create();
    this.compoundWordsIndex = objectCounterFactory.create();
    this.prefixesIndex = objectCounterFactory.create();

    this.wordNormalizer = wordNormalizerFactory.create();
    this.wordNormalizer.setIsToLowerCase(true);
    this.wordNormalizer.setLeadingCharactersToKeep(LETTERS, HYPHENS);
    this.wordNormalizer.setTrailingCharactersToKeep(LETTERS, HYPHENS);
  }

  // ==========================================================================

  @Override
  public PdfDocument execute(PdfDocument pdf) throws PdfActException {
    log.debug("Start of pipe: " + getClass().getSimpleName() + ".");

    log.debug("Preprocess: Counting words.");
    countWords(pdf);

    log.debug("Counting words done.");
    log.debug("# words               : " + this.numWords);
    log.debug("# uniq. normal words  : " + this.normalWordsIndex.size());
    log.debug("# uniq. compound words: " + this.compoundWordsIndex.size());
    log.debug("# uniq. prefixes      : " + this.prefixesIndex.size());

    log.debug("Process: Dehyphenating words.");
    dehyphenate(pdf);

    log.debug("Dehyphenating words done.");
    log.debug("# processed words   : " + this.numProcessedWords);
    log.debug("# dehyphenated words: " + this.numDehyphenatedWords);
    log.debug("> to normal words   : " + this.numNormalWords);
    log.debug("> to compound words : " + this.numCompoundWords);

    log.debug("End of pipe: " + getClass().getSimpleName() + ".");
    return pdf;
  }

  // ==========================================================================

  /**
   * Counts single, compound and prefixes of compound words.
   * 
   * @param pdf
   *        The PDF document to process.
   */
  protected void countWords(PdfDocument pdf) {
    if (pdf == null) {
      return;
    }

    List<Paragraph> paragraphs = pdf.getParagraphs();
    if (paragraphs == null) {
      return;
    }

    for (Paragraph paragraph : pdf.getParagraphs()) {
      if (paragraph == null) {
        continue;
      }

      List<Word> words = paragraph.getWords();
      if (words == null) {
        continue;
      }

      for (Word word : words) {
        if (word == null) {
          continue;
        }

        this.numWords++;

        // Count normal words, compound words and prefixes of compound words.
        // The prefixes of a compound word are the substrings before each
        // hyphen, e.g. for the compound word "sugar-free", the prefix is
        // "sugar".

        // Normalize the word: Remove leading and trailing punctuation marks
        // (but not hyphens).
        String wordStr = this.wordNormalizer.normalize(word);

        if (wordStr == null || wordStr.isEmpty()) {
          continue;
        }

        // Check if the word contains hyphens.
        TIntList idxsHyphens = PdfActUtils.indexesOf(wordStr, HYPHENS);

        if (idxsHyphens.isEmpty()) {
          // No hyphen was found. The word is a single word.
          this.normalWordsIndex.add(wordStr);
          continue;
        }

        // We are interested only in compound words with inner hyphens.
        if (idxsHyphens.get(0) == 0) {
          // The word starts with an hyphen. Ignore the word.
          continue;
        }

        if (idxsHyphens.get(idxsHyphens.size() - 1) == wordStr.length() - 1) {
          // The word ends with an hyphen. Ignore it.
          continue;
        }

        this.compoundWordsIndex.add(wordStr);

        // Count the prefixes of compound words.
        TIntIterator itr = idxsHyphens.iterator();
        while (itr.hasNext()) {
          this.prefixesIndex.add(wordStr.substring(0, itr.next()));
        }
      }
    }
  }

  // ==========================================================================

  /**
   * Dehyphenates the hyphenated words in the given PDF document.
   * 
   * @param pdf
   *        The PDF document to process.
   */
  protected void dehyphenate(PdfDocument pdf) {
    if (pdf == null) {
      return;
    }

    List<Paragraph> paragraphs = pdf.getParagraphs();
    if (paragraphs == null) {
      return;
    }

    for (Paragraph paragraph : paragraphs) {
      if (paragraph == null) {
        continue;
      }

      ElementList<Word> words = paragraph.getWords();
      if (words == null) {
        continue;
      }

      Iterator<Word> wordItr = words.iterator();
      ElementList<Word> dehyphWords = this.wordListFactory.create(words.size());

      while (wordItr.hasNext()) {
        Word word = wordItr.next();

        this.numProcessedWords++;

        if (word == null) {
          continue;
        }

        if (!word.isHyphenated()) {
          dehyphWords.add(word);
          continue;
        }

        Word nextWord = wordItr.hasNext() ? wordItr.next() : null;
        if (nextWord != null) {
          dehyphWords.add(dehyphenate(word, nextWord));
          this.numProcessedWords++;
        } else {
          dehyphWords.add(word);
        }
      }

      paragraph.setWords(dehyphWords);
      paragraph.setText(PdfActUtils.join(dehyphWords, " "));
    }
  }

  /**
   * Dehyphenates the two given words.
   * 
   * @param word1
   *        The first word to process.
   * @param word2
   *        The second word to process.
   * 
   * @return The dehyphenated word.
   */
  public Word dehyphenate(Word word1, Word word2) {
    if (word1 == null) {
      return null;
    }

    if (word2 == null) {
      return word1;
    }

    ElementList<Character> chars1 = word1.getCharacters();
    ElementList<Character> chars2 = word2.getCharacters();
    ElementList<Character> mergedChars = this.characterListFactory.create();

    if (isHyphenMandatory(word1, word2)) {
      mergedChars.addAll(chars1);
      this.numCompoundWords++;
    } else {
      mergedChars.addAll(chars1.subList(0, chars1.size() - 1));
      this.numNormalWords++;
    }
    this.numDehyphenatedWords++;

    mergedChars.addAll(chars2);
    word1.setCharacters(chars2);

    word1.addPositions(word2.getPositions());
    word1.setIsHyphenated(false);
    word1.setIsDehyphenated(true);
    word1.setText(PdfActUtils.join(mergedChars, ""));

    return word1;
  }

  /**
   * Returns true if the hyphen between the two given words is mandatory on.
   * 
   * @param word1
   *        The first word (the part before the hyphen).
   * @param word2
   *        The second word (the part behind the hyphen).
   * 
   * @return True if we have to ignore the hyphen between the two given words;
   *         False otherwise.
   */
  protected boolean isHyphenMandatory(Word word1, Word word2) {
    String word1Str = this.wordNormalizer.normalize(word1);
    String word2Str = this.wordNormalizer.normalize(word2);

    // TODO: Use the word normalizer.
    String prefix = word1Str.replaceAll("[-]$", "");
    String withHyphen = word1Str + word2Str;
    String withoutHyphen = prefix + word2Str;

    int singleWordFreq = this.normalWordsIndex.getFrequency(withoutHyphen);
    int compoundWordFreq = this.compoundWordsIndex.getFrequency(withHyphen);
    int compoundWordPrefixFreq = this.prefixesIndex.getFrequency(prefix);

    if (compoundWordFreq != singleWordFreq) {
      return compoundWordFreq > singleWordFreq;
    }

    return compoundWordPrefixFreq > 0;
  }
}
