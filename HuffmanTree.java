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

import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;
import java.util.TreeMap;

public class HuffmanTree {

    private TreeNode root;
    private int size;

    public HuffmanTree(TreeMap<Integer, Integer> table){ // generate tree given frequency table
        HuffmanQueue queue = new HuffmanQueue(table);
        while(queue.size() > 1) {
            TreeNode leftChild = queue.poll();
            TreeNode rightChild = queue.poll();
            queue.push(new TreeNode(leftChild, leftChild.getFrequency() + rightChild.getFrequency(), rightChild));
        }
        this.root = queue.poll();
        this.size = queue.treeSize();
    }

    public HuffmanTree(BitInputStream inputStream) throws IOException { // given IO stream in decompress, generate tree
        int bitsToRead = inputStream.readBits(IHuffConstants.BITS_PER_INT);
        int bitsRead = 0;

        Stack<TreeNode> stack = new Stack<>();
        this.root = new TreeNode(-1, -1);
        stack.push(this.root);

        while(!stack.isEmpty() && bitsRead < bitsToRead){
            TreeNode node = stack.peek();
            TreeNode child = new TreeNode(-1, -1);
            bitsRead++;
            if(node.getLeft() != null)
                stack.pop();
            if(inputStream.readBits(1) == 0)
                stack.push(child);
            else{
                child = new TreeNode(inputStream.readBits(IHuffConstants.BITS_PER_WORD + 1), -1);
                bitsRead += IHuffConstants.BITS_PER_WORD + 1;
            }
            if (node.getLeft() == null)
                node.setLeft(child);
            else
                node.setRight(child);
        }
        this.root = this.root.getLeft();
        if(bitsRead != bitsToRead)
            throw new IOException("Incorrect Header Format");
    }

    public HashMap<Integer, Integer> getMap(){
        HashMap<Integer, Integer> map = new HashMap<>();
        fillMap(map);
        return map;
    }

    private void fillMap(HashMap<Integer, Integer> map){ // pre oder fill of map
        Stack<Pairs> stack = new Stack<>();
        stack.push(new Pairs(this.root, 1));
        while(!stack.isEmpty()){
            Pairs current = stack.pop();
            if(current.node.isLeaf())
                map.put(current.node.getValue(), current.value);
            else{
                if(current.node.getLeft() != null)
                    stack.push(new Pairs(current.node.getLeft(), current.value << 1));
                if(current.node.getRight() != null)
                    stack.push(new Pairs(current.node.getRight(), (current.value << 1) + 1));
            }
        }
    }

    private static class Pairs{ // private class for help with fillMap
        TreeNode node;
        Integer value;
        public Pairs(TreeNode node, Integer value){
            this.node = node;
            this.value = value;
        }
    }

    public int size(){
        return this.size;
    }

    public TreeNode getRoot(){
        return this.root;
    }

}
