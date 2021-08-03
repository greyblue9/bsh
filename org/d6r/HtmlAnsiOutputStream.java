package org.d6r;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
@author Daniel Doubrovkine
*/
public class HtmlAnsiOutputStream extends AnsiOutputStream {

  boolean concealOn = false;
  String[][] selectedMap = FONT_TAG_MAP;

  @Override
  public void close() throws IOException {
    closeAttributes();
    super.close();
  }

  static final String[] ANSI_COLOR_MAP = { 
    "black",   // 30
    "red",     // 31
    "green",   // 32
    "yellow",  // 33
    "blue",    // 34
    "magenta", // 35
    "cyan",    // 36
    "white"    // 37
  };
  
  static final int FORE_ARR = 0;
  static final int BACK_ARR = 1;
  
  static final String[][] SPAN_TAG_MAP 
         = new String[2][ANSI_COLOR_MAP.length];
  
  static final String[][] FONT_TAG_MAP 
         = new String[2][ANSI_COLOR_MAP.length];
  
  static {
    for (int i=0; i<ANSI_COLOR_MAP.length; i++) {
      SPAN_TAG_MAP[FORE_ARR][i] = String.format(
        "<span style=\"color: %s\">", ANSI_COLOR_MAP[i]
      );
      SPAN_TAG_MAP[BACK_ARR][i] = String.format(
        "<span style=\"background-color: %s\">", ANSI_COLOR_MAP[i]
      );
      FONT_TAG_MAP[FORE_ARR][i] = String.format(
        "<font color=\"%s\">", ANSI_COLOR_MAP[i]
      );
      FONT_TAG_MAP[BACK_ARR][i] = String.format(
        "<font bgcolor=\"%s\">", ANSI_COLOR_MAP[i]
      );
    }
  }
  
  static final byte[] BYTES_QUOT = "&quot;".getBytes();
  static final byte[] BYTES_AMP  =  "&amp;".getBytes();
  static final byte[] BYTES_LT   =   "&lt;".getBytes();
  static final byte[] BYTES_GT   =   "&gt;".getBytes();
  
  public HtmlAnsiOutputStream(OutputStream os) {
    super(os);
  }

  final List<String> closingAttributes = new ArrayList<String>();

  void write(String s) throws IOException {
    super.out.write(s.getBytes());
  }

  void writeAttribute(String s) throws IOException {
    write("<" + s + ">");
    closingAttributes.add(0, s.split(" ", 2)[0]);
  }

  void closeAttributes() throws IOException {
    for (String attr : closingAttributes) {
      write("</" + attr + ">");
    }
    closingAttributes.clear();
  }
  
  public void setUseFontTag(boolean shouldUseFontTag) {
    selectedMap = shouldUseFontTag
      ? FONT_TAG_MAP
      : SPAN_TAG_MAP;
  }
  
  public void write(int data) throws IOException {
    switch((data)) {
      case 34:
        // "
        out.write(BYTES_QUOT);
        break;
      case 38:
        // &
        out.write(BYTES_AMP);
        break;
      case 60:
        // <
        out.write(BYTES_LT);
        break;
      case 62:
        // >
        out.write(BYTES_GT);
        break;
      default:
    }
  }

  public void writeLine(byte[] buf, int offset, int len) throws IOException {
    write(buf, offset, len);
    closeAttributes();
  }

  @Override
  protected void processSetAttribute(int attribute) throws IOException {
    switch((attribute)) {
      case ATTRIBUTE_CONCEAL_ON:
        write("\u001B[8m");
        concealOn = true;
        break;
      case ATTRIBUTE_INTENSITY_BOLD:
        writeAttribute("b");
        break;
      case ATTRIBUTE_INTENSITY_NORMAL:
        closeAttributes();
        break;
      case ATTRIBUTE_UNDERLINE:
        writeAttribute("u");
        break;
      case ATTRIBUTE_UNDERLINE_OFF:
        closeAttributes();
        break;
      case ATTRIBUTE_NEGATIVE_ON:
        break;
      case ATTRIBUTE_NEGATIVE_OFF:
        break;
      default:
    }
  }

  @Override
  protected void processAttributeRest() throws IOException {
    if (concealOn) {
      write("\u001B[0m");
      concealOn = false;
    }
    closeAttributes();
  }

  @Override
  protected void processSetForegroundColor(int color, boolean bright)
    throws IOException
  {
    writeAttribute(selectedMap[FORE_ARR][color]);
  }

  @Override
  protected void processSetBackgroundColor(int color, boolean bright) 
    throws IOException
  {
    writeAttribute(selectedMap[BACK_ARR][color]);
  }
}


