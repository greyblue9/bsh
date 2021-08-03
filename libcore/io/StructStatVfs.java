package libcore.io;


public final class StructStatVfs
{
  public final long f_bavail;
  public final long f_bfree;
  public final long f_blocks;
  public final long f_bsize;
  public final long f_favail;
  public final long f_ffree;
  public final long f_files;
  public final long f_flag;
  public final long f_frsize;
  public final long f_fsid;
  public final long f_namemax;
  
  StructStatVfs(final long f_bsize, final long f_frsize, final long f_blocks, final long f_bfree, final long f_bavail, final long f_files, final long f_ffree, final long f_favail, final long f_fsid, final long f_flag, final long f_namemax) {
    this.f_bsize = f_bsize;
    this.f_frsize = f_frsize;
    this.f_blocks = f_blocks;
    this.f_bfree = f_bfree;
    this.f_bavail = f_bavail;
    this.f_files = f_files;
    this.f_ffree = f_ffree;
    this.f_favail = f_favail;
    this.f_fsid = f_fsid;
    this.f_flag = f_flag;
    this.f_namemax = f_namemax;
  }
}
