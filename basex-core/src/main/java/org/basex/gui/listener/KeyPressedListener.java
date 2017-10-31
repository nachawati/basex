package org.basex.gui.listener;

import java.awt.event.*;

/**
 * Listener interface for released keys.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Christian Gruen
 */
public interface KeyPressedListener extends KeyListener {
  @Override
  default void keyTyped(KeyEvent e) { }

  @Override
  default void keyReleased(KeyEvent e) { }
}
