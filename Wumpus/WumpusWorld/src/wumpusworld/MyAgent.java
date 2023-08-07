package wumpusworld;

import java.util.ArrayList;

public class MyAgent implements Agent
{
    private World w;
    private int target_x = 0;
    private int target_y = 0;
    private boolean has_target = false;
    private boolean gun_shoot = false;
    private int pits = 0;
    private int[] end = {0,0};
    private int wumpus_sta = NOT_IDENTIFIED;
    private int[] wumpus_coor= {0,0};
    private int[] arrow_target = {0,0};
    private int[] coordinate = {0,0};
    private  boolean identify_bug = false;
    public static final int NOT_IDENTIFIED = 0;
    public static final int IDENTIFIED = 1;
    public static final int DEAD = 2;




    /**
     * Creates a new instance of your solver agent.
     *
     * @param world Current world state
     */
    public MyAgent(World world)
    {
        w = world;
    }


    /**
     * Asks your solver agent to execute an action.
     */

    public void doAction()
    {

        int cX = w.getPlayerX();
        int cY = w.getPlayerY();

        if (w.hasGlitter(cX, cY))
        {
            w.doAction(World.A_GRAB);
            return;
        }

        if (w.isInPit())
        {
            w.doAction(World.A_CLIMB);
            return;
        }

        if (w.hasBreeze(cX, cY))
        {
            System.out.println("I am in a Breeze");
        }
        if (w.hasStench(cX, cY))
        {
            System.out.println("I am in a Stench");
        }
        if (w.hasPit(cX, cY))
        {
            System.out.println("I am in a Pit");
        }
        if (w.getDirection() == World.DIR_RIGHT)
        {
            System.out.println("I am facing Right");
        }
        if (w.getDirection() == World.DIR_LEFT)
        {
            System.out.println("I am facing Left");
        }
        if (w.getDirection() == World.DIR_UP)
        {
            System.out.println("I am facing Up");
        }
        if (w.getDirection() == World.DIR_DOWN)
        {
            System.out.println("I am facing Down");
        }

        if(cX==target_x && cY==target_y){
            has_target=false;
        }

        if(!has_target){

            NB2 nb = new NB2(w);

            if(wumpus_sta==NOT_IDENTIFIED){
                if(nb.get_wumpus_coor(wumpus_coor)){
                    wumpus_sta = IDENTIFIED;   /*Estimate the position of the wumpus*/
                }
            }
            else if (wumpus_sta==IDENTIFIED){
                nb.update_wumpus_pos(wumpus_coor);
            }
            int[] goal = new int[2];
            gun_shoot = get_target(goal,wumpus_sta,nb);
            has_target = true;
            target_x = goal[0];
            target_y = goal[1];
            if(gun_shoot){
                arrow_target[0]=target_x;
                arrow_target[1]=target_y;
            }
        }

        coordinate[0]=0;
        coordinate[1]=1;
        move_to_target(cX,cY);

    }

    /**
    *Get the possible safe cell for making the next move
    * @param position
    * wumpus_sta
    * nbH
     */
    public boolean get_target(int[] position,int wumpus_sta,NB2 nb){
        nb.npits();
        NB2.PIT_PROB = nb.get_pit_prob();


        int index;
        boolean shoot=false;
        boolean nodanger=false;
        double pit_prob = 0.2;

        ArrayList<double[]> probability =    nb.get_probability(1);

        if(wumpus_sta==NOT_IDENTIFIED){

            double minimu_wumpus=1;
            int n = -1;
            while(n<0)
            {
                for(int i = 0; i< probability.size(); i++)
                {

                    double chance_wumpus = probability.get(i)[1];
                    double chance_pit = probability.get(i)[0];

                    if(chance_wumpus<=minimu_wumpus && chance_pit<pit_prob) {
                        if (chance_wumpus == minimu_wumpus && n>=0) {
                            if(nb.get_distance(nb.neighnours.get(i), nb.neighnours.get(n))) {
                                continue;
                            }
                        }
                        minimu_wumpus=chance_wumpus;
                        n=i;
                    }
                }

                pit_prob += NB2.OFFSET;
            }

            index = n;
            if(probability.get(index)[1]> NB2.RISK && w.hasArrow()){
                shoot = true;
            }
        }



        else{

            double min_pit=1;
            int n=-1;

            while(!nodanger)
            {
                for(int i = 0; i< probability.size(); i++){

                    double p = probability.get(i)[0];
                    if(p<=min_pit) {
                        if (p == min_pit && n>=0 && nb.get_distance(nb.neighnours.get(i), nb.neighnours.get(n))) {
                            continue;
                        }
                        min_pit=p;
                        n=i;
                    }
                }

                if(wumpus_sta!=MyAgent.DEAD){
                    if(wumpus_coor[0]== nb.neighnours.get(n)[0] && wumpus_coor[1]== nb.neighnours.get(n)[1])
                    {
                        if(w.hasArrow()){
                            shoot = true;
                            nodanger = true;
                        }
                        else{
                            if(probability.size()>1){
                                probability.remove(probability.get(n));
                                min_pit=1;
                            }
                            else nodanger = true;

                        }
                    }
                    else nodanger=true;

                }
                else{
                    nodanger=true;
                }
            }

            index = n;

        }

        position[0] = nb.neighnours.get(index)[0];
        position[1] = nb.neighnours.get(index)[1];

        return shoot;

    }


    public double measure_distance(int newX, int newY){
        int currentX = w.getPlayerX();
        int currentY = w.getPlayerY();
        double distance = Math.abs(currentX-newX)+Math.abs(currentY-newY);
        return distance;
    }

    
    public void move_to_target(int x,int y)
    {
        boolean is_adja = false;
        if(measure_distance(target_x,target_y)==1){
            is_adja = true;
        }
        int dir = w.getDirection();

            if(x<target_x){
                if(!w.isUnknown(x+1,y)&&!w.hasPit(x+1,y)&&!(x+1== coordinate[0])||is_adja){
                    change_way(dir,World.DIR_RIGHT);
                    return;
                }
            }

            if(x>target_x){
                if(!w.isUnknown(x-1,y)&&!w.hasPit(x-1,y)&&!(x-1== coordinate[0])||is_adja){
                    change_way(dir,World.DIR_LEFT);
                    return;
                }
            }

            if(y<target_y){
                if(!w.isUnknown(x,y+1)&&!w.hasPit(x,y+1)&&!(y+1== coordinate[1])||is_adja){
                    change_way(dir,World.DIR_UP);
                    return;
                }
            }

            if(y>target_y){
                if(!w.isUnknown(x,y-1)&&!w.hasPit(x,y-1)&&!(y-1== coordinate[1])||is_adja){
                    change_way(dir,World.DIR_DOWN);
                    return;
                }
            }


        int pitdir = -1;

        if(w.isVisited(x+1,y) && w.hasPit(x+1,y)){
            pitdir=w.DIR_RIGHT;
        }
        if(w.isVisited(x-1,y) && w.hasPit(x-1,y)){
            pitdir=w.DIR_LEFT;
        }
        if(w.isVisited(x,y+1) && w.hasPit(x,y+1)){
            pitdir=w.DIR_UP;
        }
        if(w.isVisited(x,y-1) && w.hasPit(x,y-1)){
            pitdir=w.DIR_DOWN;
        }
        else if(w.isVisited(x,y-1) && !w.hasPit(x,y-1)){
            pitdir = -1;
        }


        if( pitdir >= 0){
            coordinate[0]=x;
            coordinate[1]=y;
            change_way(dir,pitdir);
            return;
        }

        check_new_goal(x,y, false);


        move_to_target(w.getPlayerX(),w.getPlayerY());
    }

    public void check_new_goal(int x,int y,boolean has_new_target){
        if(x != target_x){
            for(int i=1; i<=w.getSize(); i++){
                boolean has_way = true;
                if(x>target_x){
                    for(int j=x; j>=target_x; j--){
                        if(w.isUnknown(j,i)){
                            has_way = false;
                            break;
                        }
                    }
                }

                else{
                    for(int k=x; k<=target_x; k++){
                        if(w.isUnknown(k,i)){
                            has_way = false;
                            break;
                        }
                    }
                }

                if(has_way){
                    target_y = i;
                    has_new_target = true;
                    break;
                }
            }
        }

        else if(y != target_y){
            boolean has_way = true;
            for(int xx=1; xx<=w.getSize(); xx++){
                if(y>target_y){
                    for(int yy=y; yy>=target_y; yy--){
                        if(w.isUnknown(xx,yy)){
                            has_way = false;
                            break;
                        }
                    }
                }

                else{
                    for(int yy=y; yy<=target_y; y++){
                        if(w.isUnknown(xx,yy)){
                            has_way = false;
                            break;
                        }
                    }
                }

                if(has_way){
                    target_x = xx;
                    has_new_target = true;
                    break;
                }
            }
        }
        else System.out.println("");
        if(!has_new_target){
            change_target();
        }
    }


    private void change_way(int currentDir, int targetDir)
    {
        int dir = currentDir-targetDir;

        if(w.isInPit()){
            w.doAction(World.A_CLIMB);
        }
        if(dir==0){

            if(gun_shoot&&measure_distance(arrow_target[0],arrow_target[1])==1){
                w.doAction(World.A_SHOOT);
                gun_shoot=false;
                if(!w.wumpusAlive()){
                    wumpus_sta = DEAD;
                }
            }
            else {
                w.doAction(World.A_MOVE);
            }
        }
        else if(dir == -1 ||dir == 3) {
            w.doAction(World.A_TURN_RIGHT);
        }
        else  {
            w.doAction(World.A_TURN_LEFT);
        }

    }


    private  void change_target(){
        if(!w.isUnknown(target_x+1,target_y) && w.isValidPosition(target_x+1,target_y)){
            target_x += 1;
        }

        if(!w.isUnknown(target_x-1,target_y) && w.isValidPosition(target_x-1,target_y)){
           target_x -= 1;
        }

        if(!w.isUnknown(target_x,target_y+1) && w.isValidPosition(target_x,target_y+1)){ ;
          target_y += 1;
        }

        if(!w.isUnknown(target_x,target_y-1) && w.isValidPosition(target_x,target_y-1)){
           target_y -= 1;
        }
    }




}

