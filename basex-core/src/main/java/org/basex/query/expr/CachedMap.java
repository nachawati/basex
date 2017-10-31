package org.basex.query.expr;

import org.basex.query.*;
import org.basex.query.iter.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.hash.*;

/**
 * Simple map expression.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
final class CachedMap extends SimpleMap {
  /**
   * Constructor.
   * @param info input info
   * @param exprs expressions
   */
  CachedMap(final InputInfo info, final Expr... exprs) {
    super(info, exprs);
  }

  @Override
  public Iter iter(final QueryContext qc) throws QueryException {
    return value(qc).iter();
  }

  @Override
  public Value value(final QueryContext qc) throws QueryException {
    Value result = qc.value(exprs[0]);

    final QueryFocus qf = qc.focus, focus = new QueryFocus();
    qc.focus = focus;
    try {
      final int el = exprs.length;
      for(int e = 1; e < el; e++) {
        final Expr ex = exprs[e];
        focus.pos = 0;
        focus.size = result.size();
        final ValueBuilder vb = new ValueBuilder();
        for(final Item it : result) {
          focus.pos++;
          focus.value = it;
          vb.add(qc.value(ex));
        }
        result = vb.value();
      }
      return result;
    } finally {
      qc.focus = qf;
    }
  }

  @Override
  public SimpleMap copy(final CompileContext cc, final IntObjMap<Var> vm) {
    return copyType(new CachedMap(info, Arr.copyAll(cc, vm, exprs)));
  }
}
