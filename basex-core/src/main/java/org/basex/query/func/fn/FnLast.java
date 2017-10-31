package org.basex.query.func.fn;

import org.basex.core.locks.*;
import org.basex.query.*;
import org.basex.query.func.*;
import org.basex.query.util.*;
import org.basex.query.value.item.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class FnLast extends StandardFunc {
  @Override
  public Item item(final QueryContext qc, final InputInfo ii) throws QueryException {
    ctxValue(qc);
    return Int.get(qc.focus.size);
  }

  @Override
  public boolean accept(final ASTVisitor visitor) {
    return visitor.lock(Locking.CONTEXT) && super.accept(visitor);
  }
}
