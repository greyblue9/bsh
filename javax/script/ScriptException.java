package javax.script;

/**
The generic Exception class for the Scripting APIs.  Checked
exception types thrown by underlying scripting implementations must be wrapped in instances of
ScriptException.

The class has members to store line and column numbers and
filenames if this information is available.

@author Mike Grogan
@since 1.6
*/
public class ScriptException extends Exception {

  private String fileName;
  private int lineNumber;
  private int columnNumber;

  /**
  Creates a ScriptException with a String to be used in its
  message. Filename, and line and column numbers are unspecified.
  
  @param s The String to use in the message.
  */
  public ScriptException(String s) {
    super(s);
    fileName = null;
    lineNumber = -1;
    columnNumber = -1;
  }

  /**
  Creates a ScriptException wrapping an Exception thrown by an
  underlying interpreter. Line and column numbers and filename are
  unspecified.
  
  @param e The wrapped Exception.
  */
  public ScriptException(Exception e) {
    super(e);
    fileName = null;
    lineNumber = -1;
    columnNumber = -1;
  }

  /**
  Creates a ScriptException with message, filename, and linenumber
  to be used in error messages.
  
  @param message The string to use in the message
  @param fileName The file or resource name describing the 
    location of a script error causing the ScriptException to be 
    thrown.
  @param lineNumber A line number describing the location of a
    script error causing the ScriptException to be thrown.
  */
  public ScriptException(String message, String fileName,
  int lineNumber)
  {
    super(message);
    this.fileName = fileName;
    this.lineNumber = lineNumber;
    this.columnNumber = -1;
  }

  /**
  ScriptException constructor specifying message, filename, line 
  number, and column number.
  
  @param message The message.
  @param fileName The filename
  @param lineNumber the line number.
  @param columnNumber the column number.
  */
  public ScriptException(String message, String fileName,
  int lineNumber, int columnNumber)
  {
    super(message);
    this.fileName = fileName;
    this.lineNumber = lineNumber;
    this.columnNumber = columnNumber;
  }

  /**
  Returns a message containing the String passed to a constructor,
  as well as line and column numbers and filename, if any of these
  are known.
  
  @return The error message.
  */
  public String getMessage() {
    String ret = super.getMessage();
    if (fileName != null) {
      ret += (" in " + fileName);
      if (lineNumber != -1) {
        ret += " at line number " + lineNumber;
      }
      if (columnNumber != -1) {
        ret += " at column number " + columnNumber;
      }
    }
    return ret;
  }

  /**
  Get the line number on which an error occurred.
  
  @return The line number.  Returns -1 if a line number is 
  unavailable.
  */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
  Get the column number on which an error occurred.
  
  @return The column number.  Returns -1 if a column number is 
  unavailable.
  */
  public int getColumnNumber() {
    return columnNumber;
  }

  /**
  Get the source of the script causing the error.
  
  @return The file name of the script or some other string 
  describing the script source. May return some implementation-
  defined string such as `unknown'
  if a description of the source is unavailable.
  */
  public String getFileName() {
    return fileName;
  }
}

