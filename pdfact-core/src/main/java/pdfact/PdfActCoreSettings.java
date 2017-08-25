package pdfact;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import pdfact.model.LogLevel;
import pdfact.pipes.parse.stream.pdfbox.convert.PDFontConverter;

/**
 * Some global settings to control the behavior of PdfAct.
 * 
 * @author Claudius Korzen
 */
public class PdfActCoreSettings {
  // ==========================================================================
  // General settings.

  /**
   * The default character encoding.
   */
  public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

  // ==========================================================================
  // Log settings.

  /**
   * The default log level.
   */
  public static final LogLevel DEFAULT_LOG_LEVEL = LogLevel.OFF;

  /**
   * The number of decimal places for floating numbers.
   */
  public static final int FLOATING_NUMBER_PRECISION = 1;

  // ==========================================================================

  /**
   * The path to the AFM file (used in {@link PDFontConverter}.
   */
  public static final String AFM_FILE_PATH = "afm.map";

  /**
   * The field delimiter in the AFM file.
   */
  public static final String AFM_FILE_FIELD_DELIMITER = "\n";
}
