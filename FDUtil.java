import java.util.Set;
import java.util.HashSet;

/**
 * This utility class is not meant to be instantitated, and just provides some
 * useful methods on FD sets.
 * 
 * @author David
 * @version 5/18/2022
 */
public final class FDUtil {

  /**
   * Resolves all trivial FDs in the given set of FDs
   * 
   * @param fdset (Immutable) FD Set
   * @return a set of trivial FDs with respect to the given FDSet
   */
  public static FDSet trivial(final FDSet fdset) {
    FDSet result = new FDSet();
    for (FD fd : fdset) {
      // the left hand of any FD can trivially determine all of its subsets
      // (i.e., power set of the left hand side)
      for (Set<String> attrs : FDUtil.powerSet(fd.getLeft())) {
        if (!attrs.isEmpty()) {
          result.add(new FD(fd.getLeft(), attrs));
        }
      }
    }
    return result;
  }

  /**
   * Augments every FD in the given set of FDs with the given attributes
   * 
   * @param fdset FD Set (Immutable)
   * @param attrs a set of attributes with which to augment FDs (Immutable)
   * @return a set of augmented FDs
   */
  public static FDSet augment(final FDSet fdset, final Set<String> attrs) {
    FDSet result = new FDSet();
    for (FD fd : fdset) {
      FD newFD = new FD(fd);
      newFD.addToLeft(attrs);
      newFD.addToRight(attrs);
      result.add(newFD);
    }
    return result;
  }

  /**
   * Exhaustively resolves transitive FDs with respect to the given set of FDs
   * 
   * @param fdset (Immutable) FD Set
   * @return all transitive FDs with respect to the input FD set
   */
  public static FDSet transitive(final FDSet fdset) {
    int last;
    FDSet result = new FDSet();
    do {
      last = result.size();

      // take the union between the result and the given fdset
      FDSet union = new FDSet(fdset);
      union.addAll(result);

      // examine every pair of FDs in the union and check for transitivity
      for (FD alpha : union) {
        for (FD beta : union) {
          if (alpha != beta && alpha.getRight().containsAll(beta.getLeft())) {
            result.add(new FD(alpha.getLeft(), beta.getRight()));
          }
        }
      }
    } while (result.size() != last);
    return result;
  }

  /**
   * Generates the closure of the given FD Set
   * 
   * @param fdset (Immutable) FD Set
   * @return the closure of the input FD Set
   */
  public static FDSet fdSetClosure(final FDSet fdset) {
    int last;
    FDSet current = new FDSet(fdset);

    // all attributes represented in the FD set
    Set<String> attrSet = new HashSet<>();
    for (FD f : fdset) {
      attrSet.addAll(f.getLeft());
      attrSet.addAll(f.getRight());
    }
    Set<Set<String>> attrPset = FDUtil.powerSet(attrSet);

    // repeated applications of Armstrong's axioms
    // until no further changes are detected.
    do {
      last = current.size();

      // union with augmentations of current FD set with all subsets of attrs
      for (Set<String> attrs : attrPset) {
        current.addAll(FDUtil.augment(current, attrs));
      }
      // union with trivial dependencies
      current.addAll(FDUtil.trivial(current));

      // union with transitive dependencies
      current.addAll(FDUtil.transitive(current));
    } while (current.size() != last);

    return current;
  }

  /**
   * Generates the power set of the given set (that is, all subsets of
   * the given set of elements)
   * 
   * @param set Any set of elements (Immutable)
   * @return the power set of the input set
   */
  @SuppressWarnings("unchecked")
  public static <E> Set<Set<E>> powerSet(final Set<E> set) {

    // base case: power set of the empty set is the set containing the empty set
    if (set.size() == 0) {
      Set<Set<E>> basePset = new HashSet<>();
      basePset.add(new HashSet<>());
      return basePset;
    }

    // remove the first element from the current set
    E[] attrs = (E[]) set.toArray();
    set.remove(attrs[0]);

    // recurse and obtain the power set of the reduced set of elements
    Set<Set<E>> currentPset = FDUtil.powerSet(set);

    // restore the element from input set
    set.add(attrs[0]);

    // iterate through all elements of current power set and union with first
    // element
    Set<Set<E>> otherPset = new HashSet<>();
    for (Set<E> attrSet : currentPset) {
      Set<E> otherAttrSet = new HashSet<>(attrSet);
      otherAttrSet.add(attrs[0]);
      otherPset.add(otherAttrSet);
    }
    currentPset.addAll(otherPset);
    return currentPset;
  }
}