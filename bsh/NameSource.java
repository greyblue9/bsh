package bsh;

public interface NameSource {
  String[] getAllNames();

  void addNameSourceListener(NameSource.Listener var1);

  public interface Listener {
    void nameSourceChanged(NameSource var1);
  }
}
