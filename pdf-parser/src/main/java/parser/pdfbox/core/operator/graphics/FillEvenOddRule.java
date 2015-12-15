package parser.pdfbox.core.operator.graphics;

import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSFloat;

import parser.pdfbox.core.operator.OperatorProcessor;

/**
 * f* Fill path using even odd rule.
 * 
 * @author Claudius Korzen
 */
public final class FillEvenOddRule extends OperatorProcessor {
  @Override
  public void process(Operator operator, List<COSBase> operands)
    throws IOException {
    // Call operation "stroke path" with given windingRule.
    // Use COSFloat, because COSInteger is private.
    operands.add(new COSFloat(GeneralPath.WIND_EVEN_ODD));
    
    context.processOperator("S", operands); // Stroke path
  }

  @Override
  public String getName() {
    return "f*";
  }
}
