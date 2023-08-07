package ai;

import ai.Global;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import kalaha.*;

public class AIClient implements Runnable
{
    private int player;
    private JTextArea text;
    
    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;

    // Initailizing Variables
    private static final int initdep = 0;
    private static final int Timlim = 4999;
    private int currdep;
    private int move;
    private int globmove;
    private long sTim;
    private boolean timo;
    public int maxplayer = 1;
    public int minplayer = 2;
    	
    public AIClient()
    {
	player = -1;
        connected = false;
        
        initGUI();
	
        try
        {
            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        }
        catch (Exception ex)
        {
            addText("Unable to connect to server");
            return;
        }
    }
    
    public void start()
    {
    
        if (connected)
        {
            thr = new Thread(this);
            thr.start();
        }
    }
    
    private void initGUI()
    {
    
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420,250));
        frame.getContentPane().setLayout(new FlowLayout());
        
        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));
        
        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setVisible(true);
    }
    
    public void addText(String txt)
    {
      
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }
    
    public void run()
    {
        String reply;
        running = true;
        
        try
        {
            while (running)
            {
                if (player == -1)
                {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);
                    
                    addText("I am player " + player);
                }
                

                out.println(Commands.WINNER);
                reply = in.readLine();
                if(reply.equals("1") || reply.equals("2") )
                {
                    int w = Integer.parseInt(reply);
                    if (w == player)
                    {
                        addText("I won!");
                    }
                    else
                    {
                        addText("I lost...");
                    }
                    running = false;
                }
                if(reply.equals("0"))
                {
                    addText("Even game!");
                    running = false;
                }

                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running)
                {
                    int nextPlayer = Integer.parseInt(reply);

                    if(nextPlayer == player)
                    {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove)
                        {
                            long startT = System.currentTimeMillis();
                            GameState currentBoard = new GameState(currentBoardStr);
                            int cMove = getMove(currentBoard);
                            
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double)tot / (double)1000;
                            
                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR"))
                            {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }
                        }
                    }
                }
                
        
                Thread.sleep(100);
            }
	}
        catch (Exception ex)
        {
            running = false;
        }
        
        try
        {
            socket.close();
            addText("Disconnected from server");
        }
        catch (Exception ex)
        {
            addText("Error closing connection: " + ex.getMessage());
        }
    }
    

    public int getMove(GameState currentboard)
    {
        timo = false;
        sTim= System.currentTimeMillis();
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int play;
        if(currentboard.getNextPlayer()!=1)
        {
            play = minplayer;
        }
        else 
        {
            play = maxplayer;
        }


        int k = 0;
        while(true)
        {
            if(k>0)
                globmove=move;
            currdep= initdep+k;
            maxi(currentboard,currdep,alpha,beta,player);
            if(timo){
                return globmove;
            }
            k++;
        }
    }

    public int getscore(GameState kalaha,int player)
    {
        int score;
        int nextplayer;
        if(player == 1) 
        {
            nextplayer = 2;
            score = kalaha.getScore(player) - kalaha.getScore(nextplayer);
        }
        else 
        {
            nextplayer = 1;
            score = kalaha.getScore(nextplayer) - kalaha.getScore(player);
        }

        return score;
    }

    // Maximizer
    public int maxi(GameState kalaha,int depth,int alpha,int beta,int player)
    {
        int nplayer;
        if(player != 1)
            nplayer = 1;
        else
            nplayer = 2;

        if(System.currentTimeMillis()-sTim> Timlim)
        {
            timo=true;
            return alpha;
        }
        if(kalaha.gameEnded()||depth==0)
        {
            return  getscore(kalaha,player);
        }
        int i = 1;
        while(i<7)
        {
            GameState kalahaboard = kalaha.clone();
            if(kalahaboard.moveIsPossible(i)){
                kalahaboard.makeMove(i);
                int val = mini(kalahaboard,depth-1,alpha,beta,nplayer);
                if(val>alpha)
                {
                    alpha=val;
                    if(depth == currdep)
                    {
                        move = i;
                    }
                }
                if(alpha >= beta)
                    return alpha;
            }
            i++;
        }
        return alpha;
    }
    // Minimizer
    public int mini(GameState kalaha,int depth,int alpha,int beta,int player)
    {
        int nextplayer;
        if(player==1)
            nextplayer = 2;
        else
            nextplayer = 1;


        if(depth==0||kalaha.gameEnded())
        {
            return getscore(kalaha,player);
        }
        for(int i = 1; i < 7;i++)
        {
            GameState kalahaBoard = kalaha.clone();
            if(kalahaBoard.moveIsPossible(i))
            {
                kalahaBoard.makeMove(i);
                int val = maxi(kalahaBoard,depth-1,alpha,beta, nextplayer);
                if(val<=beta)
                {
                    beta = val;
                }
                if(alpha>=beta)
                {
                    return beta;
                }
            }
        }
        return beta;
    }
}