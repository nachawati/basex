package org.basex.query.func.fn;

import org.basex.query.*;
import org.basex.query.value.item.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class FnUnparsedText extends Parse {
  @Override
  public Item item(final QueryContext qc, final InputInfo ii) throws QueryException {
    return unparsedText(qc, false, true);
  }

  @Override
  protected FnUnparsedText opt(final CompileContext cc) {
    singleOcc();
    return this;
  }
}
