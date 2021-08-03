package org.d6r;

import java.util.Set;


public interface RelatedClassFinder {
  boolean tryFindRelated(String className, Set<? super String> dest);
}


