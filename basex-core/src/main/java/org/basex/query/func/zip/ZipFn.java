package org.basex.query.func.zip;

import static org.basex.query.QueryText.*;
import static org.basex.util.Token.*;

import org.basex.query.func.*;
import org.basex.query.value.item.*;

/**
 * Functions on zip files.
 *
 * @author BaseX Team 2005-15, BSD License
 * @author Christian Gruen
 */
abstract class ZipFn extends StandardFunc {
  /** QName. */
  static final QNm Q_FILE = QNm.get(ZIP_PREFIX, "file", ZIP_URI);
  /** QName. */
  static final QNm Q_DIR = QNm.get(ZIP_PREFIX, "dir", ZIP_URI);
  /** QName. */
  static final QNm Q_ENTRY = QNm.get(ZIP_PREFIX, "entry", ZIP_URI);

  /** Attribute: href. */
  static final byte[] HREF = token("href");
  /** Attribute: name. */
  static final byte[] NAME = token("name");
}
