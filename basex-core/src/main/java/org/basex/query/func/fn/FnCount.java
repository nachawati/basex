package org.basex.query.func.fn;

import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.func.*;
import org.basex.query.func.map.*;
import org.basex.query.iter.*;
import org.basex.query.value.item.*;
import org.basex.query.var.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class FnCount extends StandardFunc {
  @Override
  public Item item(final QueryContext qc, final InputInfo ii) throws QueryException {
    final Iter iter = qc.iter(exprs[0]);
    long c = iter.size();
    if(c == -1) {
      do {
        qc.checkStop();
        ++c;
      } while(iter.next() != null);
    }
    return Int.get(c);
  }

  @Override
  protected Expr opt(final CompileContext cc) throws QueryException {
    // skip non-deterministic and variable expressions
    final Expr e = exprs[0];
    if(e.has(Flag.NDT) || e.has(Flag.UPD) || e instanceof VarRef) return this;

    // return size known at compile time
    final long c = e.size();
    if(c >= 0) return Int.get(c);

    // rewrite count(map:keys(...)) to map:size(...)
    if(e instanceof MapKeys) return cc.function(Function._MAP_SIZE, info, ((MapKeys) e).exprs);

    // no optimization possible
    return this;
  }
}
