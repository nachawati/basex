package org.basex.gui.dialog;

import static org.basex.core.Text.*;

import java.awt.*;

import org.basex.gui.*;
import org.basex.gui.layout.*;

/**
 * Sort dialog.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public final class DialogSort extends BaseXDialog {
  /** Case sensitive. */
  private final BaseXCheckBox cs;
  /** Sort ascending. */
  private final BaseXCheckBox asc;
  /** Merge duplicate lines. */
  private final BaseXCheckBox merge;
  /** Unicode order. */
  private final BaseXCheckBox unicode;
  /** Column. */
  private final BaseXTextField column;
  /** Buttons. */
  private final BaseXBack buttons;

  /**
   * Default constructor.
   * @param gui reference to the main window
   */
  public DialogSort(final GUI gui) {
    super(gui, SORT);

    final BaseXBack p = new BaseXBack(new TableLayout(6, 1));

    final GUIOptions gopts = gui.gopts;
    asc = new BaseXCheckBox(this, ASCENDING_ORDER, GUIOptions.ASCSORT, gopts);
    cs = new BaseXCheckBox(this, CASE_SENSITIVE, GUIOptions.CASESORT, gopts);
    merge = new BaseXCheckBox(this, MERGE_DUPLICATES, GUIOptions.MERGEDUPL, gopts);
    unicode = new BaseXCheckBox(this, UNICODE_ORDER, GUIOptions.UNICODE, gopts);
    column = new BaseXTextField(this, GUIOptions.COLUMN, gopts);
    column.setColumns(4);

    final BaseXBack pp = new BaseXBack(new TableLayout(1, 2, 8, 4));
    pp.border(12, 0, 0, 0);
    pp.add(new BaseXLabel(COLUMN + COLS));
    pp.add(column);

    p.add(cs);
    p.add(asc);
    p.add(merge);
    p.add(unicode);
    p.add(pp);
    set(p, BorderLayout.CENTER);

    buttons = newButtons(B_OK, B_CANCEL);
    set(buttons, BorderLayout.SOUTH);
    action(null);
    finish();
  }

  @Override
  public void action(final Object source) {
    ok = column.check();
    enableOK(buttons, B_OK, ok);
  }

  @Override
  public void close() {
    if(!ok) return;
    super.close();
    cs.assign();
    asc.assign();
    unicode.assign();
    merge.assign();
    column.assign();
  }
}
