package libcore.io;


public final class StructStat
{
  public final long st_dev;
  public final long st_ino;
  public final int st_mode;
  public final long st_nlink;
  public final int st_uid;
  public final int st_gid;
  public final long st_rdev;
  public final long st_size;
  public final long st_atime;
  public final long st_mtime;
  public final long st_ctime;
  public final long st_blksize;
  public final long st_blocks;
  
  StructStat(final long st_dev, final long st_ino, final int st_mode, final long st_nlink, final int st_uid, final int st_gid, final long st_rdev, final long st_size, final long st_atime, final long st_mtime, final long st_ctime, final long st_blksize, final long st_blocks) {
    this.st_dev = st_dev;
    this.st_ino = st_ino;
    this.st_mode = st_mode;
    this.st_nlink = st_nlink;
    this.st_uid = st_uid;
    this.st_gid = st_gid;
    this.st_rdev = st_rdev;
    this.st_size = st_size;
    this.st_atime = st_atime;
    this.st_mtime = st_mtime;
    this.st_ctime = st_ctime;
    this.st_blksize = st_blksize;
    this.st_blocks = st_blocks;
  }
}
