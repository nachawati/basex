package org.basex.query.func.fn;

import java.util.*;

import org.basex.data.*;
import org.basex.index.path.*;
import org.basex.index.stats.*;
import org.basex.query.*;
import org.basex.query.expr.*;
import org.basex.query.expr.path.*;
import org.basex.query.func.*;
import org.basex.query.iter.*;
import org.basex.query.util.collation.*;
import org.basex.query.util.hash.*;
import org.basex.query.value.*;
import org.basex.query.value.item.*;
import org.basex.query.value.seq.*;
import org.basex.query.value.type.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class FnDistinctValues extends StandardFunc {
  @Override
  public Iter iter(final QueryContext qc) throws QueryException {
    final Collation coll = toCollation(1, qc);
    if(exprs[0] instanceof RangeSeq) return qc.iter(exprs[0]);

    final ItemSet set = coll == null ? new HashItemSet() : new CollationItemSet(coll);
    final Iter iter = exprs[0].atomIter(qc, info);
    return new Iter() {
      @Override
      public Item next() throws QueryException {
        do {
          final Item it = iter.next();
          if(it == null) return null;
          if(set.add(it, info)) return it;
          qc.checkStop();
        } while(true);
      }
    };
  }

  @Override
  public Value value(final QueryContext qc) throws QueryException {
    final Collation coll = toCollation(1, qc);
    if(exprs[0] instanceof RangeSeq) return (RangeSeq) exprs[0];

    final ValueBuilder vb = new ValueBuilder();
    final ItemSet set = coll == null ? new HashItemSet() : new CollationItemSet(coll);
    final Iter iter = exprs[0].atomIter(qc, info);
    for(Item it; (it = iter.next()) != null;) {
      qc.checkStop();
      if(set.add(it, info)) vb.add(it);
    }
    return vb.value();
  }

  @Override
  protected Expr opt(final CompileContext cc) throws QueryException {
    final SeqType st = exprs[0].seqType();
    if(st.type instanceof NodeType) {
      seqType = SeqType.get(AtomType.ATM, st.occ);
    } else if(!st.mayBeArray()) {
      seqType = st;
    }
    return exprs.length == 1 ? cmpDist(cc) : this;
  }

  /**
   * Pre-evaluates distinct-values() function, utilizing database statistics.
   * @param cc compilation context
   * @return original or optimized expression
   * @throws QueryException query exception
   */
  private Expr cmpDist(final CompileContext cc) throws QueryException {
    // can only be performed on axis paths
    if(!(exprs[0] instanceof AxisPath)) return this;

    // try to get statistics for resulting nodes
    final ArrayList<PathNode> nodes = ((AxisPath) exprs[0]).pathNodes(cc);
    if(nodes == null) return this;
    // loop through all nodes
    final HashItemSet is = new HashItemSet();
    for(PathNode pn : nodes) {
      // retrieve text child if addressed node is an element
      if(pn.kind == Data.ELEM) {
        if(!pn.stats.isLeaf()) return this;
        for(final PathNode n : pn.children) if(n.kind == Data.TEXT) pn = n;
      }
      // skip nodes others than texts and attributes
      if(pn.kind != Data.TEXT && pn.kind != Data.ATTR) return this;
      // check if distinct values are available
      if(!StatsType.isCategory(pn.stats.type)) return this;
      // if yes, add them to the item set
      for(final byte[] c : pn.stats.values) is.put(new Atm(c), info);
    }
    // return resulting sequence
    final ValueBuilder vb = new ValueBuilder();
    for(final Item i : is) vb.add(i);
    return vb.value();
  }
}
