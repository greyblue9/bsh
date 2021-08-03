package org.d6r;

import java.io.IOException;
import java.util.Locale;
import org.d6r.Ansi.Attribute;
import org.d6r.Ansi.Color;
import java.nio.*;
import java.util.Deque;
import java.util.ArrayDeque;


/**
Renders ANSI color escape-codes in strings by parsing out some special syntax to pick up the correct fluff to use.
The syntax for embedded ANSI codes is:
<pre>
<tt>@|</tt><em>code</em>(<tt>,</tt><em>code</em>)* <em>text</em><tt>|@</tt>
</pre>
Examples:
<pre>
<tt>@|bold Hello|@</tt>
</pre>
<pre>
<tt>@|bold,red Warning!|@</tt>
</pre>
@author <a href="mailto:jason@planet57.com">Jason Dillon</a>
@author <a href="http://hiramchirino.com">Hiram Chirino</a>
@since 1.1
*/
public class AnsiRenderer {

  public static final String BEGIN_TOKEN = "\u001b[";

  public static final String END_TOKEN = "m";

  public static final String CODE_TEXT_SEPARATOR = ";";

  public static final String CODE_LIST_SEPARATOR = ";";

  private static final int BEGIN_TOKEN_LEN = 2;

  private static final int END_TOKEN_LEN = 1;


  /**
  Renders the given input to the target Appendable.
  @param input
  source to render
  @param target
  render onto this target Appendable.
  @return the given Appendable
  @throws IOException
  If an I/O error occurs
  */
  public static CharBuffer render(final String input)
    throws IOException     
  {
    Deque<String> que = new ArrayDeque<String>();
    CharBuffer out = CharBuffer.allocate(
      (int)  (((float)input.length()) * 50.75f)
    );
    int i = 0;
    int j, k;
    while (i < input.length()) {
      j = input.indexOf(BEGIN_TOKEN, i);
      if (j == -1) {
        if (i == 0) {
          out.put(input);
          return out;
        }
        out.put(input.substring(i, input.length()));
        return out;
      }
      out.put(input.substring(i, j));
      k = input.indexOf(END_TOKEN, j);
      if (k == -1) {
        out.put(input);
        return out;
      }
      j += BEGIN_TOKEN_LEN;
      String spec = input.substring(j, k);
      String[] items = spec.split(CODE_TEXT_SEPARATOR, 2);
      /*if (items.length == 1) {
        out.put(input);
        return out;
      }*/
      render(
        "", out, que,
        spec.split(CODE_LIST_SEPARATOR)
      );
      i = k + END_TOKEN_LEN;
    }
    return out;
  }
 
  
  public static CharBuffer render(final String text, CharBuffer out, 
  Deque<String> que, final String... codes)
  {
    Ansi ansi = Ansi.ansi();
    for (String name : codes) {
      
      Code code = Code.values()[
        Integer.parseInt(name, 10)
      ];
      
      if (code.isColor()) {
        int value = code.getColor().value();
        
        String tag = HtmlAnsiOutputStream.FONT_TAG_MAP
        [  code.isBackground()? 1 :0 ]
        [  value  ];
        
        out.put(tag);        
        que.offerLast("</font>");       
        out.put(text);
      }
      
      if (code == Code.RESET) {
        out.put(text);
        if (que.isEmpty()) que.offerLast("</font>");
        while (! que.isEmpty()) {
          out.put(que.pollLast());
        }        
      }
    }
    return out;
  }

  public static boolean test(final String text) {
    return text != null && text.contains(BEGIN_TOKEN);
  }

  public enum Code {
    
    RESET(Attribute.RESET),
    INTENSITY_BOLD(Attribute.INTENSITY_BOLD),
    INTENSITY_FAINT(Attribute.INTENSITY_FAINT), 
    ITALIC(Attribute.ITALIC), 
    UNDERLINE(Attribute.UNDERLINE), 
    BLINK_SLOW(Attribute.BLINK_SLOW), 
    BLINK_FAST(Attribute.BLINK_FAST), 
    BLINK_OFF(Attribute.BLINK_OFF), // 7
    
    UNUSED_08( 8),
    UNUSED_09( 9),
    
    BLACK(Color.BLACK), 
    RED(Color.RED), 
    GREEN(Color.GREEN), 
    YELLOW(Color.YELLOW), 
    BLUE(Color.BLUE), 
    MAGENTA(Color.MAGENTA), 
    CYAN(Color.CYAN), 
    WHITE(Color.WHITE),
    UNUSED_18(18),
    UNUSED_19(19),
    
    UNUSED_20(20), // 20
    UNUSED_21(21),
    UNUSED_22(22),
    UNUSED_23(23),
    UNUSED_24(24),
    UNUSED_25(25),
    UNUSED_26(26),
    UNUSED_27(27),
    UNUSED_28(28),
    UNUSED_29(29),
    
    // Colors
    FG_BLACK(Color.BLACK, false), 
    FG_RED(Color.RED, false), 
    FG_GREEN(Color.GREEN, false), 
    FG_YELLOW(Color.YELLOW, false), 
    FG_BLUE(Color.BLUE, false), 
    FG_MAGENTA(Color.MAGENTA, false), 
    FG_CYAN(Color.CYAN, false), 
    FG_WHITE(Color.WHITE, false),
    UNUSED_38(38),
    UNUSED_39(39),
    
    // Background Colors
    BG_BLACK(Color.BLACK, true), 
    BG_RED(Color.RED, true), 
    BG_GREEN(Color.GREEN, true), 
    BG_YELLOW(Color.YELLOW, true), 
    BG_BLUE(Color.BLUE, true), 
    BG_MAGENTA(Color.MAGENTA, true), 
    BG_CYAN(Color.CYAN, true), 
    BG_WHITE(Color.WHITE, true),
    UNUSED_48(48),
    UNUSED_49(49),
    // Attributes
    NEGATIVE_ON(Attribute.NEGATIVE_ON), 
    NEGATIVE_OFF(Attribute.NEGATIVE_OFF), 
    CONCEAL_ON(Attribute.CONCEAL_ON), 
    CONCEAL_OFF(Attribute.CONCEAL_OFF), 
    UNDERLINE_DOUBLE(Attribute.UNDERLINE_DOUBLE), 
    UNDERLINE_OFF(Attribute.UNDERLINE_OFF),     // Aliases
    BOLD(Attribute.INTENSITY_BOLD),
    FAINT(Attribute.INTENSITY_FAINT);

    @SuppressWarnings("unchecked")
    private final Enum n;
    public final int ord;
    private final boolean background;

    @SuppressWarnings("unchecked")
    Code(final Enum n, boolean background) {
      this.n = n;
      this.background = background;
      this.ord = -1;
    }

    @SuppressWarnings("unchecked")
    Code(final Enum n) {
      this(n, false);     
    }

    @SuppressWarnings("unchecked")
    Code(int ord) {
      this.n = null;
      this.background = false;
      this.ord = ord;
    }

    public boolean isColor() {
      return n instanceof Ansi.Color;
    }

    public Ansi.Color getColor() {
      return (Ansi.Color) n;
    }

    public boolean isAttribute() {
      return n instanceof Attribute;
    }

    public Attribute getAttribute() {
      return (Attribute) n;
    }

    public boolean isBackground() {
      return background;
    }
  }

  private AnsiRenderer() {
  }
}