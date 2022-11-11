import java.util.HashSet;
import java.util.Set;

/**
 * This class provides static methods for performing normalization
 * 
 * @author Seung Park
 * @version 10.31.22
 */
public class Normalizer {

  /**
   * Performs BCNF decomposition
   * 
   * @param rel   A relation (as an attribute set)
   * @param fdset A functional dependency set
   * @return a set of relations (as attribute sets) that are in BCNF
   */
  public static Set<Set<String>> BCNFDecompose(Set<String> rel, FDSet fdset) {
    // First test if the given relation is already in BCNF with respect to
    // the provided FD set.
    Set<String> relationCopy = new HashSet<>(rel);
    FDSet fdsetCopy = new FDSet(fdset);
    Set<Set<String>> allSuperkeys = findSuperkeys(relationCopy, fdset);
    if (isBCNF(relationCopy, fdset)) {
      return allSuperkeys;
    }

    // Identify a nontrivial FD that violates BCNF. Split the relation's
    // attributes using that FD, as seen in class.
    FD theFD = new FD();
    for (FD fd : fdset) {
      if (!fd.isTrivial() && !allSuperkeys.contains(fd.getLeft())) {
        theFD = fd;
        break;
      }
    }
    // split the relation on the violating FD
    Set<String> leftSchema = new HashSet<>();
    leftSchema.addAll(theFD.getLeft());
    leftSchema.addAll(theFD.getRight());
    Set<String> rightSchema = new HashSet<>();
    rightSchema.addAll(theFD.getLeft());
    Set<String> intoRight = relationCopy;
    // remove everything in the right side of the FD from the relation
    for (String attribute : theFD.getRight()) {
      intoRight.remove(attribute);
    }
    rightSchema.addAll(intoRight);

    // redistribute dependencies into F+ onto left schema & right schema
    FDSet fdSetClosure = FDUtil.fdSetClosure(fdsetCopy);
    FDSet closureLeft = new FDSet();
    FDSet closureRight = new FDSet();
    // Iterate through closure of the given set of FDs
    for (FD fd : fdSetClosure) {
      // union attributes in the fd
      Set<String> allAttr = new HashSet<>();
      allAttr.addAll(fd.getLeft());
      allAttr.addAll(fd.getRight());
      // if it is a subset of the left schema, add to the left fdset
      if (leftSchema.containsAll(allAttr)) {
        closureLeft.add(fd);
      }
      // if it is a subset of the right schema, add to the right fdset
      if (rightSchema.containsAll(allAttr)) {
        closureRight.add(fd);
      }
      // decompose the two relations recursively & return the union of their
      // decompositions
      Set<Set<String>> decomp = BCNFDecompose(leftSchema, closureLeft);
      decomp.addAll(BCNFDecompose(rightSchema, closureRight));
      return decomp;
    }

    // Repeat the above until all relations are in BCNF
    return null;
  }

  /**
   * Tests whether the given relation is in BCNF. A relation is in BCNF iff the
   * left-hand attribute set of all nontrivial FDs is a super key.
   * 
   * @param rel   A relation (as an attribute set)
   * @param fdset A functional dependency set
   * @return true if the relation is in BCNF with respect to the specified FD set
   */
  public static boolean isBCNF(Set<String> rel, FDSet fdset) {
    // gets the list of all the superkeys
    Set<Set<String>> allSuperkeys = findSuperkeys(rel, fdset);
    // checks whether all left-hand attributes of all nontrivial FDs
    // is a part of the set of superkeys
    for (FD fd : fdset) {
      if (!fd.isTrivial() && !allSuperkeys.contains(fd.getLeft())) {
        return false;
      }
    }

    return true;
  }

  /**
   * This method returns a set of super keys
   * 
   * @param rel   A relation (as an attribute set)
   * @param fdset A functional dependency set
   * @return a set of super keys
   */
  public static Set<Set<String>> findSuperkeys(Set<String> rel, FDSet fdset) {
    // set to contain all of the superkeys
    Set<Set<String>> allSuperkeys = new HashSet<>();

    // sanity check: are all the attributes in the FD set even in the
    // relation? Throw an IllegalArgumentException if not.
    Set<String> copy = rel;
    for (FD fd : fdset) {
      if (!copy.containsAll(fd.getLeft()) || !copy.containsAll(fd.getRight())) {
        throw new IllegalArgumentException(
            "FD refers to unknown attributes: " + fd.getLeft() + " --> " + fd.getRight());
      }
    }

    // iterate through each subset of the relation's attributes, and test
    // the attribute closure of each subset
    Set<Set<String>> theSet = FDUtil.powerSet(rel);
    for (Set<String> attr : theSet) {
      Set<String> theAttrs = attributeClosure(attr, fdset);
      if (theAttrs.equals(rel)) {
        allSuperkeys.add(attr);
      }
    }

    return allSuperkeys;
  }

  /**
   * This method returns the attribute closure
   *
   * @param attr  An attribute set
   * @param fdset A functional dependency set
   * @return a set attributes
   */
  public static Set<String> attributeClosure(Set<String> attr, FDSet fdset) {
    // deep copy of attributes
    Set<String> copy = new HashSet<>(attr);

    // adds right side to attribute set if left side is a subset of the inputted
    // attributes
    for (FD fd : fdset) {
      if (copy.containsAll(fd.getLeft())) {
        copy.addAll(fd.getRight());
      }
    }

    // stops recursing when the attribute closure doesn't change
    if (attr.equals(copy)) {
      return copy;
    }

    return attributeClosure(copy, fdset);
  }
}