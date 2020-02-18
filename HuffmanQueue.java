/*  Student information for assignment:
 *
 *  On my honor, Michael Liu, this programming assignment is my own work
 *  and I have not provided this code to any other student.
 *
 *  Number of slip days used: 2
 *
 *  Student 1 Michael Liu
 *  UTEID: MML2924
 *  email address: MichaelLiu@utexas.edu
 *  Grader name: Claire Mathieu
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;

public class HuffmanQueue {

    private ArrayList<TreeNode> queue;
    private int first;

    public HuffmanQueue(TreeMap<Integer, Integer> table){
        queue = new ArrayList<>();
        first = 0;
        mapFill(table);
    }

    private void mapFill(TreeMap<Integer, Integer> table){ // given frequency table
        for(int byt : table.keySet())
            queue.add(new TreeNode(byt, table.get(byt)));
        queue.add(new TreeNode(IHuffConstants.PSEUDO_EOF, 1));
        Collections.sort(queue);
    }

    public void push(TreeNode node){
        int i = 0;
        while(i < queue.size() && queue.get(i).compareTo(node) <= 0)
            i++;
        queue.add(i, node);
    }

    public TreeNode poll(){
        return queue.get(first++);
    }

    public int size(){
        return queue.size() - first;
    }

    public int treeSize(){
        return queue.size();
    }
}
