package org.d6r;

import java.util.*;
import java8.util.Optional;
import javax.annotation.Nullable;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.BaseTree;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.Token;
import org.antlr.runtime.CommonToken;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
  
  
public class AntlrUtil {
  
  public static List<Tree> searchTree(Tree tree, int targetType) {
    List<Tree> found = new ArrayList<Tree>();
    Set<Tree> visited = new IdentityHashSet<Tree>();
    Deque<Tree> q = new ArrayDeque<Tree>();
    q.offer(tree);
    while (!q.isEmpty()) {
      Tree node = q.poll();
      if (visited.contains(node)) continue;
      visited.add(node);
      if (node.getType() == targetType) found.add(node);
      List<Tree> children = getChildren(node);
      if (children == null) continue;
      q.addAll(children);
    }
    return found;    
  }
  
  public static SortedMap<Tree, ? extends List<Tree>> searchTreeArguments(
  final Tree tree, final int targetType)
  {
    return (SortedMap<Tree, ArrayList<Tree>>) 
             (SortedMap<? extends Tree, ? extends Object>)
              searchTreeArgument(tree, targetType, (Integer)null);
  }
  
  
  public static <S extends Object>
  SortedMap<Tree, S> searchTreeArgument(final Tree tree, final int targetType,
  final @Nullable Integer argumentIndex)
  {
    SortedMap<Tree, S> nodeArgsMap 
      = new TreeMap<Tree, S>(Comparators.comparingInt("startIndex"));
    List<Tree> found = searchTree(tree, targetType); 
    for (Tree node: found) {
      List<Tree> siblings = new ArrayList<Tree>(),
                 children = getChildren(node);
      if (children != null) {
        siblings.addAll(children);
      }
      Tree parent = node.getParent();
      if (parent != null) {
        int nodeChildIndex = node.getChildIndex();
        List<Tree> siblingsWithSelf = getChildren(parent);
        List<Tree> successorSiblings
          = siblingsWithSelf.subList(nodeChildIndex + 1, siblingsWithSelf.size()); 
        siblings.addAll(successorSiblings);
      }
      if (argumentIndex != null) {
        int targetIndex = ((Integer) argumentIndex).intValue();
        if (siblings.size() < targetIndex+1) continue;
        nodeArgsMap.put(node, (S) siblings.get(targetIndex));
      } else {
        nodeArgsMap.put(node, (S) siblings);
      }
    }
    return nodeArgsMap;
  }
  
  
  
  public static <L extends Iterable<? extends Tree> & Cloneable>
  Optional<? extends L> filterNonTerminals(L nodesInput)
  {
    try {
      L clonedNodes = Reflector.<L>invokeOrDefault(nodesInput, "clone");
      for (Iterator<? extends Tree> it = clonedNodes.iterator(); it.hasNext();) {
        Tree node = it.next();
        if (isNonTerminal(node)) it.remove();
      }
      if (false) throw new CloneNotSupportedException();
      return Optional.of(clonedNodes);
    } catch (CloneNotSupportedException ex) {
      throw Reflector.Util.sneakyThrow(ex);
    }
  }
  
  
  public static List<Tree> getDescendants(Tree tree) {
    List<Tree> found = new ArrayList<Tree>();
    Set<Tree> visited = new IdentityHashSet<Tree>();
    Deque<Tree> q = new ArrayDeque<Tree>();
    q.offer(tree);
    while (!q.isEmpty()) {
      Tree node = q.poll();
      if (visited.contains(node)) continue;
      visited.add(node);
      List<Tree> children = getChildren(node);
      if (children != null) {
        for (Tree child: children) {
          found.add(node);
          found.add(child);
          q.offerFirst(child);
        };
      };
    };
    return found;
  }
 
  
  public static <T extends Tree> List<T> getChildren(Tree node) {
    if (node instanceof BaseTree) {
      List<T> children = (List<T>) (List<?>) ((BaseTree) node).getChildren();
      return children != null? children: Collections.<T>emptyList();
    }
    final int childCount = node.getChildCount();
    if (childCount == 0) return Collections.<T>emptyList();
    List<T> children = new ArrayList<T>(childCount);
    for (int index=0; index<childCount; ++index) {
      children.add((T) (Object) node.getChild(index));
    }
    return children;
  }
  
  
  public static boolean isNonTerminal(final Tree node) {
    int start, stop, lnpos, line;
    CommonTree treeNode;
    if (node instanceof CommonTree
    && ((treeNode = (CommonTree)node)).getToken() instanceof CommonToken) {
      final CommonToken token = (CommonToken) treeNode.getToken();
      start = token.getStartIndex();
      stop  = token.getStopIndex();
      lnpos = token.getCharPositionInLine();
      line  = token.getLine();
    } else {
      start = node.getTokenStartIndex();
      stop  = node.getTokenStopIndex();
      lnpos = node.getCharPositionInLine();
      line  = node.getLine();
    }
    return (lnpos < 0 || line < 0 || (start == 0 && stop == 0));    
  }
 
}



