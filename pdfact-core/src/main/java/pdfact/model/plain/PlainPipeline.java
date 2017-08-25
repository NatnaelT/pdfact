package pdfact.model.plain;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.assistedinject.AssistedInject;

import pdfact.model.PdfDocument;
import pdfact.util.exception.PdfActException;
import pdfact.util.pipeline.Pipe;
import pdfact.util.pipeline.Pipeline;

/**
 * A plain implementation of {@link Pipeline}.
 * 
 * @author Claudius Korzen
 */
public class PlainPipeline implements Pipeline {
  /**
   * The registered pipes.
   */
  protected List<Pipe> pipes;

  /**
   * Creates an empty pipeline.
   */
  @AssistedInject
  public PlainPipeline() {
    this.pipes = new ArrayList<>();
  }

  // ==========================================================================

  @Override
  public PdfDocument process(PdfDocument pdf) throws PdfActException {
    PdfDocument processed = pdf;
    for (Pipe pipe : this.pipes) {
      processed = pipe.execute(processed);
    }
    return processed;
  }

  // ==========================================================================

  @Override
  public List<Pipe> getPipes() {
    return this.pipes;
  }

  @Override
  public void setPipes(List<Pipe> pipes) {
    this.pipes = pipes;
  }

  @Override
  public void addPipes(List<Pipe> pipes) {
    this.pipes.addAll(pipes);
  }

  @Override
  public void addPipe(Pipe pipe) {
    this.pipes.add(pipe);
  }
}
