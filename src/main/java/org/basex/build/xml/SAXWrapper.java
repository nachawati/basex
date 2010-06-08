package org.basex.build.xml;

import static org.basex.core.Text.*;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import org.basex.build.FileParser;
import org.basex.core.Main;
import org.basex.core.ProgressException;
import org.basex.io.IO;
import org.basex.io.IOFile;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

/**
 * This class parses an XML document with Java's default SAX parser. Note that
 * large file cannot be parsed with the default parser due to entity handling
 * (e.g. the DBLP data).
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class SAXWrapper extends FileParser {
  /** File counter. */
  long counter;
  /** Current line. */
  int line = 1;

  /** SAX Handler reference. */
  private SAXHandler sax;
  /** Optional XML reader. */
  private final SAXSource source;
  /** File length. */
  private long length;

  /**
   * Constructor.
   * @param s sax source
   * @param ta target to insert into.
   */
  public SAXWrapper(final SAXSource s, final String ta) {
    super(IO.get(s.getSystemId()), ta);
    source = s;
  }

  @Override
  public void parse() throws IOException {
    final InputSource is = wrap(source.getInputSource());
    try {
      XMLReader r = source.getXMLReader();
      if(r == null) {
        final SAXParserFactory f = SAXParserFactory.newInstance();
        f.setNamespaceAware(true);
        f.setValidating(false);
        r = f.newSAXParser().getXMLReader();
      }

      sax = new SAXHandler(builder);
      sax.doc = doc;
      r.setDTDHandler(sax);
      r.setContentHandler(sax);
      r.setProperty("http://xml.org/sax/properties/lexical-handler", sax);
      r.setErrorHandler(sax);

      if(is != null) r.parse(is);
      else r.parse(source.getSystemId());
    } catch(final SAXParseException ex) {
      final String msg = Main.info(SCANPOS, ex.getSystemId(),
          ex.getLineNumber(), ex.getColumnNumber())
          + ": " + ex.getMessage();
      final IOException ioe = new IOException(msg);
      ioe.setStackTrace(ex.getStackTrace());
      throw ioe;
    } catch(final ProgressException ex) {
      throw ex;
    } catch(final Exception ex) {
      final IOException ioe = new IOException(ex.getMessage());
      ioe.setStackTrace(ex.getStackTrace());
      throw ioe;
    }
  }

  /**
   * Wraps the input source with a stream which counts the number of read bytes.
   * @param is input source
   * @return resulting stream
   * @throws IOException I/O exception
   */
  private InputSource wrap(final InputSource is) throws IOException {
    if(!(file instanceof IOFile) || is == null || is.getByteStream() != null
        || is.getSystemId() == null || is.getSystemId().isEmpty()) return is;

    final InputSource in = new InputSource(new FileInputStream(file.path()) {
      @Override
      public int read(final byte[] b, final int off, final int len)
          throws IOException {
        final int i = super.read(b, off, len);
        for(int o = off; o < len; o++)
          if(b[off + o] == '\n') line++;
        counter += i;
        return i;
      }
    });
    source.setInputSource(in);
    source.setSystemId(is.getSystemId());
    length = file.length();
    return in;
  }

  @Override
  public String tit() {
    return PROGCREATE;
  }

  @Override
  public String det() {
    return length == 0 ? super.det() : Main.info(SCANPOS, file.name(), line);
  }

  @Override
  public double prog() {
    return length == 0 ? sax == null ? 0 : sax.nodes / 3000000d % 1
        : (double) counter / length;
  }
}
