package pdfact.pipes;

import static pdfact.PdfActSettings.DEFAULT_SEMANTIC_ROLES_TO_INCLUDE;
import static pdfact.PdfActSettings.DEFAULT_SERIALIZATION_FORMAT;
import static pdfact.PdfActSettings.DEFAULT_TEXT_UNITS_TO_INCLUDE;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Set;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

import pdfact.PdfActCoreSettings;
import pdfact.model.PdfDocument;
import pdfact.model.ElementType;
import pdfact.model.LogLevel;
import pdfact.model.SemanticRole;
import pdfact.model.PdfSerializationFormat;
import pdfact.pipes.PdfActCorePipe.PdfActCorePipeFactory;
import pdfact.pipes.serialize.SerializePdfPipe;
import pdfact.pipes.serialize.SerializePdfPipe.SerializePdfPipeFactory;
import pdfact.pipes.validate.ValidatePathToWritePipe;
import pdfact.pipes.validate.ValidatePathToWritePipe.ValidatePathToWritePipeFactory;
import pdfact.pipes.visualize.VisualizePdfPipe;
import pdfact.pipes.visualize.VisualizePdfPipe.VisualizePdfPipeFactory;
import pdfact.util.exception.PdfActException;
import pdfact.util.pipeline.Pipeline;
import pdfact.util.pipeline.Pipeline.PdfActPipelineFactory;

/**
 * A plain implementation of {@link PdfActServicePipe}.
 * 
 * @author Claudius Korzen
 */
public class PlainPdfActServicePipe implements PdfActServicePipe {
  /**
   * The logger.
   */
  protected static final Logger LOG = Logger.getLogger(PdfActServicePipe.class);

  /**
   * The logger level.
   */
  protected LogLevel logLevel = PdfActCoreSettings.DEFAULT_LOG_LEVEL;

  // ==========================================================================

  /**
   * The factory to create instance of {@link Pipeline}.
   */
  protected PdfActPipelineFactory pipelineFactory;

  /**
   * The factory to create instances of {@link PdfActCorePipe}.
   */
  protected PdfActCorePipeFactory pdfActCoreFactory;

  /**
   * The factory to create instances of {@link ValidatePathToWritePipe}.
   */
  protected ValidatePathToWritePipeFactory validatePathPipeFactory;

  /**
   * The factory to create instances of {@link SerializePdfPipe}.
   */
  protected SerializePdfPipeFactory serializePdfPipeFactory;

  /**
   * The factory to create instances of {@link VisualizePdfPipe}.
   */
  protected VisualizePdfPipeFactory visualizePdfPipeFactory;

  // ==========================================================================

  /**
   * The serialization target, given as a file.
   */
  protected Path serializationPath;

  /**
   * The serialization target, given as a stream.
   */
  protected OutputStream serializationStream;

  // ==========================================================================

  /**
   * The visualization target, given as a file.
   */
  protected Path visualizationPath;

  /**
   * The visualization target, given as a stream.
   */
  protected OutputStream visualizationStream;

  // ==========================================================================

  /**
   * The serialization format.
   */
  protected PdfSerializationFormat serializationFormat;

  /**
   * The text units to be included in serialization and visualization.
   */
  protected Set<ElementType> types;

  /**
   * The roles of text units to be included in serialization and visualization.
   */
  protected Set<SemanticRole> roles;

  // ==========================================================================

  /**
   * The default constructor.
   * 
   * @param pipelineFactory
   *        The factory to create instance of {@link Pipeline}
   * @param pdfActCorePipeFactory
   *        The factory to create instances of {@link PdfActCorePipe}
   * @param validatePathFactory
   *        The factory to create instances of {@link ValidatePathToWritePipe}
   * @param serializePdfPipeFactory
   *        The factory to create instances of {@link SerializePdfPipe}
   * @param visualizePdfPipeFactory
   *        The factory to create instances of {@link VisualizePdfPipe}
   */
  @Inject
  public PlainPdfActServicePipe(PdfActPipelineFactory pipelineFactory,
      PdfActCorePipeFactory pdfActCorePipeFactory,
      ValidatePathToWritePipeFactory validatePathFactory,
      SerializePdfPipeFactory serializePdfPipeFactory,
      VisualizePdfPipeFactory visualizePdfPipeFactory) {
    this.pipelineFactory = pipelineFactory;
    this.pdfActCoreFactory = pdfActCorePipeFactory;
    this.validatePathPipeFactory = validatePathFactory;
    this.serializePdfPipeFactory = serializePdfPipeFactory;
    this.visualizePdfPipeFactory = visualizePdfPipeFactory;
    this.serializationFormat = DEFAULT_SERIALIZATION_FORMAT;
    this.types = DEFAULT_TEXT_UNITS_TO_INCLUDE;
    this.roles = DEFAULT_SEMANTIC_ROLES_TO_INCLUDE;
  }

  // ==========================================================================

  /**
   * Processes the given PDF document.
   * 
   * @param pdf
   *        The PDF document to process.
   * 
   * @return The PDF document after processing.
   * 
   * @throws PdfActException
   *         If something went wrong on processing the PDF document.
   */
  public PdfDocument execute(PdfDocument pdf) throws PdfActException {
    Pipeline pipeline = this.pipelineFactory.create();

    // Parse the PDF document.
    pipeline.addPipe(this.pdfActCoreFactory.create());

    // Validate the target path for the serialization if there is any given.
    if (this.serializationPath != null) {
      ValidatePathToWritePipe valPipe = this.validatePathPipeFactory.create();
      valPipe.setPath(this.serializationPath);
      pipeline.addPipe(valPipe);
    }
    
    // Serialize if there is a target given for the serialization.
    if (this.serializationStream != null || this.serializationPath != null) {
      SerializePdfPipe serializePipe = this.serializePdfPipeFactory.create();
      serializePipe.setSerializationFormat(this.serializationFormat);
      serializePipe.setElementTypesFilters(this.types);
      serializePipe.setSemanticRolesFilters(this.roles);
      serializePipe.setTargetPath(this.serializationPath);
      serializePipe.setTargetStream(this.serializationStream);
      pipeline.addPipe(serializePipe);
    }

    // Validate the target path for the visualization if there is any given.
    if (this.visualizationPath != null) {
      ValidatePathToWritePipe valPipe = this.validatePathPipeFactory.create();
      valPipe.setPath(this.visualizationPath);
      pipeline.addPipe(valPipe);
    }
    
    // Visualize if there is a target given for the visualization.
    if (this.visualizationStream != null || this.visualizationPath != null) {
      VisualizePdfPipe visualizePipe = this.visualizePdfPipeFactory.create();
      visualizePipe.setElementTypesFilters(this.types);
      visualizePipe.setSemanticRolesFilters(this.roles);
      visualizePipe.setTargetPath(this.visualizationPath);
      visualizePipe.setTargetStream(this.visualizationStream);
      pipeline.addPipe(visualizePipe);
    }

    return pipeline.process(pdf);
  }

  // ==========================================================================

  @Override
  public LogLevel getLogLevel() {
    return this.logLevel;
  }

  @Override
  public boolean hasLogLevel(LogLevel level) {
    return this.logLevel != null && this.logLevel.implies(level);
  }

  @Override
  public void setLogLevel(LogLevel logLevel) {
    this.logLevel = logLevel;
  }

  // ==========================================================================

  @Override
  public Path getSerializationPath() {
    return this.serializationPath;
  }

  @Override
  public void setSerializationPath(Path path) {
    this.serializationPath = path;
  }

  // ==========================================================================

  @Override
  public OutputStream getSerializationStream() {
    return this.serializationStream;
  }

  @Override
  public void setSerializationStream(OutputStream stream) {
    this.serializationStream = stream;
  }

  // ==========================================================================

  @Override
  public PdfSerializationFormat getSerializationFormat() {
    return this.serializationFormat;
  }

  @Override
  public void setSerializationFormat(PdfSerializationFormat format) {
    this.serializationFormat = format;
  }

  // ==========================================================================

  @Override
  public Path getVisualizationPath() {
    return this.visualizationPath;
  }

  @Override
  public void setVisualizationPath(Path path) {
    this.visualizationPath = path;
  }

  // ==========================================================================

  @Override
  public OutputStream getVisualizationStream() {
    return this.visualizationStream;
  }

  @Override
  public void setVisualizationStream(OutputStream stream) {
    this.visualizationStream = stream;
  }

  // ==========================================================================

  @Override
  public Set<SemanticRole> getSemanticRolesFilters() {
    return this.roles;
  }

  @Override
  public void setSemanticRolesFilters(Set<SemanticRole> filter) {
    this.roles = filter;
  }

  // ==========================================================================

  @Override
  public Set<ElementType> getElementTypesFilters() {
    return this.types;
  }

  @Override
  public void setElementTypesFilters(Set<ElementType> filter) {
    this.types = filter;
  }
}