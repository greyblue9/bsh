package org.d6r;


public interface IObservable {
  
  void addObserver(IObserver observer);
  int countObservers();
  void deleteObserver(IObserver observer);
  void deleteObservers();
  // boolean hasChanged()
  void notifyObservers();
  void notifyObservers(Object data);
  
}



