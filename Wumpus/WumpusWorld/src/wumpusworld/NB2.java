package wumpusworld;

import java.util.ArrayList;

public class NB2 {

    public ArrayList<int[]> neighnours = new ArrayList<int[]>();
    private ArrayList<double[]> probability = new ArrayList<double[]>();
    private ArrayList<Block> visitedcell = new ArrayList<>();
    private ArrayList<int[]> combs = new ArrayList<int[]>();
    private World w,w2;
    private int visitedcells = 0;
    private int[] wumpus_coor = new int[2];
    private  int pit_c = 0;
    private boolean found_wumpus;
    public static double PIT_PROB = 0.2;
    private static double WUMPUS_PROB = 0.0667;
    private static final int WUMPUS = 2;
    public static final double OFFSET = 0.1;
    public static final double RISK = 0.25;

    public NB2(World world) {
        w = world;
        initiate();
    }
    public Block percept(World w,int a, int b,boolean marked){
        Block blk = new Block();
        blk.x = a;
        blk.y = b;

        blk.breeze = w.hasBreeze(a, b);
        if (w.hasPit(a, b))
            blk.pit = true;
        if (w.hasStench(a, b))
            blk.stench = true;
        if (w.hasWumpus(a, b))
            blk.Wum_pus = true;
        if (w.hasGlitter(a, b))
            blk.gold = true;
        if (blk.stench)
            blk.Danger = true;
        if(!blk.breeze || !blk.Danger || !blk.stench || !blk.pit )
            blk.safe = true;
        blk.visited= true;
        if(marked == true)
            blk.marked = true;
        return blk;
    }

    public void initiate(){
        w2 = w.cloneWorld();
        visitedcell.add(percept(w,1,1,true));
        getNeighbors(1,1);
    }

  
    public void visitedBlock(int x, int y,boolean marked) {

        Block b = new Block();
        if(!(getVisited(visitedcell,x,y,true))){
            neighnours.add(new int[]{x, y, 0});
            probability.add(new double[]{0,0});
            visitedcell.add(percept(w,x,y,marked));
            w2.setMarked(x, y);
            visitedcells =+ 1;
        }
        else         System.out.println("("+x+","+y+") has been set");
    }

   
    private void getNeighbors(int x, int y) {
        if (!(w.isValidPosition(x, y)) || w2.hasMarked(x,y)  || getVisited(visitedcell,x,y,false)) {
            return;
        }
        if (w.isUnknown(x, y)) {
            visitedBlock(x, y,true);
        }
        else
        {
            w2.setMarked(x,y);
            setmark(x,y);
            perceive_neighbours(x,y);
        }
    }

    
    public void setmark(int x,int y){
        for (Block block : visitedcell) {
            if (block.x == x && block.y == y && (!block.marked)) {
                block.marked = true;
            }
        }
    }

    public void perceive_neighbours(int x,int y){
        setNeighbours(x + 1, y );
        setNeighbours(x - 1, y);
        setNeighbours(x, y + 1);
        setNeighbours(x, y - 1);
    }

    public void setNeighbours(int x, int y){
        getNeighbors(x,y);
    }

  
    public boolean getVisited(ArrayList<Block> visitedlist,int x,int y,boolean marked){
        for (Block block : visitedlist) {
            if (block.x == x && block.y == y && block.marked == marked) {
                return true;
            }
        }
        return false;
    }

    
    public ArrayList<double[]> get_probability(int obstacle) {

        double p = 0;
        if (obstacle == 1) {
            p = PIT_PROB;
        } else if (obstacle == 2) {
            p = WUMPUS_PROB;
        } else {
            System.out.println("OUT OF CONDITION RANGE");
        }

        for (int i = 0; i < neighnours.size(); i++) {

            ArrayList<ArrayList<int []>> combinations = new ArrayList<ArrayList<int[]>>();
            int nbit = (int) Math.pow(2, neighnours.size());
            int[] count = new int[nbit];
            ArrayList<int[]> neighbours1 = cloneArrayList(neighnours);
            neighbours1.remove(i);
            for(int k=0; k<nbit; k++)
            {
                int c = 0;
                ArrayList<int[]> comb = cloneArrayList(neighbours1);
                for(int j=0; j<neighbours1.size(); j++)
                {
                    int tmp = (int) Math.pow(2,j);
                    if((tmp & k)!= 0){
                        comb.get(j)[2]=1;  /*stores different combinations*/
                        c++;
                    }
                }

                combinations.add(comb);
                count[k] = c;
            }

            int[] query_true = new int[]{neighnours.get(i)[0], neighnours.get(i)[1],1};
            int[] query_false = new int[]{neighnours.get(i)[0], neighnours.get(i)[1],0};

            double true_positive=0;
            double false_positive=0;
            double f = 0;

            for(int j = 0; j < combinations.size(); j++)
            {
                int sum = combinations.get(j).size();
                String msg = "Pit Not Sensed";
                if((obstacle==1 && count[j]<4) || (obstacle==2 && count[j]<2))
                {
                    if(get_possible_pit_wumpus_neighbours(combinations.get(j),obstacle,query_true))
                    {
                        double add = Math.pow(p, count[j]) * Math.pow(1 - p, sum - count[j]);
                        true_positive += add;
                    }else System.out.println(msg);

                    if(get_possible_pit_wumpus_neighbours(combinations.get(j),obstacle,query_false))
                    {
                        double v = Math.pow(p, count[j]) * Math.pow(1 - p, sum - count[j]);
                        double add = v;
                        false_positive += add;
                    }else System.out.println(msg);
                }
            }

            true_positive = p*true_positive;
            false_positive = (1-p)*false_positive;


            try{
                f= true_positive/(true_positive+false_positive);
            }catch (ArithmeticException e) {
                System.out.println("ERROR: dividing a number by zero not possible!");
            }

            probability.get(i)[obstacle-1]=f;

            if(obstacle==2 && f==1){
                set_wumpus_position(neighnours.get(i)[0],neighnours.get(i)[1]);
                return probability;
            }

        }
        return probability;
    }

    
    public void set_wumpus_position(int x, int y){
        found_wumpus = true;
        wumpus_coor[0] = x;
        wumpus_coor[1] = y;
    }


    private boolean get_possible_pit_wumpus_neighbours(ArrayList<int[]> arrayList, int condition, int[] query) {

        World w3 = w.cloneWorld();
        boolean sense_pit = true;
        ArrayList<int[]> conject = cloneArrayList(arrayList);
        conject.add(query);

        if(condition==1)
        {
            for (int x = 1; x <= w3.getSize(); x++) {
                for (int y = 1; y <= w3.getSize(); y++)
                {
                    if(!w3.isUnknown(x,y)&&w3.hasPit(x,y)){
                        w3.markSurrounding(x,y);
                    }
                }
            }
        }


        for (int[] ints : conject) {
            int cx, cy;
            cx = ints[0];
            cy = ints[1];
            if (ints[2] == 1) {
                w3.markSurrounding(cx, cy);
            }
        }

        return check_pit_surrounding(w3, sense_pit, condition);
    }

    public boolean check_pit_surrounding(World w1,boolean sense_pit,int condition){
        for (int x = 1; x <= w1.getSize(); x++) {
            for (int y = 1; y <= w1.getSize(); y++) {
                if (!(w1.isUnknown(x, y))) {
                    if(condition==1) {
                        if (!(w1.hasBreeze(x, y) == w1.hasMarked(x, y))) {
                            sense_pit = false;
                        }
                    }
                    else {
                        if (!(w1.hasStench(x, y) == w1.hasMarked(x, y))) {
                            sense_pit = false;
                        }
                    }
                }

                if (!sense_pit) {
                    return false;
                }
            }
        }
        return true;
    }

    public int[][] markSurrounding(int x,int y,int[][] array1){
        array1[x+1][y] = 1;
        array1[x][y+1] = 1;
        array1[x][y-1] = 1;
        array1[x-1][y] = 1;
        return array1;
    }

    
    private ArrayList<int[]> cloneArrayList(ArrayList<int[]> arrayList){
        ArrayList<int[]> clone = new ArrayList<int[]>(arrayList.size());
        for (int[] ints : arrayList) {
            clone.add(ints.clone());
        }
        return  clone;
    }

    
    public int npits(){
        for (int x = 1; x <= w.getSize(); x++) {
            for (int y = 1; y <= w.getSize(); y++)
            {
                if(!w.isUnknown(x,y)&&w.hasPit(x,y)){
                    pit_c += 1;
                }
            }
        }
        return pit_c;
    }

    public double get_pit_prob(){
        return (3-(double) this.npits())/(16-this.visitedcells);
    }

    public boolean get_wumpus_coor(int[] position){

        double visitedcells = this.visitedcells;
        WUMPUS_PROB = 1/(16-visitedcells);

        get_probability(WUMPUS);

        if(found_wumpus){
            position[0]=wumpus_coor[0];
            position[1]=wumpus_coor[1];
        }
        return found_wumpus;
    }


    public void update_wumpus_pos(int[] position){
        wumpus_coor[0]=position[0];
        wumpus_coor[1]=position[1];
    }

    public boolean get_distance(int[] goalA, int[] goalB){

        int x = w.getPlayerX();
        int y = w.getPlayerY();
        int distanceA = Math.abs(x-goalA[0])+Math.abs(y-goalA[1]);
        int distanceB = Math.abs(x-goalB[0])+Math.abs(y-goalB[1]);
        return distanceA > distanceB;

    }

}
