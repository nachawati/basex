package org.basex.query.expr;

import static org.basex.query.QueryText.*;
import org.basex.query.IndexContext;
import org.basex.query.QueryContext;
import org.basex.query.QueryException;
import org.basex.query.item.Bln;
import org.basex.query.item.DBNode;
import org.basex.query.item.Item;
import org.basex.query.util.Scoring;
import org.basex.util.Array;

/**
 * And expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class And extends Arr {
  /**
   * Constructor.
   * @param e expression list
   */
  public And(final Expr[] e) {
    super(e);
  }

  @Override
  public Expr comp(final QueryContext ctx) throws QueryException {
    super.comp(ctx);
    for(int e = 0; e < expr.length; e++) {
      if(!expr[e].i()) continue;
      
      if(!((Item) expr[e]).bool()) {
        // atomic items can be pre-evaluated
        ctx.compInfo(OPTFALSE, expr[e]);
        return Bln.FALSE;
      }
      ctx.compInfo(OPTTRUE, expr[e]);
      expr = Array.delete(expr, e--);
      if(expr.length == 0) return Bln.TRUE;
    }

    // merge predicates if possible
    Expr[] ex = {};
    Pos ps = null;
    CmpR cr = null;
    for(final Expr e : expr) {
      Expr tmp = null;
      if(e instanceof Pos) {
        // merge position predicates
        tmp = ps == null ? e : ps.intersect((Pos) e);
        if(!(tmp instanceof Pos)) return tmp;
        ps = (Pos) tmp;
      } else if(e instanceof CmpR) {
        // merge comparisons
        tmp = cr == null ? e : cr.intersect((CmpR) e);
        if(tmp instanceof CmpR) {
          cr = (CmpR) tmp;
        } else if(tmp != null) {
          return tmp;
        }
      }
      if(tmp == null) ex = Array.add(ex, e);
    }

    expr = ex;
    if(ps != null) expr = Array.add(expr, ps);
    if(cr != null) expr = Array.add(expr, cr);
    return expr.length == 1 && expr[0].returned(ctx) == Return.BLN ?
        expr[0] : this;
  }

  @Override
  public Bln atomic(final QueryContext ctx) throws QueryException {
    double s = 0;
    for(final Expr e : expr) {
      final Item it = e.ebv(ctx);
      if(!it.bool()) {
        // [SG] pre + 1  will cause troubles for some documents..
        // [CG] don't know if we get incorrect results this way ?!
        /*if(it instanceof DBNode && ctx.ftdata != null && ctx.item != null) {
          NodeMore ci = ((DBNode) it).child();
          while(ci.more()) {
            Item child = ci.next();
            if (child instanceof DBNode) {
              final DBNode dbn = (DBNode) child;
              if (dbn.type == Type.TXT) ctx.ftdata.remove(dbn.pre);
            }
          }
        }
        */
        if(ctx.ftdata != null && ctx.item != null) 
          ctx.ftdata.remove(((DBNode) ctx.item).pre + 1);
        return Bln.FALSE;
      }
      s = Scoring.and(s, it.score());
    }
    // no scoring - return default boolean
    return s == 0 ? Bln.TRUE : Bln.get(s);
  }
  
  @Override
  public Return returned(final QueryContext ctx) {
    return Return.BLN;
  }

  @Override
  public void indexAccessible(final QueryContext ctx, final IndexContext ic)
      throws QueryException {
    
    for(final Expr e : expr) {
      e.indexAccessible(ctx, ic);
      if(!ic.iu || ic.is == 0) return;
    }
  }

  @Override
  public Expr indexEquivalent(final QueryContext ctx, final IndexContext ic)
      throws QueryException {

    super.indexEquivalent(ctx, ic);
    return new InterSect(expr);
  }

  @Override
  public String toString() {
    return toString(" and ");
  }
}
