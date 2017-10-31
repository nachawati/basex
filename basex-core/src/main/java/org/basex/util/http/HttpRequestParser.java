package org.basex.util.http;

import static org.basex.query.QueryError.*;
import static org.basex.util.Token.*;
import static org.basex.util.http.HttpText.*;

import java.util.*;

import org.basex.core.*;
import org.basex.io.serial.*;
import org.basex.query.*;
import org.basex.query.iter.*;
import org.basex.query.util.list.*;
import org.basex.query.value.item.*;
import org.basex.query.value.node.*;
import org.basex.util.*;
import org.basex.util.http.HttpRequest.*;

/**
 * Request parser.
 *
 * @author BaseX Team 2005-17, BSD License
 * @author Rositsa Shadura
 */
public final class HttpRequestParser {
  /** Input information. */
  private final InputInfo info;

  /**
   * Constructor.
   * @param info input info
   */
  public HttpRequestParser(final InputInfo info) {
    this.info = info;
  }

  /**
   * Parses HTTP request data.
   * @param request request element (can be {@code null})
   * @return parsed request
   * @throws QueryException query exception
   */
  public HttpRequest parse(final ANode request) throws QueryException {
    return parse(request, null);
  }

  /**
   * Parses an <http:request/> element.
   * @param request request element (can be {@code null})
   * @param bodies content items
   * @return parsed request
   * @throws QueryException query exception
   */
  public HttpRequest parse(final ANode request, final Iter bodies) throws QueryException {
    final HttpRequest hr = new HttpRequest();

    if(request != null) {
      for(final ANode attr : request.attributes()) {
        final String key = string(attr.name());
        final Request r = Request.get(key);
        if(r == null) throw HC_REQ_X.get(info, "Unknown attribute: " + key);
        hr.attributes.put(r, string(attr.string()));
      }
      checkRequest(hr);

      // it is an error if content is set for HTTP verbs which must be empty
      final ANode payload = parseHeaders(request.children(), hr.headers);
      final String method = hr.attribute(Request.METHOD);
      if(Strings.eq(method, TRACE, DELETE) && (payload != null || bodies != null))
        throw HC_REQ_X.get(info, "Body not expected for method " + method);

      if(payload != null) {
        final QNm pl = payload.qname();
        // single part request
        if(pl.eq(Q_BODY)) {
          final Item it = bodies != null ? bodies.next() : null;
          parseBody(payload, it, hr.payloadAtts, hr.payload);
          hr.isMultipart = false;
          // multipart request
        } else if(pl.eq(Q_MULTIPART)) {
          parseMultipart(payload, bodies, hr.payloadAtts, hr.parts);
          hr.isMultipart = true;
        } else {
          throw HC_REQ_X.get(info, "Unknown payload element: " + payload.qname());
        }
      }
    }
    return hr;
  }

  /**
   * Parses the attributes of an element.
   * @param element element
   * @param atts map for parsed attributes
   */
  private static void parseAtts(final ANode element, final HashMap<String, String> atts) {
    for(final ANode attr : element.attributes()) {
      atts.put(string(attr.name()), string(attr.string()));
    }
  }

  /**
   * Parses <http:header/> children of requests and parts.
   * @param iter iterator on request/part children
   * @param hdrs map for parsed headers
   * @return body or multipart
   */
  private static ANode parseHeaders(final BasicNodeIter iter, final HashMap<String, String> hdrs) {
    for(final ANode node : iter) {
      final QNm nm = node.qname();
      if(nm == null) continue;
      if(!nm.eq(Q_HEADER)) return node;

      String name = null, value = null;
      for(final ANode attr : node.attributes()) {
        final String qn = string(attr.qname().local());
        if(qn.equals(NAME)) name = string(attr.string());
        else if(qn.equals(VALUE)) value = string(attr.string());
      }
      if(name != null && !name.isEmpty() && value != null && !value.isEmpty()) {
        hdrs.put(name, value);
      }
    }
    return null;
  }

  /**
   * Parses <http:body/> element.
   * @param body body element
   * @param contItem content item
   * @param atts map for parsed body attributes
   * @param payload payload
   * @throws QueryException query exception
   */
  private void parseBody(final ANode body, final Item contItem, final HashMap<String, String> atts,
      final ItemList payload) throws QueryException {

    parseAtts(body, atts);
    checkBody(body, atts);

    if(atts.get(SRC) == null) {
      // no linked resource for setting request content
      if(contItem == null) {
        // content is taken from children of <http:body/> element
        for(final ANode n : body.children()) payload.add(n);
      } else {
        // content is taken from $bodies parameter
        payload.add(contItem);
      }
    }
  }

  /**
   * Parses a <http:multipart/> element.
   * @param multipart multipart element
   * @param contItems content items
   * @param atts map for multipart attributes
   * @param parts list for multipart parts
   * @throws QueryException query exception
   */
  private void parseMultipart(final ANode multipart, final Iter contItems,
      final HashMap<String, String> atts, final ArrayList<Part> parts) throws QueryException {

    parseAtts(multipart, atts);
    if(atts.get(SerializerOptions.MEDIA_TYPE.name()) == null)
      throw HC_REQ_X.get(info, "Attribute media-type of http:multipart is mandatory");

    final BasicNodeIter iter = multipart.children();
    while(true) {
      final Part part = new Part();
      final ANode partBody = parseHeaders(iter, part.headers);
      if(partBody == null) break;
      // content is set from <http:body/> children or from $bodies parameter
      final Item ci = contItems == null ? null : contItems.next();
      parseBody(partBody, ci, part.bodyAtts, part.bodyContents);
      parts.add(part);
    }
  }

  /**
   * Checks consistency of attributes for <http:request/>.
   * @param req request
   * @throws QueryException query exception
   */
  private void checkRequest(final HttpRequest req) throws QueryException {
    // method denotes the HTTP verb and is mandatory
    final String mth = req.attribute(Request.METHOD);
    if(mth == null) throw HC_REQ_X.get(info, "Missing attribute: " + Request.METHOD);
    req.attributes.put(Request.METHOD, mth.toUpperCase(Locale.ENGLISH));

    // check parameters needed in case of authorization
    final String sendAuth = req.attribute(Request.SEND_AUTHORIZATION);
    if(sendAuth != null && Strings.yes(sendAuth)) {
      final String us = req.attribute(Request.USERNAME);
      if(us == null) throw HC_REQ_X.get(info, "Missing attribute: " + Request.USERNAME);
      final String pw = req.attribute(Request.PASSWORD);
      if(pw == null) throw HC_REQ_X.get(info, "Missing attribute: " + Request.PASSWORD);
      final String am = req.attribute(Request.AUTH_METHOD);
      if(am != null && !am.isEmpty()) {
        req.authMethod = StaticOptions.AUTHMETHOD.get(am);
        if(req.authMethod == null) throw HC_REQ_X.get(info, "Invalid authentication method: " + am);
      }
    } else {
      req.attributes.remove(Request.USERNAME);
      req.attributes.remove(Request.PASSWORD);
      req.attributes.remove(Request.AUTH_METHOD);
    }

    // check other parameters
    final String timeout = req.attribute(Request.TIMEOUT);
    if(timeout != null && Strings.toInt(timeout) < 0)
      throw HC_REQ_X.get(info, "Invalid timeout: " + timeout);

    for(final Request r : new Request[] {
      Request.FOLLOW_REDIRECT, Request.STATUS_ONLY, Request.SEND_AUTHORIZATION
    }) {
      final String s = req.attribute(r);
      if(s != null && !Strings.eq(s, Text.TRUE, Text.FALSE))
        throw HC_REQ_X.get(info, "Value of '" + r + "' attribute is no boolean: " + s);
    }
  }

  /**
   * Checks consistency of attributes for <http:body/>.
   * @param body body
   * @param bodyAtts body attributes
   * @throws QueryException query exception
   */
  private void checkBody(final ANode body, final HashMap<String, String> bodyAtts)
      throws QueryException {

    // @media-type is mandatory
    if(bodyAtts.get(SerializerOptions.MEDIA_TYPE.name()) == null)
      throw HC_REQ_X.get(info, "Attribute media-type of http:body is mandatory");

    // if src attribute is used to set the content of the body, no
    // other attributes must be specified and no content must be present
    if(bodyAtts.get(SRC) != null && (bodyAtts.size() > 2 || body.children().next() != null))
      throw HC_ATTR.get(info);
  }
}
