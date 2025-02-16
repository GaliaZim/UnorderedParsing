package NodeMappingAlgorithms;

import structures.GeneGroup;
import structures.Mapping;
import structures.Node;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class NodeMappingAlgorithm {
    /**
     * The string to map
     */
    ArrayList<GeneGroup> string;
    /**
     * The node to map
     */
    Node node;
    /**
     * The maximum number of deletions from the tree
     */
    int treeDeletionLimit;
    /**
     * The maximum number of deletions from the string
     */
    int stringDeletionLimit;
    /**
     * {@code substitutionFunction(A,B)} returns the score for substituting the leaf symbol A with
     * the character B of {@code string}.
     */
    BiFunction<GeneGroup,GeneGroup,Double> substitutionFunction;
    /**
     * {@code deletionCost(A)} returns the cost for deleting a leaf with label A or a character A of the string
     */
    Function<GeneGroup, Double> deletionCost;
    /**
     * key: an index of {@code string}
     * value: a list of mappings between {@code node} and substrings of {@code string} ending at {@code key}.
     * endPoint --> list of mappings
     */
    HashMap<Integer, List<Mapping>> resultMappingsByEndPoints;

    NodeMappingAlgorithm(ArrayList<GeneGroup> string, Node node, int treeDeletionLimit, int stringDeletionLimit,
                         BiFunction<GeneGroup, GeneGroup, Double> substitutionFunction,
                         Function<GeneGroup, Double> deletionCost) {
        this.string = string;
        this.node = node;
        this.treeDeletionLimit = treeDeletionLimit;
        this.stringDeletionLimit = stringDeletionLimit;
        this.substitutionFunction = substitutionFunction;
        this.deletionCost = deletionCost;
        resultMappingsByEndPoints = new HashMap<>(); //TODO: capacity
    }

    /**
     * Puts all the mappings between {@code node} and every possible (according to {@code treeDeletionLimit}
     * and {@code stringDeletionLimit}) substring of {@code string} in {@code resultMappingsByEndPoints}
     * by the end point of the substring.
     */
    public abstract void runAlgorithm();

    /**
     * @return All the mappings as calculated by {@code runAlgorithm} hashed by their end points.
     * If {@code runAlgorithm} was not called, returns an empty {@code HashMap}
     */
    public HashMap<Integer, List<Mapping>> getResultMappingsByEndPoints() {
        return resultMappingsByEndPoints;
    }

    /**
     * @return The string to be mapped
     */
    public ArrayList<GeneGroup> getString() {
        return string;
    }

    /**
     * @return The node to be mapped
     */
    public Node getNode() {
        return node;
    }

    //For debugging
    public void printResultMappings() {
        resultMappingsByEndPoints.forEach((endPoint, ls) -> {
            System.out.println(endPoint + ": ");
            ls.forEach(mapping -> {
                System.out.println(mapping);
                if (!mapping.getScore().equals(Double.NEGATIVE_INFINITY)) {
                    mapping.getChildrenMappings().forEach(System.out::println);
                    System.out.println("Deleted:");
                    System.out.println("Descendants:");
                    mapping.getDeletedDescendant().forEach(child ->
                            System.out.println(child.getLabel()));
                    System.out.println("String Indices:");
                    mapping.getDeletedStringIndices().forEach(System.out::println);
                }
                System.out.println("----");
            });
            System.out.println("*********");
        });
    }

    /**
     * @return the one-to-one mapping between the leaves descendants of {@code node} and
     * {@code string} according to the best mapping of the tree rooted in {@code node}
     * Best mapping according to the {@code Mapping::compareTo} method.
     * If {@code runAlgorithm} was not called, returns null.
     */
    public HashMap<Integer,Node> getBestStringIndexToLeafMapping() {
        HashMap<Integer, Node> mappingToReturn = null;
        Mapping maxMapping = getBestMapping();
        if(maxMapping != null) {
            mappingToReturn = maxMapping.getOneToOneMappingByStringIndices();
        }
        return mappingToReturn;
    }

    public Mapping getBestMapping() {
        return resultMappingsByEndPoints.values().stream()
                .flatMap(Collection::stream).max(Mapping::compareTo).orElse(null);
    }

    public List<Mapping> getAllPossibleMappings() {
        return getAllPossibleMappings(Double.NEGATIVE_INFINITY);
    }

    public List<Mapping> getAllPossibleMappings(Double threshold) {
        List<Mapping> mappingList = resultMappingsByEndPoints.values().stream()
                .flatMap(mappings -> mappings.stream()
                        .filter(mapping -> mapping != null && mapping.getScore().compareTo(threshold) > 0))
                .sorted(Mapping::compareTo)
                .collect(Collectors.toList());
        Collections.reverse(mappingList);
        return mappingList;
    }

    public List<Mapping> getBestPossibleMappingsForDistinctIndices() {
        return getBestPossibleMappingsForDistinctIndices(Double.NEGATIVE_INFINITY);
    }

    public List<Mapping> getBestPossibleMappingsForDistinctIndices(Double threshold) {
        HashMap<Integer, List<Mapping>> mappingsByStartIndex = new HashMap<>();
        mapOfMappingsListsToStreamOfBestMappingFromEachList(threshold, resultMappingsByEndPoints)
                .forEach(mapping -> {
                    int startIndex = mapping.getStartIndex();
                    mappingsByStartIndex.putIfAbsent(startIndex, new ArrayList<>());
                    mappingsByStartIndex.get(startIndex).add(mapping);
                });
        List<Mapping> mappingList =
                mapOfMappingsListsToStreamOfBestMappingFromEachList(threshold, mappingsByStartIndex)
                .sorted(Mapping::compareTo)
                .collect(Collectors.toList());
        Collections.reverse(mappingList);
        return mappingList;
    }

    private Stream<Mapping> mapOfMappingsListsToStreamOfBestMappingFromEachList(Double threshold,
                                                                               HashMap<Integer, List<Mapping>>
                                                                                       mapOfMappingsLists) {
        return mapOfMappingsLists.values().stream()
                .map(listOfMappings -> listOfMappings.stream().max(Mapping::compareTo).orElse(null))
                .filter(mapping -> mapping != null && mapping.getScore().compareTo(threshold) > 0);
    }
}
