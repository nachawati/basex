package org.basex.query.func.admin;

import static org.basex.query.QueryError.*;

import org.basex.query.*;
import org.basex.query.value.item.*;
import org.basex.server.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class AdminDeleteLogs extends AdminFn {
  @Override
  public Item item(final QueryContext qc, final InputInfo ii) throws QueryException {
    checkAdmin(qc);

    final String name = Token.string(toToken(exprs[0], qc));
    final LogFile file = qc.context.log.file(name);
    if(file == null) throw WHICHRES_X.get(info, name);
    if(file.current()) throw BXAD_TODAY.get(info, name);
    if(!file.delete()) throw BXAD_DELETE_X.get(info, name);
    return null;
  }
}
