package pdfact.pipes.tokenize.paragraphs;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import pdfact.model.PdfDocument;
import pdfact.model.Character;
import pdfact.model.Page;
import pdfact.model.Paragraph;
import pdfact.model.Paragraph.ParagraphFactory;
import pdfact.util.CharacterUtils;
import pdfact.util.CollectionUtils;
import pdfact.util.exception.PdfActException;
import pdfact.model.Position;
import pdfact.model.SemanticRole;
import pdfact.model.TextBlock;
import pdfact.model.TextLine;
import pdfact.model.Word;

/**
 * A plain implementation of {@link DetectParagraphsPipe}.
 * 
 * @author Claudius Korzen
 */
public class PlainDetectParagraphsPipe implements DetectParagraphsPipe {
  /**
   * The factory to create instances of {@link Paragraph}.
   */
  protected ParagraphFactory paragraphFactory;

  /**
   * The default constructor.
   * 
   * @param paragraphFactory
   *        The factory to create instances of {@link Paragraph}.
   */
  @Inject
  public PlainDetectParagraphsPipe(ParagraphFactory paragraphFactory) {
    this.paragraphFactory = paragraphFactory;
  }

  @Override
  public PdfDocument execute(PdfDocument pdf) throws PdfActException {
    List<Paragraph> paragraphs = new ArrayList<>();

    // Segment the PDF document into paragraphs.
    List<List<TextBlock>> segments = segmentIntoParagraphs(pdf);

    // Create the PdfParagraph objects.
    for (List<TextBlock> segment : segments) {
      Paragraph paragraph = this.paragraphFactory.create();
      for (TextBlock block : segment) {
        for (TextLine line : block.getTextLines()) {
          paragraph.addWords(line.getWords());
        }
      }
      paragraph.setText(computeText(paragraph));
      paragraph.setPositions(computePositions(segment));
      paragraph.setSemanticRole(computeRole(segment));
      // paragraph.setCharacterStatistic(computeCharacterStatistics(paragraph));
      // paragraph.setTextLineStatistic(computeTextLineStatistics(paragraph));
      paragraphs.add(paragraph);
    }
    
    pdf.setParagraphs(paragraphs);

    return pdf;
  }

  // ==========================================================================

  /**
   * Segments the given PDF document into paragraphs.
   * 
   * @param pdf
   *        The PDF document to process.
   * 
   * @return The list of list of text blocks of a paragraph.
   */
  protected List<List<TextBlock>> segmentIntoParagraphs(PdfDocument pdf) {
    List<List<TextBlock>> result = new ArrayList<>();

    // Put all blocks to a single list to be able to iterate them in one go.
    List<TextBlock> allTextBlocks = new ArrayList<>();
    for (Page page : pdf.getPages()) {
      allTextBlocks.addAll(page.getTextBlocks());
    }
    
    TIntSet indexesOfAlreadyProcessedBlocks = new TIntHashSet();

    // Identify the paragraphs from the text blocks.
    for (int i = 0; i < allTextBlocks.size(); i++) {
      TextBlock block = allTextBlocks.get(i);

      if (indexesOfAlreadyProcessedBlocks.contains(i)) {
        // The block was already added to a paragraph. Ignore it.
        continue;
      }

      // Create a new paragraph.
      List<TextBlock> paragraphBlocks = new ArrayList<>();
      paragraphBlocks.add(block);
      indexesOfAlreadyProcessedBlocks.add(i);

      // If the role of the block is "body text", check if there is another
      // block in the remaining blocks that belongs to the same paragraph.
      if (block.getSemanticRole() == SemanticRole.BODY_TEXT) {
        for (int j = i + 1; j < allTextBlocks.size(); j++) {
          TextBlock otherBlock = allTextBlocks.get(j);
          if (otherBlock.getSemanticRole() == SemanticRole.HEADING) {
            break;
          }
          if (otherBlock.getSemanticRole() == SemanticRole.BODY_TEXT) {
            if (!belongsToParagraph(otherBlock, paragraphBlocks)) {
              break;
            }
            // Add the block to the existing paragraph.
            paragraphBlocks.add(otherBlock);
            indexesOfAlreadyProcessedBlocks.add(j);
          }
        }
      }
      result.add(paragraphBlocks);
    }
    return result;
  }

  // ==========================================================================

  /**
   * Computes the text for the given paragraph.
   * 
   * @param p
   *        The paragraph to process.
   * @return The text for the given paragraph.
   */
  protected String computeText(Paragraph p) {
    // TODO: Dehyphenate
    return CollectionUtils.join(p.getWords(), " ");
  }

  /**
   * Computes the positions for the given paragraph.
   * 
   * @param blocks
   *        The blocks of the paragraph to process.
   * 
   * @return The positions for the given paragraph.
   */
  protected List<Position> computePositions(List<TextBlock> blocks) {
    List<Position> positions = new ArrayList<>();
    for (TextBlock block : blocks) {
      positions.add(block.getPosition());
    }
    return positions;
  }

  /**
   * Computes the role for the given paragraph.
   * 
   * @param blocks
   *        The blocks of the paragraph to process.
   * 
   * @return The role for the given paragraph.
   */
  protected SemanticRole computeRole(List<TextBlock> blocks) {
    if (blocks == null || blocks.isEmpty()) {
      return null;
    }
    // Return the role of the first text block.
    return blocks.get(0).getSemanticRole();
  }

  /**
   * Checks, if the given text block belongs to the given paragraph.
   * 
   * @param block
   *        The text block to process.
   * @param paraBlocks
   *        The paragraph to process.
   * 
   * @return True, if the given text block should be added to the paragraph.
   */
  protected boolean belongsToParagraph(TextBlock block,
      List<TextBlock> paraBlocks) {
    if (block == null || paraBlocks == null) {
      return false;
    }

    if (paraBlocks.isEmpty()) {
      return false;
    }

    TextBlock lastParaBlock = paraBlocks.get(paraBlocks.size() - 1);

    // The block belongs to the paragraph, if the paragraph doesn't end with
    // a punctuation mark.
    Word word = lastParaBlock.getLastTextLine().getLastWord();
    Character lastChar = word != null ? word.getLastCharacter() : null;
    if (!CharacterUtils.isPunctuationMark(lastChar)) {
      return true;
    }

    // The block belongs to the paragraph, if the block starts with an
    // lowercased letter.
    Word firstWord = block.getFirstTextLine().getFirstWord();
    if (CharacterUtils.isLowercase(firstWord.getFirstCharacter())) {
      return true;
    }

    return false;
  }
}