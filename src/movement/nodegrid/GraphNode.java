package movement.nodegrid;

import core.Coord;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class GraphNode {
    private Set<GraphNode> adjacentNodes = new HashSet<>();
    private Coord location;

    public GraphNode(Coord location) {
        this.location = location;
    }

    public Coord getLocation() {
        return location;
    }

    public void setLocation(Coord location) {
        this.location = location;
    }

    public void addAdjacentNode(GraphNode other) {
        this.adjacentNodes.add(other);
    }

    public Set<GraphNode> getAdjacentNodes() {
        return adjacentNodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode graphNode = (GraphNode) o;
        return getLocation().equals(graphNode.getLocation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLocation());
    }
}
