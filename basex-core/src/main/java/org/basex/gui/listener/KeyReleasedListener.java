package org.basex.gui.listener;

import java.awt.event.*;

/**
 * Listener interface for released keys.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public interface KeyReleasedListener extends KeyListener {
  @Override
  default void keyTyped(KeyEvent e) { }

  @Override
  default void keyPressed(KeyEvent e) { }
}
