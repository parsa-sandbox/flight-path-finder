//this program gets a list of flights with their time and cost as input and 
//based on the request file it will taylor the output to show the cheapest or fastest
// flight to the destination
import java.io.*;
import java.util.*;

public class flightPlannerApp {
    public static void main(String[] args) {
        String flightDataFile = "flight_data.txt";
        String requestsFile   = "requests.txt";
        String outputFile     = "output.txt";

        // Load graph
        Graph graph = new Graph();
        try {
            graph.loadFlightData(flightDataFile);

            // Read flight requests
            List<Request> requests = Request.readRequests(requestsFile);
            PrintWriter writer = new PrintWriter(new FileWriter(outputFile));

            FlightPlanner planner = new FlightPlanner(graph);
            int flightCount = 1;
            for (Request req : requests) {
                // Header for each requested flight
                writer.printf("Flight %d: %s, %s (%s)%n",
                        flightCount++,
                        req.origin,
                        req.destination,
                        req.orderBy.equalsIgnoreCase("T") ? "Time" : "Cost");

                // Find all possible paths
                List<FlightPlan> plans = planner.findAllPaths(req.origin, req.destination);
                if (plans.isEmpty()) {
                    writer.printf("No available flight plan from %s to %s.%n%n",
                            req.origin, req.destination);
                    continue;
                }

                // Sort by chosen criterion
                if (req.orderBy.equalsIgnoreCase("T")) {
                    plans.sort(Comparator.comparingInt(fp -> fp.totalTime));
                } else {
                    plans.sort(Comparator.comparingDouble(fp -> fp.totalCost));
                }

                // Output top 3 or fewer
                int limit = Math.min(3, plans.size());
                for (int i = 0; i < limit; i++) {
                    FlightPlan plan = plans.get(i);
                    writer.printf("Path %d: %s. Time: %d Cost: %.2f%n",
                            i + 1,
                            String.join(" -> ", plan.cities),
                            plan.totalTime,
                            plan.totalCost);
                }
                writer.println();
            }
            writer.close();
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}

// Represents an edge in the graph (a flight leg)
class Edge {
    String to;
    int cost;
    int time;

    Edge(String to, int cost, int time) {
        this.to = to;
        this.cost = cost;
        this.time = time;
    }
}

// Graph with adjacency list representation
class Graph {
    Map<String, List<Edge>> adj = new HashMap<>();

    // Loads flight data from file and populates adjacency lists
    void loadFlightData(String filename) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            int n = Integer.parseInt(br.readLine().trim());
            for (int i = 0; i < n; i++) {
                String[] parts = br.readLine().split("\\|");
                String c1 = parts[0].trim();
                String c2 = parts[1].trim();
                int cost = Integer.parseInt(parts[2].trim());
                int time = Integer.parseInt(parts[3].trim());

                addEdge(c1, c2, cost, time);
                addEdge(c2, c1, cost, time);
            }
        }
    }

    // Adds a directed edge from 'from' to 'to'
    void addEdge(String from, String to, int cost, int time) {
        adj.computeIfAbsent(from, k -> new LinkedList<>()).add(new Edge(to, cost, time));
        // Ensure 'to' node exists in map for lookup, even if no outgoing
        adj.computeIfAbsent(to, k -> new LinkedList<>());
    }
}

// Represents a flight request
class Request {
    String origin;
    String destination;
    String orderBy; // "T" for time, "C" for cost

    Request(String o, String d, String ob) {
        origin = o;
        destination = d;
        orderBy = ob;
    }

    // Reads requested flights from file
    static List<Request> readRequests(String filename) throws IOException {
        List<Request> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            int m = Integer.parseInt(br.readLine().trim());
            for (int i = 0; i < m; i++) {
                String[] parts = br.readLine().split("\\|");
                list.add(new Request(
                    parts[0].trim(),
                    parts[1].trim(),
                    parts[2].trim()
                ));
            }
        }
        return list;
    }
}

// Holds one complete flight plan
class FlightPlan {
    List<String> cities;
    double totalCost;
    int totalTime;

    FlightPlan(List<String> cities, double cost, int time) {
        this.cities = cities;
        this.totalCost = cost;
        this.totalTime = time;
    }
}

// Finds all paths using iterative DFS with backtracking
class FlightPlanner {
    private final Graph graph;

    FlightPlanner(Graph g) {
        this.graph = g;
    }

    List<FlightPlan> findAllPaths(String origin, String dest) {
        List<FlightPlan> result = new ArrayList<>();
        if (!graph.adj.containsKey(origin) || !graph.adj.containsKey(dest)) {
            return result;
        }

        Set<String> visited = new HashSet<>();
        Deque<PathState> stack = new ArrayDeque<>();

        visited.add(origin);
        stack.push(new PathState(origin, 0, new ArrayList<>(Arrays.asList(origin)), 0, 0));

        while (!stack.isEmpty()) {
            PathState state = stack.peek();
            List<Edge> edges = graph.adj.get(state.city);

            if (state.nextIndex >= edges.size()) {
                // Backtrack
                visited.remove(state.city);
                stack.pop();
                continue;
            }

            Edge edge = edges.get(state.nextIndex++);
            String nextCity = edge.to;
            if (visited.contains(nextCity)) {
                continue;
            }

            // Build new path
            List<String> newPath = new ArrayList<>(state.path);
            newPath.add(nextCity);
            double newCost = state.cost + edge.cost;
            int newTime = state.time + edge.time;

            if (nextCity.equals(dest)) {
                // Found a complete plan
                result.add(new FlightPlan(newPath, newCost, newTime));
            } else {
                // Continue DFS
                visited.add(nextCity);
                stack.push(new PathState(nextCity, 0, newPath, newCost, newTime));
            }
        }
        return result;
    }

    // Internal state for iterative DFS
    private static class PathState {
        String city;
        int nextIndex;
        List<String> path;
        double cost;
        int time;

        PathState(String city, int nextIndex, List<String> path, double cost, int time) {
            this.city = city;
            this.nextIndex = nextIndex;
            this.path = path;
            this.cost = cost;
            this.time = time;
        }
    }
}
