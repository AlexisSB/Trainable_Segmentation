package trainableSegmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.process.ImageConverter;

import java.awt.*;
import java.util.*;


public class IntelligentScissors {

    ImagePlus saturation;
    Point[] neighbours = new Point[9];
    PixelNode[][] infoMatrix = null;
    boolean activeFlag = false;

    private ArrayList<Point> userSelectedPoints = new ArrayList<Point>();


    IntelligentScissors(){
        setupNeighbours();
    }

    public ArrayList<Point> getUserSelectedPoints() {
        ArrayList<Point> copyOfPoints = new ArrayList<Point>();

        for(Point p : userSelectedPoints){
            copyOfPoints.add(new Point(p.x,p.y));
        }
        return copyOfPoints;
    }

    public PointRoi getUserSelectedPointROI(){
        if( userSelectedPoints.size()> 0) {
            int[] xArray = new int[userSelectedPoints.size()];
            int[] yArray = new int[userSelectedPoints.size()];
            for(int i = 0 ; i < userSelectedPoints.size(); i++){
                xArray[i] = userSelectedPoints.get(i).x;
                yArray[i] = userSelectedPoints.get(i).y;
            }

            PointRoi output = new PointRoi(xArray, yArray, userSelectedPoints.size());
            return output;
        }else{
            return null;
        }
    }

    public void addUserSelectedPoints(Point point) {
        if(point != null) {
            this.userSelectedPoints.add(point);
        }
    }

    private void setupNeighbours(){
        int count = 0;
        for( int i = -1; i <= 1; i++){
            for(int j = -1; j <=1 ; j++){
                neighbours[count] = new Point(i,j);
                count++;
            }
        }
    }

    private void setupInfoMatrix(){
        infoMatrix = new PixelNode[saturation.getWidth()][saturation.getHeight()];

        for(int row = 0 ; row < infoMatrix.length; row++){
            for(int col = 0 ; col < infoMatrix[row].length; col++){
                infoMatrix[row][col] = new PixelNode(col, row);
            }
        }

    }
    /**
     *  Converts RGB image into saturation channel image used for scissors.
     *
     * @param src - RGB image of slice
     */
    public void setImage(ImagePlus src){
        ImagePlus temp = (ImagePlus) src.clone();
        ImageConverter ic = new ImageConverter(temp);
        ic.convertToHSB();
        temp.setSlice(2);
        ImagePlus saturation = new ImagePlus();
        saturation.setProcessor(temp.getProcessor());

        //StackWindow satWindow = new StackWindow(saturation);

        this.saturation = saturation;
        setupInfoMatrix();
    }

    public void reset(){
        userSelectedPoints.clear();
        setupInfoMatrix();
    }


    /*ArrayList<Point> visitedNodes = new ArrayList<Point>();
    ArrayList<Point> activeNodes = new ArrayList<Point>();
    ArrayList<Point> knownNodes = new ArrayList<Point>();
*/
    public ArrayList<Point> calculateShortestPath(Point start, Point goal) {
        System.out.println(" Point a");
        Point temp = new Point(goal.x, goal.y);
        ArrayList<Point> path = new ArrayList<Point>();
        System.out.println(" Point b");
        //path.add(new Point(temp.x, temp.y));
        while (!temp.equals(start)) {
            System.out.println(" Point c");
            //System.out.println(temp);
            //IJ.log(temp.toString());
            Point previous = infoMatrix[temp.y][temp.x].previous;
            temp.x += previous.x;
            temp.y += previous.y;
            System.out.println(" Point d");
            path.add(new Point(temp.x, temp.y));

        }

        //System.out.println(temp);
        //path.add(new Point(temp.x, temp.y));
        System.out.println("calculateShortestPath returns : \n" + path);
        return path;
    }

    public Roi drawShortestPath(){

        ArrayList<Point> path = new ArrayList<Point>();

        System.out.println("User Selected Points : \n"+ userSelectedPoints);
        if (userSelectedPoints.size() <2){
            path.addAll(userSelectedPoints);
        }else {
            for (int i = 0; i < userSelectedPoints.size() - 1; i++) {
                System.out.println("Start : " + userSelectedPoints.get(i) + " End : " + userSelectedPoints.get(i+1));
                System.out.println("User Points : " + userSelectedPoints);
                Point start = new Point(userSelectedPoints.get(i).x, userSelectedPoints.get(i).y);
                Point end = new Point(userSelectedPoints.get(i+1).x, userSelectedPoints.get(i+1).y);
                djikstraSearch(start,end);
                ArrayList<Point> shortestPath = calculateShortestPath(start, end);
                Collections.reverse(shortestPath);
                path.addAll(shortestPath);

            }
            path.add(userSelectedPoints.get(userSelectedPoints.size()-1));
        }

        System.out.println(path);
        int[] xArray = new int[path.size()];
        int[] yArray = new int[path.size()];
        for (int i = 0; i < path.size(); i++) {
            xArray[i] = path.get(i).x;
            yArray[i] = path.get(i).y;
        }
        System.out.println(Arrays.toString(xArray));
        System.out.println(Arrays.toString(yArray));
        PolygonRoi pathRoi = new PolygonRoi(xArray, yArray, path.size(), Roi.POLYLINE);
        return pathRoi;

    }

    public Roi drawShortestPath(Point[] selectedPoints){
        reset();
        ArrayList<Point> path = new ArrayList<Point>();

        if (selectedPoints.length <2){
            for( int i =0; i < selectedPoints.length; i++){
                path.add(new Point(selectedPoints[i].x, selectedPoints[i].y));
            }
        }else {
            for (int i = 0; i < selectedPoints.length - 1; i++) {
                System.out.println("Start : " + selectedPoints[i] + " End : " + selectedPoints[i+1]);
                Point start = new Point(selectedPoints[i].x, selectedPoints[i].y);
                Point end = new Point(selectedPoints[i+1].x, selectedPoints[i+1].y);
                System.out.println(" Point 1");
                djikstraSearch(start,end);
                System.out.println(" Point 2");
                ArrayList<Point> shortestPath = calculateShortestPath(start, end);
                System.out.println(" Point 3");
                Collections.reverse(shortestPath);
                path.addAll(shortestPath);
                System.out.println(" Point 4");
            }
            //Add the last point.
            path.add(selectedPoints[selectedPoints.length-1]);
            System.out.println(" Point 5");
        }

        System.out.println(path);
        int[] xArray = new int[path.size()];
        int[] yArray = new int[path.size()];
        for (int i = 0; i < path.size(); i++) {
            xArray[i] = path.get(i).x;
            yArray[i] = path.get(i).y;
        }
        System.out.println(Arrays.toString(xArray));
        System.out.println(Arrays.toString(yArray));
        PolygonRoi pathRoi = new PolygonRoi(xArray, yArray, path.size(), Roi.POLYLINE);
        return pathRoi;

    }

    public void djikstraSearch(Point start, Point goal){

        PriorityQueue<PixelNode> pq = new PriorityQueue<PixelNode>();

        //Setup first node
        PixelNode seed = infoMatrix[start.y][start.x];
        seed.cost = 0;

        pq.add(seed);

        while(!pq.isEmpty()){
            PixelNode node = pq.poll();

            //Check if goal
            if(node.getPoint().equals(goal)){
                return;
            }

            //Set point to visited;
            node.state = State.VISITED;

            //Explore the neighbours
            for( int i =0; i < neighbours.length; i++){
                Point neighbour = node.getPoint();
                neighbour.x += neighbours[i].x;
                neighbour.y += neighbours[i].y;

                //Bounds check
                if(neighbour.x > 0 && neighbour.y > 0 && neighbour.x < saturation.getWidth() && neighbour.y < saturation.getHeight()) {

                    PixelNode nextNode = infoMatrix[neighbour.y][neighbour.x];

                    if (nextNode.state == State.INITIAL) {
                        //Check cost and update if less
                        double totalcost = node.cost + calculateLinkCost(node.getPoint(), nextNode.getPoint(), goal);
                        nextNode.cost = totalcost;
                        nextNode.previous = neighbours[8 - i];
                        nextNode.state = State.ACTIVE;
                        pq.add(nextNode);

                    } else if (nextNode.state == State.ACTIVE) {
                        //Do nothing
                        double totalcost = node.cost + calculateLinkCost(node.getPoint(), nextNode.getPoint(),goal);
                        if (totalcost < nextNode.cost) {
                            pq.remove(nextNode);
                            nextNode.cost = totalcost;
                            nextNode.previous = neighbours[8 - i];
                            nextNode.state = State.ACTIVE;
                            pq.add(nextNode);
                        }
                    }
                }
            }
        }
    }

    private double calculateLinkCost(Point one, Point two, Point goal) {

        int diagonalTest = Math.abs(two.x-one.x)+ Math.abs(two.y - one.y);
        double diagonalMultiplier = 1.0;
        if (diagonalTest == 2){
            diagonalMultiplier = 1.41;
        }

        int distanceFromEnd = Math.abs(goal.y-two.y)+Math.abs(goal.x-goal.x);


        int grayIntensity = saturation.getPixel(two.x, two.y)[0];
        //IJ.log(Arrays.toString(saturation.getPixel(two.x, two.y)));
        return ((255-grayIntensity) + distanceFromEnd)*diagonalMultiplier;

    }




    public enum State {INITIAL, ACTIVE, VISITED};

    protected class PixelNode implements Comparator<PixelNode>, Comparable<PixelNode> {

        int x;
        int y;
        double cost;
        Point previous;
        State state;

        public PixelNode(Point p){
            this.x = p.x;
            this.y = p.y;
            state = State.INITIAL;
            previous = null;
        }

        public PixelNode(int x, int y){
            this.x = x;
            this.y = y;
            state = State.INITIAL;
            previous = null;
        }


        @Override
        public int compareTo(PixelNode o) {
            double output = this.cost-o.cost;
            return (int)output;
        }

        @Override
        public int compare(PixelNode o1, PixelNode o2) {
            double output = o1.cost - o2.cost;
            return (int)output;
        }

        Point getPoint(){
            return new Point(x,y);
        }

    }




}
