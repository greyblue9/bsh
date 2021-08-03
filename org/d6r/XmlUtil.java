package org.d6r;

// import org.d6r.xml.DomBuilder;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.base.Objects;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.commons.lang3.StringUtils;
import org.d6r.XmlUtil.DomBuilder;
import org.d6r.XmlUtil.Position;
import org.d6r.XmlUtil.SourcePosition;



public class XmlUtil {
  static final String BOM_ERROR_MESSAGE = "Content is not allowed in prolog";
  static final String SAX_FEATURES_URI = "http://xml.org/sax/features";
  
  static final String NAMESPACE_FEATURE = SAX_FEATURES_URI + "/namespaces";
  static final String NAMESPACE_PREFIX_FEATURE
    = SAX_FEATURES_URI + "/namespace-prefixes";
  static final String PROVIDE_XMLNS_URIS
    = SAX_FEATURES_URI + "/xmlns-uris";
  static final String TAG = "XmlUtil";
  static final String UTF_8 = "UTF-8";                 //$NON-NLS-1$
  static final String UTF_16 = "UTF_16";               //$NON-NLS-1$
  static final String POS_KEY = "offsets";             //$NON-NLS-1$
  static final String UTF_16LE = "UTF_16LE";           //$NON-NLS-1$
  static final String CONTENT_KEY = "contents";        //$NON-NLS-1$

  /** See http://www.w3.org/TR/REC-xml/#NT-EncodingDecl */
  static final Pattern ENCODING_PATTERN
    = Pattern.compile("encoding=['\"](\\S*)['\"]");
  
  
  static final Map<String, Boolean> featureFlags = RealArrayMap.toMap(
    NAMESPACE_FEATURE, Boolean.FALSE,
    NAMESPACE_PREFIX_FEATURE, Boolean.TRUE
    //PROVIDE_XMLNS_URIS, Boolean.FALSE
  );
  
  /**
  Parses the XML content from the given input stream.
  
  @param input the input stream containing the XML to be parsed
  @return the corresponding document
  @throws ParserConfigurationException if a SAX parser is not available
  @throws SAXException if the document contains a parsing error
  @throws IOException if something is seriously wrong. This should not
    happen since the input source is known to be constructed from a string.
  */
  public Document parse(final InputStream input)
    throws ParserConfigurationException, SAXException, IOException
  {
    return parse(toXmlString(input));
  }
  
  /** Parses the XML content from the given XML string; see #parse(InputStream). */
  public Document parse(final String xml)
    throws ParserConfigurationException, SAXException, IOException
  {
    final SAXParserFactory factory = SAXParserFactory.newInstance();
    Log.d(TAG, "factory := SAXParserFactory.newInstance() --> %s", factory);
    
    for (final Map.Entry<String, Boolean> feat: featureFlags.entrySet()) {
      Log.d(TAG,
        "Requesting feature '%s' to be %s ...", 
        (feat.getKey().indexOf('/') != -1)
          ? StringUtils.substringAfterLast(feat.getKey(), "/")
          : feat.getKey(),
        (Boolean.TRUE.equals(feat.getValue())) ? "ENABLED": "DISABLED"
      );
      try {
        factory.setFeature(feat.getKey(), Boolean.TRUE.equals(feat.getValue()));
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }
    
    try {
      final InputSource input = new InputSource(new StringReader(xml));
      final DomBuilder handler = new DomBuilder(xml);
      ((SAXParser) factory.newSAXParser()).parse(input, handler); // SAXParser
      return handler.getDocument();
    } catch (SAXException saxEx) {
      final String message = saxEx.getMessage();
      final boolean isBomError
        = (message != null && message.contains(BOM_ERROR_MESSAGE));
      if (isBomError) {
        // Byte order mark in the string? Skip it. There are many markers
        // (see http://en.wikipedia.org/wiki/Byte_order_mark) so here well
        // just skip those up to the XML prolog beginning character,
        final String xmlNoBom = xml.replaceFirst("^([\\W]+)<","<");
        final InputSource inputNB = new InputSource(new StringReader(xmlNoBom));
        final DomBuilder handlerNB = new DomBuilder(xmlNoBom);
        ((SAXParser) factory.newSAXParser()).parse(inputNB, handlerNB);
        return handlerNB.getDocument();
      } else {
        throw saxEx;
      }
    }
  }
  
  public static String toXmlString(final InputStream input) {
    try {
      // Read in all the data
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      final byte[] buf = new byte[1024];
      int read;
      try {
        while ((read = input.read(buf)) != -1) out.write(buf, 0, read);
      } finally {
        input.close();
      }
      final byte[] data = out.toByteArray();
      final String xml = getXmlString(data);
      return xml;
    } catch (IOException e) {
      throw new RuntimeException("Error reading stream into byte[]/XML string", e);
    }
  }
  
  
  public static String stripBom(String xml) {
    return xml.replaceFirst("^([\\W]+)<","<");
  }
  
  public static String getXmlString(final byte[] data) {
    int offset = 0;
    String defaultCharset = UTF_8, charset = null;
    // Look for the byte order mark, to see if we need to remove bytes from
    // the input stream (and to determine whether files are big endian or
    // little endian), etc. for files which do not specify the encoding.
    // See http://unicode.org/faq/utf_bom.html#BOM for more.
    if (data.length > 4) {
      if (data[0] == (byte)0xef && data[1] == (byte)0xbb && data[2] == (byte)0xbf) {
        // UTF-8
        defaultCharset = charset = UTF_8;
        offset += 3;
      } else if (data[0] == (byte)0xfe && data[1] == (byte)0xff) {
        //  UTF-16, big-endian
        defaultCharset = charset = UTF_16;
        offset += 2;
      } else if (data[0] == (byte)0x0 && data[1] == (byte)0x0
          && data[2] == (byte)0xfe && data[3] == (byte)0xff) {
        // UTF-32, big-endian
        defaultCharset = charset = "UTF_32";  //$NON-NLS-1$
        offset += 4;
      } else if (data[0] == (byte)0xff && data[1] == (byte)0xfe
          && data[2] == (byte)0x0 && data[3] == (byte)0x0) {
        // UTF-32, little-endian. We must check for this *before* looking for
        // UTF_16LE since UTF_32LE has the same prefix!
        defaultCharset = charset = "UTF_32LE";  //$NON-NLS-1$
        offset += 4;
      } else if (data[0] == (byte)0xff && data[1] == (byte)0xfe) {
        //  UTF-16, little-endian
        defaultCharset = charset = UTF_16LE;
        offset += 2;
      }
    }
    int length = data.length - offset;
    // Guess encoding by searching for an encoding= entry in the first line.
    // The prologue, and the encoding names, will always be in ASCII - which means
    // we don't need to worry about strange character encodings for the prologue
    // characters. However, one wrinkle is that the whole file may be encoded in
    // something like UTF-16 where there are two bytes per character, so we can't
    // just look for `['e','n','c','o','d','i','n','g']`, etc. in the byte array,
    // since there could be multiple bytes for each character. However, since
    // (again) the prologue is in ASCII, we can just drop the zeroes.
    boolean seenOddZero = false;
    boolean seenEvenZero = false;
    int prologueStart = -1;
    for (int lineEnd = offset; lineEnd < data.length; lineEnd++) {
      if (data[lineEnd] == 0) {
        if ((lineEnd - offset) % 2 == 0) seenEvenZero = true;
        else seenOddZero = true;
      } else if (data[lineEnd] == '\n' || data[lineEnd] == '\r') { 
        break;
      } else if (data[lineEnd] == '<') {
        prologueStart = lineEnd;
      } else if (data[lineEnd] == '>') {
        // End of prologue. Quick check to see if this is a utf-8 file since thats
        // common
        for (int i = lineEnd - 4; i >= 0; i--) {
          if ((data[i] == 'u' || data[i] == 'U')
              && (data[i + 1] == 't' || data[i + 1] == 'T')
              && (data[i + 2] == 'f' || data[i + 2] == 'F')
              && (data[i + 3] == '-' || data[i + 3] == '_')
              && (data[i + 4] == '8')
              ) {
            charset = UTF_8;
            break;
          }
        }
        if (charset == null) {
          StringBuilder sb = new StringBuilder();
          for (int i = prologueStart; i <= lineEnd; i++) {
            if (data[i] != 0) sb.append((char) data[i]);
          }
          String prologue = sb.toString();
          int encodingIndex = prologue.indexOf("encoding"); //$NON-NLS-1$
          if (encodingIndex != -1) {
            Matcher matcher = ENCODING_PATTERN.matcher(prologue);
            if (matcher.find(encodingIndex)) charset = matcher.group(1);
          }
        }
        break;
      }
    }
    // No prologue on the first line, and no byte order mark: Assume UTF-8/16
    if (charset == null) {
      charset = seenOddZero ? UTF_16LE : seenEvenZero ? UTF_16 : UTF_8;
    }
    String xml = null;
    try {
      xml = new String(data, offset, length, charset);
    } catch (UnsupportedEncodingException e) {
      try {
        if (charset != defaultCharset) {
          xml = new String(data, offset, length, defaultCharset);
        }
      } catch (UnsupportedEncodingException u) {
      } // Just use the default encoding below
    }
    if (xml == null) xml = new String(data, offset, length);
    return xml;
  }
  



  

  /*@NonNull
  private static Document parse(@NonNull String xml, @NonNull final InputSource input, final boolean checkBom) throws ParserConfigurationException, SAXException, IOException {
    try {
      final SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setFeature("http://xml.org/sax/features/namespaces", true);
      factory.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
      factory.setFeature("http://xml.org/sax/features/xmlns-uris", true);
      final SAXParser parser = factory.newSAXParser();
      final DomBuilder handler = new DomBuilder(xml);
      final XMLReader xmlReader = parser.getXMLReader();
      xmlReader.setProperty("http://xml.org/sax/properties/lexical-handler", handler);
      parser.parse(input, handler);
      return handler.getDocument();
    } catch (SAXException e) {
      if (checkBom && e.getMessage().contains("Content is not allowed in prolog")) {
        xml = xml.replaceFirst("^([\\W]+)<", "<");
        return parse(xml, new InputSource(new StringReader(xml)), false);
      }
      throw e;
    }
  }*/
  
  @NonNull
  public static String getXmlString(@NonNull final byte[] data,
  @NonNull String defaultCharset)
  {
    int offset = 0;
    String charset = null;
    if (data.length > 4) {
      if (data[0] == -17 && data[1] == -69 && data[2] == -65) {
        charset = (defaultCharset = "UTF-8");
        offset += 3;
      } else if (data[0] == -2 && data[1] == -1) {
        charset = (defaultCharset = "UTF_16");
        offset += 2;
      } else if (data[0] == 0 && data[1] == 0 && data[2] == -2 && data[3] == -1) {
        charset = (defaultCharset = "UTF_32");
        offset += 4;
      } else if (data[0] == -1 && data[1] == -2 && data[2] == 0 && data[3] == 0) {
        charset = (defaultCharset = "UTF_32LE");
        offset += 4;
      } else if (data[0] == -1 && data[1] == -2) {
        charset = (defaultCharset = "UTF_16LE");
        offset += 2;
      }
    }
    final int length = data.length - offset;
    boolean seenOddZero = false;
    boolean seenEvenZero = false;
    int prologueStart = -1;
    for (int lineEnd = offset; lineEnd < data.length; ++lineEnd) {
      if (data[lineEnd] == 0) {
        if ((lineEnd - offset) % 2 == 0) {
          seenEvenZero = true;
        } else {
          seenOddZero = true;
        }
      } else {
        if (data[lineEnd] == 10) {
          break;
        }
        if (data[lineEnd] == 13) {
          break;
        }
        if (data[lineEnd] == 60) {
          prologueStart = lineEnd;
        } else if (data[lineEnd] == 62) {
          for (int i = lineEnd - 4; i >= 0; --i) {
            if ((data[i] == 117 || data[i] == 85) &&
                (data[i + 1] == 116 || data[i + 1] == 84) && 
                (data[i + 2] == 102 || data[i + 2] == 70) && 
                (data[i + 3] == 45 || data[i + 3] == 95) &&
                 data[i + 4] == 56)
            {
              charset = "UTF-8";
              break;
            }
          }
          if (charset == null) {
            final StringBuilder sb = new StringBuilder();
            for (int j = prologueStart; j <= lineEnd; ++j) {
              if (data[j] != 0) {
                sb.append((char) data[j]);
              }
            }
            final String prologue = sb.toString();
            final int encodingIndex = prologue.indexOf("encoding");
            if (encodingIndex != -1) {
              final Matcher matcher 
                = ENCODING_PATTERN.matcher(prologue);
              if (matcher.find(encodingIndex)) {
                charset = matcher.group(1);
              }
            }
            break;
          }
          break;
        }
      }
    }
    if (charset == null) {
      charset = (seenOddZero ? "UTF_16LE" : (seenEvenZero ? "UTF_16" : defaultCharset));
    }
    String xml = null;
    try {
      xml = new String(data, offset, length, charset);
    } catch (UnsupportedEncodingException e) {
      try {
        if (charset != defaultCharset) {
          xml = new String(data, offset, length, defaultCharset);
        }
      } catch (UnsupportedEncodingException ex) {
      }
    }
    if (xml == null) {
      xml = new String(data, offset, length);
    }
    return xml;
  }

  @NonNull
  public static SourcePosition getPosition(@NonNull final Node node) {
    return getPosition(node, -1, -1);
  }

  @NonNull
  public static SourcePosition getPosition(@NonNull final Node node, final int start, final int end) {
    final Position p = getPositionHelper(node, start, end);
    return (p == null) ? SourcePosition.UNKNOWN : p.toSourcePosition();
  }

  @Nullable
  private static Position getPositionHelper(@NonNull final Node node, final int start, final int end) {
    if (node instanceof Attr) {
      final Attr attr = (Attr) node;
      final Position pos = (Position) attr.getOwnerElement().getUserData("offsets");
      if (pos != null) {
        int startOffset = pos.getOffset();
        int endOffset = pos.getEnd().getOffset();
        if (start != -1) {
          startOffset += start;
          if (end != -1) {
            endOffset = startOffset + (end - start);
          }
        }
        final String contents = (String) node.getOwnerDocument().getUserData("contents");
        if (contents == null) {
          return null;
        }
        final String name = attr.getName();
        final Pattern pattern = Pattern.compile((attr.getPrefix() != null) ? String.format("(%1$s\\s*=\\s*[\"'].*?[\"'])", name) : String.format("[^:](%1$s\\s*=\\s*[\"'].*?[\"'])", name));
        final Matcher matcher = pattern.matcher(contents);
        if (matcher.find(startOffset) && matcher.start(1) <= endOffset) {
          final int index = matcher.start(1);
          int line = pos.getLine();
          int column = pos.getColumn();
          for (int offset = pos.getOffset(); offset < index; ++offset) {
            final char t = contents.charAt(offset);
            if (t == '\n') {
              ++line;
              column = 0;
            } else {
              ++column;
            }
          }
          final Position attributePosition = new Position(line, column, index);
          attributePosition.setEnd(new Position(line, column + matcher.end(1) - index, matcher.end(1)));
          return attributePosition;
        }
        return pos;
      }
    } else if (node instanceof Text) {
      Position pos2 = null;
      if (node.getPreviousSibling() != null) {
        pos2 = (Position) node.getPreviousSibling().getUserData("offsets");
      }
      if (pos2 == null) {
        pos2 = (Position) node.getParentNode().getUserData("offsets");
      }
      if (pos2 != null) {
        final int startOffset2 = pos2.getOffset();
        final int endOffset2 = pos2.getEnd().getOffset();
        int line2 = pos2.getLine();
        int column2 = pos2.getColumn();
        final String contents2 = (String) node.getOwnerDocument().getUserData("contents");
        if (contents2 == null || contents2.length() < endOffset2) {
          return null;
        }
        boolean inAttribute = false;
        for (int offset2 = startOffset2; offset2 <= endOffset2; ++offset2) {
          final char c = contents2.charAt(offset2);
          if (c == '>' && !inAttribute) {
            ++offset2;
            ++column2;
            final String text = node.getNodeValue();
            int textIndex = 0;
            int textLength = text.length();
            int newLine = line2;
            int newColumn = column2;
            if (start != -1) {
              for (textLength = Math.min(textLength, start); textIndex < textLength; ++textIndex) {
                final char t2 = text.charAt(textIndex);
                if (t2 == '\n') {
                  ++newLine;
                  newColumn = 0;
                } else {
                  ++newColumn;
                }
              }
            } else {
              while (textIndex < textLength) {
                final char t2 = text.charAt(textIndex);
                if (t2 == '\n') {
                  ++newLine;
                  newColumn = 0;
                } else {
                  if (!Character.isWhitespace(t2)) {
                    break;
                  }
                  ++newColumn;
                }
                ++textIndex;
              }
            }
            if (textIndex == text.length()) {
              textIndex = 0;
            } else {
              line2 = newLine;
              column2 = newColumn;
            }
            final Position attributePosition2 = new Position(line2, column2, offset2 + textIndex);
            if (end != -1) {
              attributePosition2.setEnd(new Position(line2, column2, offset2 + end));
            } else {
              attributePosition2.setEnd(new Position(line2, column2, offset2 + textLength));
            }
            return attributePosition2;
          }
          if (c == '\"') {
            inAttribute = !inAttribute;
          } else if (c == '\n') {
            ++line2;
            column2 = -1;
          }
          ++column2;
        }
        return pos2;
      }
    }
    return (Position) node.getUserData("offsets");
  }

  
  
  public static final class DomBuilder extends DefaultHandler2 {

    private final String mXml;
    private final Document mDocument;
    private Locator mLocator;

    private int mCurrentLine;
    private int mCurrentOffset;
    private int mCurrentColumn;

    private final List<Element> mStack;
    private final StringBuilder mPendingText;

    private DomBuilder(final String xml) throws ParserConfigurationException {
      this.mCurrentLine = 0;
      this.mStack = new ArrayList<Element>();
      this.mPendingText = new StringBuilder();
      this.mXml = xml;
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setValidating(false);
      final DocumentBuilder docBuilder = factory.newDocumentBuilder();
      (this.mDocument = docBuilder.newDocument()).setUserData("contents", xml, null);
    }

    Document getDocument() {
      return this.mDocument;
    }

    @Override
    public void setDocumentLocator(final Locator locator) {
      this.mLocator = locator;
    }

    @Override
    public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
      try {
        this.flushText();
        final Element element = this.mDocument.createElementNS(uri, qName);
        for (int i = 0; i < attributes.getLength(); ++i) {
          if (attributes.getURI(i) != null && !attributes.getURI(i).isEmpty()) {
            final Attr attr = this.mDocument.createAttributeNS(attributes.getURI(i), attributes.getQName(i));
            attr.setValue(attributes.getValue(i));
            element.setAttributeNodeNS(attr);
            assert attr.getOwnerElement() == element;
          } else {
            final Attr attr = this.mDocument.createAttribute(attributes.getQName(i));
            attr.setValue(attributes.getValue(i));
            element.setAttributeNode(attr);
            assert attr.getOwnerElement() == element;
          }
        }
        final Position pos = this.getCurrentPosition();
        element.setUserData("offsets", this.findOpeningTag(pos), null);
        this.mStack.add(element);
      } catch (Exception t) {
        throw new SAXException(t);
      }
    }

    @Override
    public void endElement(final String uri, final String localName, final String qName) {
      this.flushText();
      final Element element = this.mStack.remove(this.mStack.size() - 1);
      final Position pos = (Position) element.getUserData("offsets");
      assert pos != null;
      pos.setEnd(this.getCurrentPosition());
      this.addNodeToParent(element);
    }

    @Override
    public void comment(final char[] chars, final int start, final int length) throws SAXException {
      this.flushText();
      final String comment = new String(chars, start, length);
      final Comment domComment = this.mDocument.createComment(comment);
      final Position currentPosition = this.getCurrentPosition();
      final Position startPosition = this.findOpeningTag(currentPosition);
      startPosition.setEnd(currentPosition);
      domComment.setUserData("offsets", startPosition, null);
      this.addNodeToParent(domComment);
    }

    private void addNodeToParent(final Node nodeToAdd) {
      if (this.mStack.isEmpty()) {
        this.mDocument.appendChild(nodeToAdd);
      } else {
        final Element parent = this.mStack.get(this.mStack.size() - 1);
        parent.appendChild(nodeToAdd);
      }
    }

    private Position findOpeningTag(final Position startingPosition) {
      for (int offset = startingPosition.getOffset() - 1; offset >= 0; --offset) {
        final char c = this.mXml.charAt(offset);
        if (c == '<') {
          int line = startingPosition.getLine();
          for (int i = offset, n = startingPosition.getOffset(); i < n; ++i) {
            if (this.mXml.charAt(i) == '\n') {
              --line;
            }
          }
          int column = 0;
          for (int j = offset - 1; j >= 0 && this.mXml.charAt(j) != '\n'; --j, ++column) {
          }
          return new Position(line, column, offset);
        }
      }
      return startingPosition;
    }

    private Position getCurrentPosition() {
      final int line = this.mLocator.getLineNumber() - 1;
      final int column = this.mLocator.getColumnNumber() - 1;
      final int xmlLength = this.mXml.length();
      while (this.mCurrentLine < line && this.mCurrentOffset < xmlLength) {
        final char c = this.mXml.charAt(this.mCurrentOffset);
        if (c == '\r' && this.mCurrentOffset < xmlLength - 1) {
          if (this.mXml.charAt(this.mCurrentOffset + 1) != '\n') {
            ++this.mCurrentLine;
            this.mCurrentColumn = 0;
          }
        } else if (c == '\n') {
          ++this.mCurrentLine;
          this.mCurrentColumn = 0;
        } else {
          ++this.mCurrentColumn;
        }
        ++this.mCurrentOffset;
      }
      this.mCurrentOffset += column - this.mCurrentColumn;
      if (this.mCurrentOffset >= xmlLength) {
        this.mCurrentOffset = xmlLength;
      }
      this.mCurrentColumn = column;
      return new Position(this.mCurrentLine, this.mCurrentColumn, this.mCurrentOffset);
    }

    @Override
    public void characters(final char[] c, final int start, final int length) throws SAXException {
      this.mPendingText.append(c, start, length);
    }

    private void flushText() {
      if (this.mPendingText.length() > 0 && !this.mStack.isEmpty()) {
        final Element element = this.mStack.get(this.mStack.size() - 1);
        final Node textNode = this.mDocument.createTextNode(this.mPendingText.toString());
        element.appendChild(textNode);
        this.mPendingText.setLength(0);
      }
    }
  }

  public static class Position {

    private final int mLine;

    private final int mColumn;

    private final int mOffset;

    private Position mEnd;

    public Position(final int line, final int column, final int offset) {
      this.mLine = line;
      this.mColumn = column;
      this.mOffset = offset;
    }

    public int getLine() {
      return this.mLine;
    }

    public int getOffset() {
      return this.mOffset;
    }

    public int getColumn() {
      return this.mColumn;
    }

    public Position getEnd() {
      return this.mEnd;
    }

    public void setEnd(@NonNull final Position end) {
      this.mEnd = end;
    }

    public SourcePosition toSourcePosition() {
      int endLine = this.mLine;
      int endColumn = this.mColumn;
      int endOffset = this.mOffset;
      if (this.mEnd != null) {
        endLine = this.mEnd.getLine();
        endColumn = this.mEnd.getColumn();
        endOffset = this.mEnd.getOffset();
      }
      return new SourcePosition(this.mLine, this.mColumn, this.mOffset, endLine, endColumn, endOffset);
    }
  }
  
  
  // @Immutable
  public static class SourcePosition {
  
    public static final SourcePosition UNKNOWN;
  
    private final int mStartLine;
  
    private final int mStartColumn;
  
    private final int mStartOffset;
  
    private final int mEndLine;
  
    private final int mEndColumn;
  
    private final int mEndOffset;
  
    public SourcePosition(final int startLine, final int startColumn, final int startOffset, final int endLine, final int endColumn, final int endOffset) {
      this.mStartLine = startLine;
      this.mStartColumn = startColumn;
      this.mStartOffset = startOffset;
      this.mEndLine = endLine;
      this.mEndColumn = endColumn;
      this.mEndOffset = endOffset;
    }
  
    public SourcePosition(final int lineNumber, final int column, final int offset) {
      this.mEndLine = lineNumber;
      this.mStartLine = lineNumber;
      this.mEndColumn = column;
      this.mStartColumn = column;
      this.mEndOffset = offset;
      this.mStartOffset = offset;
    }
  
    private SourcePosition() {
      final int n = -1;
      this.mEndOffset = n;
      this.mEndColumn = n;
      this.mEndLine = n;
      this.mStartOffset = n;
      this.mStartColumn = n;
      this.mStartLine = n;
    }
  
    protected SourcePosition(final SourcePosition copy) {
      this.mStartLine = copy.getStartLine();
      this.mStartColumn = copy.getStartColumn();
      this.mStartOffset = copy.getStartOffset();
      this.mEndLine = copy.getEndLine();
      this.mEndColumn = copy.getEndColumn();
      this.mEndOffset = copy.getEndOffset();
    }
  
    @Override
    public String toString() {
      if (this.mStartLine == -1) {
        return "?";
      }
      final StringBuilder sB = new StringBuilder(15);
      sB.append(this.mStartLine + 1);
      if (this.mStartColumn != -1) {
        sB.append(':');
        sB.append(this.mStartColumn + 1);
      }
      if (this.mEndLine != -1) {
        if (this.mEndLine == this.mStartLine) {
          if (this.mEndColumn != -1 && this.mEndColumn != this.mStartColumn) {
            sB.append('-');
            sB.append(this.mEndColumn + 1);
          }
        } else {
          sB.append('-');
          sB.append(this.mEndLine + 1);
          if (this.mEndColumn != -1) {
            sB.append(':');
            sB.append(this.mEndColumn + 1);
          }
        }
      }
      return sB.toString();
    }
  
    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof SourcePosition)) {
        return false;
      }
      final SourcePosition other = (SourcePosition) obj;
      return other.mStartLine == this.mStartLine && 
             other.mStartColumn == this.mStartColumn && 
             other.mStartOffset == this.mStartOffset && 
             other.mEndLine == this.mEndLine && 
             other.mEndColumn == this.mEndColumn && 
             other.mEndOffset == this.mEndOffset;
    }
  
    @Override
    public int hashCode() {
      return Objects.hashCode(
        this.mStartLine, this.mStartColumn, 
        this.mStartOffset, this.mEndLine, 
        this.mEndColumn, this.mEndOffset
      );
    }
  
    public int getStartLine() {
      return this.mStartLine;
    }
  
    public int getStartColumn() {
      return this.mStartColumn;
    }
  
    public int getStartOffset() {
      return this.mStartOffset;
    }
  
    public int getEndLine() {
      return this.mEndLine;
    }
  
    public int getEndColumn() {
      return this.mEndColumn;
    }
  
    public int getEndOffset() {
      return this.mEndOffset;
    }
  
    static {
      UNKNOWN = new SourcePosition();
    }
  }
  
}





