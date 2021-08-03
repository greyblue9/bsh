package org.d6r;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

/**
FileWalker
==========

Abstract class that walks through a directory hierarchy and 
provides subclasses with convenient hooks to add specific
behaviour.
 
This class operates with a FileFilter and maximum depth to
limit the files and direcories visited.
Commons IO supplies many common filter implementations in the
`filefilter' package.
 
The following sections describe:
 
  1. Example Implementation - example FileCleaner implementation. 
  2. Filter Example - using FileFilters with FileWalker.
  3. Cancellation - how to implement cancellation behaviour. 
 
1. Example Implementation 
-------------------------
There are many possible extensions, for example, to delete all
files and '.svn' directories, and return a list of deleted files:
 
public class FileCleaner extends FileWalker<File> {
  
  public FileCleaner() {
    super();
  }
  public List<File> clean(File startDirectory) {
    List<File> results = new ArrayList<File>();
    walk(startDirectory, results);
    return results;
  }
  public boolean handleDirectory(File directory, int depth,
  Collection<File> results) 
  {
    // delete svn directories and then skip
    if (".svn".equals(directory.getName())) {
      directory.delete();
      return false;
    } else {
      return true;
    }
  }
  
  public void handleFile(File file, int depth, 
  Collection<File> results) 
  {
    // delete file and add to list of deleted
    file.delete();
    results.add(file);
  }
}

2. Filter Example
-----------------
Choosing which directories and files to process can be a key 
aspect of using this class. This information can be setup in three
ways, via three different constructors.
 
The first option is to visit all directories and files.
This is achieved via the no-args constructor.
 
The second constructor option is to supply a single {FileFilter}
that describes the files and directories to visit. Care must be 
taken with this option as the same filter is used for both 
directories and files.
 
For example, if you wanted all directories which are not hidden
and files which end in ".txt":
 
public class FooFileWalker extends FileWalker {
  public FooFileWalker(FileFilter filter) {
    super(filter, -1);
  }
}

// Build up the filters and create the walker
// Create a filter for Non-hidden directories

IOFileFilter fooDirFilter =
FileFilterUtils.andFileFilter(FileFilterUtils.directoryFileFilter,
HiddenFileFilter.VISIBLE);

// Create a filter for Files ending in ".txt"

IOFileFilter fooFileFilter =
FileFilterUtils.andFileFilter(FileFilterUtils.fileFileFilter,
FileFilterUtils.suffixFileFilter(".txt"));

// Combine the directory and file filters using an OR condition

java.io.FileFilter fooFilter =
FileFilterUtils.orFileFilter(fooDirFilter, fooFileFilter);

// Use the filter to construct a FileWalker implementation

FooFileWalker walker = new FooFileWalker(fooFilter);
 
 
The third constructor option is to specify separate filters, one 
for directories and one for files. These are combined internally 
to form the correct FileFilter, something which is very easy to
get wrong when attempted manually, particularly when trying to
express constructs like 'any file in directories named docs'.
 
For example, if you wanted all directories which are not hidden
and files which end in ".txt":
 
public class FooFileWalker extends FileWalker {
  public FooFileWalker(IOFileFilter dirFilter,
  IOFileFilter fileFilter)
  {
    super(dirFilter, fileFilter, -1);
  }
}

// Use the filters to construct the walker
FooFileWalker walker = new FooFileWalker(
  HiddenFileFilter.VISIBLE,
  FileFilterUtils.suffixFileFilter(".txt"),
);
 
This is much simpler than the previous example, and is why it is the preferred
option for filtering.


3. Cancellation 
---------------

The FileWalker contains some of the logic required for cancel processing.
Subclasses must complete the implementation.
 
What  FileWalker  does provide for cancellation is:
 
 {CancelException} which can be thrown in any of the
 lifecycle  methods to stop processing. 
 The  walk()  method traps thrown {CancelException}
and calls the  handleCancelled()  method, providing
a place for custom cancel processing. 
 
 
Implementations need to provide:
 
 The decision logic on whether to cancel processing or not. 
 Constructing and throwing a {CancelException}. 
 Custom cancel processing in the  handleCancelled()  method.
 
 
Two possible scenarios are envisaged for cancellation:
 
  3.1 External / Mult-threaded  - cancellation being
    decided/initiated by an external process. 
  3.2 Internal  - cancellation being decided/initiated
    from within a FileWalker implementation. 
 
 
The following sections provide example implementations for these two different
scenarios.

 3.1 External / Multi-threaded 
This example provides a public cancel()  method that can be
called by another thread to stop the processing. A typical example use-case
would be a cancel button on a GUI. Calling this method sets a
 
volatile  flag to ensure it will work properly in a multi-threaded environment.
The flag is returned by the  handleIsCancelled()  method, which
will cause the walk to stop immediately. The  handleCancelled() 
method will be the next, and last, callback method received once cancellation
has occurred.
 
public class FooFileWalker extends FileWalker {
  public volatile boolean cancelled = false;
  public void cancel() {
    cancelled = true;
  }
  public boolean handleIsCancelled(File file, int depth,
  Collection results) 
  {
   return cancelled;
  }
  public void handleCancelled(File startDirectory, 
  Collection results, CancelException cancel) 
  {
    // implement processing required when a cancellation occurs
  }
}
 
 3.2 Internal 
This shows an example of how internal cancellation processing 
could be implemented.
Note: the decision logic and throwing a {CancelException} could be
implemented in any of the  lifecycle  methods.
 
public class BarFileWalker extends FileWalker {
  
  public boolean handleDirectory(File directory, int depth, 
  Collection results) 
    throws IOException
  {
    // cancel if hidden directory
    if (directory.isHidden()) {
      throw new CancelException(file, depth);
    }
    return true;
  }
  
  public void handleFile(File file, int depth, 
  Collection results) 
    throws IOException 
  {
    // cancel if read-only file
    if (!file.canWrite()) {
      throw new CancelException(file, depth);
    }
    results.add(file);
  }
  
  public void handleCancelled(File startDirectory, 
  Collection results, CancelException cancel) 
  {
    // implement processing required when a cancellation occurs
  }
}
 
@since 1.3
@version $Id: FileWalker.java 1723627 2016-01-07 21:15:47Z niallp $
*/
public class FileWalker<T> 
  implements FileWalkable<T> 
{

  /**
  The file filter to use to filter files and directories.
  */
  public final FileFilter filter;

  /**
  The limit on the directory depth to walk.
  */
  public final int depthLimit;

  /**
  Construct an instance with no filtering and unlimited  depth .
  */
  public FileWalker() {
    this(null, -1);
  }

  /**
  Construct an instance with a filter and limit the  depth  navigated to.
   
  The filter controls which files and directories will be navigated to as
  part of the walk. The {FileFilterUtils} class is useful for combining
  various filters together. A {@code null} filter means that no
  filtering should occur and all files and directories will be visited.
  @param filter  the filter to apply, null means visit all files
  @param depthLimit  controls how  deep  the hierarchy is
  navigated to (less than 0 means unlimited)
  */
  public FileWalker(final FileFilter filter, final int depthLimit)
  {
    this.filter = filter;
    this.depthLimit = depthLimit;
  }

  /**
  Construct an instance with a directory and a file filter and an optional
  limit on the  depth  navigated to.
   
  The filters control which files and directories will be navigated to as part
  of the walk. This constructor uses {FileFilterUtils#makeDirectoryOnly(IOFileFilter)}
  and {FileFilterUtils#makeFileOnly(IOFileFilter)} internally to combine the filters.
  A {@code null} filter means that no filtering should occur.
  @param directoryFilter  the filter to apply to directories, null means visit all directories
  @param fileFilter  the filter to apply to files, null means visit all files
  @param depthLimit  controls how  deep  the hierarchy is
  navigated to (less than 0 means unlimited)
  */
  public FileWalker(IOFileFilter directoryFilter, 
  IOFileFilter fileFilter, final int depthLimit) 
  {
    if (directoryFilter == null && fileFilter == null) {
      this.filter = null;
    } else {
      directoryFilter = directoryFilter != null ? directoryFilter : TrueFileFilter.TRUE;
      fileFilter = fileFilter != null ? fileFilter : TrueFileFilter.TRUE;
      directoryFilter = FileFilterUtils.makeDirectoryOnly(directoryFilter);
      fileFilter = FileFilterUtils.makeFileOnly(fileFilter);
      this.filter = FileFilterUtils.or(directoryFilter, fileFilter);
    }
    this.depthLimit = depthLimit;
  }

  //-----------------------------------------------------------------------
  /**
  Internal method that walks the directory hierarchy in a depth-first manner.
   
  Users of this class do not need to call this method. This method will
  be called automatically by another (public) method on the specific subclass.
   
  Writers of subclasses should call this method to start the directory walk.
  Once called, this method will emit events as it walks the hierarchy.
  The event methods have the prefix  handle .
  @param startDirectory  the directory to start from, not null
  @param results  the collection of result objects, may be updated
  @throws NullPointerException if the start directory is null
  @throws IOException if an I/O Error occurs
  */
  public final void walk(final File startDirectory, 
  final Collection<T> results) 
    throws IOException 
  {
    if (startDirectory == null) {
      throw new NullPointerException("Start Directory is null");
    }
    try {
      handleStart(startDirectory, results);
      walk(startDirectory, 0, results);
      handleEnd(results);
    } catch (final CancelException cancel) {
      handleCancelled(startDirectory, results, cancel);
    }
  }

  /**
  Main recursive method to examine the directory hierarchy.
  @param directory  the directory to examine, not null
  @param depth  the directory level (starting directory = 0)
  @param results  the collection of result objects, may be updated
  @throws IOException if an I/O Error occurs
  */
  public void walk(final File directory, final int depth, 
  final Collection<T> results) 
    throws IOException 
  {
    checkIfCancelled(directory, depth, results);
    if (handleDirectory(directory, depth, results)) {
      handleDirectoryStart(directory, depth, results);
      final int childDepth = depth + 1;
      if (depthLimit < 0 || childDepth <= depthLimit) {
        checkIfCancelled(directory, depth, results);
        File[] childFiles = filter == null ? directory.listFiles() : directory.listFiles(filter);
        childFiles = filterDirectoryContents(directory, depth, childFiles);
        if (childFiles == null) {
          handleRestricted(directory, childDepth, results);
        } else {
          for (final File childFile : childFiles) {
            if (childFile.isDirectory()) {
              walk(childFile, childDepth, results);
            } else {
              checkIfCancelled(childFile, childDepth, results);
              handleFile(childFile, childDepth, results);
              checkIfCancelled(childFile, childDepth, results);
            }
          }
        }
      }
      handleDirectoryEnd(directory, depth, results);
    }
    checkIfCancelled(directory, depth, results);
  }

  //-----------------------------------------------------------------------
  /**
  Checks whether the walk has been cancelled by calling {#handleIsCancelled},
  throwing a  CancelException  if it has.
   
  Writers of subclasses should not normally call this method as it is called
  automatically by the walk of the tree. However, sometimes a single method,
  typically {#handleFile}, may take a long time to run. In that case,
  you may wish to check for cancellation by calling this method.
  @param file  the current file being processed
  @param depth  the current file level (starting directory = 0)
  @param results  the collection of result objects, may be updated
  @throws IOException if an I/O Error occurs
  */
  public final void checkIfCancelled(final File file, 
  final int depth, final Collection<T> results) 
    throws IOException 
  {
    if (handleIsCancelled(file, depth, results)) {
      throw new CancelException(file, depth);
    }
  }

  /**
  Overridable callback method invoked to determine if the entire walk
  operation should be immediately cancelled.
   
  This method should be implemented by those subclasses that want to
  provide a public cancel()  method available from another
  thread. The design pattern for the subclass should be as follows:
   
  public class FooFileWalker extends FileWalker {
  public volatile boolean cancelled = false;
  public void cancel() {
  cancelled = true;
  }
  public void handleIsCancelled(File file, int depth, Collection results) {
  return cancelled;
  }
  public void handleCancelled(File startDirectory,
  Collection results, CancelException cancel) {
  // implement processing required when a cancellation occurs
  }
  }
   
   
  If this method returns true, then the directory walk is immediately
  cancelled. The next callback method will be {#handleCancelled}.
   
  This implementation returns false.
  @param file  the file or directory being processed
  @param depth  the current directory level (starting directory = 0)
  @param results  the collection of result objects, may be updated
  @return true if the walk has been cancelled
  @throws IOException if an I/O Error occurs
  */
  public boolean handleIsCancelled(final File file, 
  final int depth, final Collection<T> results) 
    throws IOException 
  {
    // do nothing - overridable by subclass
    return false;// not cancelled

  }

  /**
  Overridable callback method invoked when the operation is cancelled.
  The file being processed when the cancellation occurred can be
  obtained from the exception.
   
  This implementation just re-throws the {CancelException}.
  @param startDirectory  the directory that the walk started from
  @param results  the collection of result objects, may be updated
  @param cancel  the exception throw to cancel further processing
  containing details at the point of cancellation.
  @throws IOException if an I/O Error occurs
  */
  public void handleCancelled(final File startDirectory, 
  final Collection<T> results, final CancelException cancel) 
    throws IOException 
  {
    // re-throw exception - overridable by subclass
    throw cancel;
  }

  //-----------------------------------------------------------------------
  /**
  Overridable callback method invoked at the start of processing.
   
  This implementation does nothing.
  @param startDirectory  the directory to start from
  @param results  the collection of result objects, may be updated
  @throws IOException if an I/O Error occurs
  */
  public void handleStart(final File startDirectory, 
  final Collection<T> results) 
    throws IOException 
  {
    // do nothing - overridable by subclass
  }

  /**
  Overridable callback method invoked to determine if a directory should be processed.
   
  This method returns a boolean to indicate if the directory should be examined or not.
  If you return false, the entire directory and any subdirectories will be skipped.
  Note that this functionality is in addition to the filtering by file filter.
   
  This implementation does nothing and returns true.
  @param directory  the current directory being processed
  @param depth  the current directory level (starting directory = 0)
  @param results  the collection of result objects, may be updated
  @return true to process this directory, false to skip this directory
  @throws IOException if an I/O Error occurs
  */
  public boolean handleDirectory(final File directory,
  final int depth, final Collection<T> results)
    throws IOException 
  {
    // do nothing - overridable by subclass
    return true;// process directory
  }

  /**
  Overridable callback method invoked at the start of processing each directory.
   
  This implementation does nothing.
  @param directory  the current directory being processed
  @param depth  the current directory level (starting directory = 0)
  @param results  the collection of result objects, may be updated
  @throws IOException if an I/O Error occurs
  */
  public void handleDirectoryStart(final File directory, 
  final int depth, final Collection<T> results)
    throws IOException 
  {
    // do nothing - overridable by subclass
  }

  /**
  Overridable callback method invoked with the contents of each directory.
   
  This implementation returns the files unchanged
  @param directory  the current directory being processed
  @param depth  the current directory level (starting directory = 0)
  @param files the files (possibly filtered) in the directory, may be {@code null}
  @return the filtered list of files
  @throws IOException if an I/O Error occurs
  @since 2.0
  */
  public File[] filterDirectoryContents(final File directory,
  final int depth, final File[] files) 
    throws IOException 
  {
    return files;
  }

  /**
  Overridable callback method invoked for each (non-directory) file.
   
  This implementation does nothing.
  @param file  the current file being processed
  @param depth  the current directory level (starting directory = 0)
  @param results  the collection of result objects, may be updated
  @throws IOException if an I/O Error occurs
  */
  public void handleFile(final File file, final int depth,
  final Collection<T> results)
    throws IOException 
  {
    // do nothing - overridable by subclass
  }

  /**
  Overridable callback method invoked for each restricted directory.
   
  This implementation does nothing.
  @param directory  the restricted directory
  @param depth  the current directory level (starting directory = 0)
  @param results  the collection of result objects, may be updated
  @throws IOException if an I/O Error occurs
  */
  public void handleRestricted(final File directory, 
  final int depth, final Collection<T> results) 
    throws IOException
  {
    // do nothing - overridable by subclass
  }

  /**
  Overridable callback method invoked at the end of processing each directory.
   
  This implementation does nothing.
  @param directory  the directory being processed
  @param depth  the current directory level (starting directory = 0)
  @param results  the collection of result objects, may be updated
  @throws IOException if an I/O Error occurs
  */
  public void handleDirectoryEnd(final File directory,
  final int depth, final Collection<T> results)
    throws IOException
  {
    // do nothing - overridable by subclass
  }

  /**
  Overridable callback method invoked at the end of processing.
   
  This implementation does nothing.
  @param results  the collection of result objects, may be updated
  @throws IOException if an I/O Error occurs
  */
  public void handleEnd(final Collection<T> results) 
    throws IOException 
  {
    // do nothing - overridable by subclass
  }

  //-----------------------------------------------------------------------
}


interface FileWalkable<T> {
  /**
  CancelException is thrown in FileWalker to cancel the current
  processing.
  */
  public static class CancelException extends IOException {
    /**
    Serialization id. */
    public static final long serialVersionUID 
      = 1347339620135041008L;
    /**
    The file being processed when the exception was thrown. */
    public final File file;
    /**
    The file depth when the exception was thrown. */
    public final int depth;
    /**
    Constructs a  CancelException  with
    the file and depth when cancellation occurred.
    @param file  the file when the operation was cancelled, 
      may be null
    @param depth  the depth when the operation was cancelled,
      may be null
    */
    public CancelException(final File file, final int depth) {
      this("Operation Cancelled", file, depth);
    }
    /**
    Constructs a  CancelException  with
    an appropriate message and the file and depth when
    cancellation occurred.
    @param message  the detail message
    @param file  the file when the operation was cancelled
    @param depth  the depth when the operation was cancelled
    */
    public CancelException(final String message, 
    final File file, final int depth) 
    {
      super(message);
      this.file = file;
      this.depth = depth;
    }
    /**
    Return the file when the operation was cancelled.
    @return the file when the operation was cancelled
    */
    public File getFile() {
      return file;
    }
    /**
    Return the depth when the operation was cancelled.
    @return the depth when the operation was cancelled
    */
    public int getDepth() {
      return depth;
    }
  }
  
  File[] filterDirectoryContents(File directory, int depth, 
  File[] files) throws IOException;
  
  void handleCancelled(File startDirectory, Collection<T> results,
  CancelException cancel) throws IOException;
  
  boolean handleDirectory(File directory, int depth,
  Collection<T> results) throws IOException;
  
  void handleDirectoryEnd(File directory, int depth, 
  Collection<T> results) throws IOException;
  
  void handleDirectoryStart(File directory, int depth, 
  Collection<T> results) throws IOException;
  
  void handleEnd(Collection<T> results) throws IOException;
  
  void handleFile(File file, int depth, Collection<T> results) 
    throws IOException;
  
  boolean handleIsCancelled(File file, int depth,
  Collection<T> results) throws IOException;
  
  void handleRestricted(File directory, int depth, 
  Collection<T> results) throws IOException;
  
  void handleStart(File startDirectory, Collection<T> results) 
    throws IOException;
  
  void walk(File directory, int depth, Collection<T> results) 
    throws IOException; 
}


 