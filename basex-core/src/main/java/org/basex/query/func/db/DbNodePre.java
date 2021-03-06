package org.basex.query.func.db;

import org.basex.query.*;
import org.basex.query.func.*;
import org.basex.query.iter.*;
import org.basex.query.value.item.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class DbNodePre extends StandardFunc {
  @Override
  public Iter iter(final QueryContext qc) throws QueryException {
    final Iter iter = qc.iter(exprs[0]);
    return new Iter() {
      @Override
      public Int next() throws QueryException {
        final Item it = iter.next();
        return it == null ? null : Int.get(toDBNode(it).pre());
      }
    };
  }
}
