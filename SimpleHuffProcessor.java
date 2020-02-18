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

import java.io.*;
import java.util.*;

public class SimpleHuffProcessor implements IHuffProcessor {

    private IHuffViewer myViewer;
    private int headerFormat;
    private TreeMap<Integer, Integer> fTable; // frequency table/map
    private HuffmanTree hTree;
    private HashMap<Integer, Integer> hMap;
    private int bitsSaved;

    /**
     * Preprocess data so that compression is possible ---
     * count characters/create tree/store state so that
     * a subsequent call to compress will work. The InputStream
     * is <em>not</em> a BitInputStream, so wrap it int one as needed.
     * @param in is the stream which could be subsequently compressed
     * @param headerFormat a constant from IHuffProcessor that determines what kind of
     * header to use, standard count format, standard tree format, or
     * possibly some format added in the future.
     * @return number of bits saved by compression or some other measure
     * Note, to determine the number of
     * bits saved, the number of bits written includes
     * ALL bits that will be written including the
     * magic number, the header format number, the header to
     * reproduce the tree, AND the actual data.
     * @throws IOException if an error occurs while reading from the input file.
     */
    public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
        this.headerFormat = headerFormat;
        fTable = new TreeMap<>();
        int bitCounter = 0;
        int data;
        BitInputStream inputStream = new BitInputStream(in);
        while((data = inputStream.readBits(BITS_PER_WORD)) != -1){ // read in data
            Integer temp = fTable.get(data); // fill frequency table
            fTable.put(data, temp == null ? 1 : (temp + 1));
            bitCounter += BITS_PER_WORD;
        }

        this.hTree = new HuffmanTree(fTable); // create data structures
        this.hMap = hTree.getMap();
        int bits = bitsWritten(fTable);
        bitsSaved = bitCounter - bits;
        return bitsSaved;
    }

    private int bitsWritten(TreeMap<Integer, Integer> fTable){
        int bits = 0;
        bits += 2 * BITS_PER_INT; // magic number and tree header

        if(this.headerFormat == STORE_COUNTS) // for SCF
            bits += ALPH_SIZE * BITS_PER_INT;
        else if(headerFormat == STORE_TREE){
            bits += BITS_PER_INT;
            bits += hTree.size() + hMap.size() * (BITS_PER_WORD + 1); // + 1 for the EOF
        } else{
            showString("Error: Unknown Header Format");
            return -1;
        }
        for(int key : hMap.keySet()) {
            if (key != PSEUDO_EOF)
                bits += (int) (Math.log(hMap.get(key)) / Math.log(2)) * fTable.get(key); // Log base 2 used to determine bit length
            else
                bits += (int) (Math.log(hMap.get(key)) / Math.log(2));
        }
        return bits;
    }

    /**
     * Compresses input to output, where the same InputStream has
     * previously been pre-processed via <code>preprocessCompress</code>
     * storing state used by this call.
     * <br> pre: <code>preprocessCompress</code> must be called before this method
     * @param in is the stream being compressed (NOT a BitInputStream)
     * @param out is bound to a file/stream to which bits are written
     * for the compressed file (not a BitOutputStream)
     * @param force if this is true create the output file even if it is larger than the input file.
     * If this is false do not create the output file if it is larger than the input file.
     * @return the number of bits written.
     * @throws IOException if an error occurs while reading from the input file or
     * writing to the output file.
     */
    public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
        if(!force && bitsSaved <= 0){ // if no force compression and compressed file is larger, terminate
            showString("Compressed file is larger than original file: select \"Force Compression\" under \"Options\" to continue.");
            return -1;
        }

        BitInputStream inputStream = new BitInputStream(new BufferedInputStream(in)); // wrapped in buffered streams to increase efficiency
        BitOutputStream outputStream = new BitOutputStream((new BufferedOutputStream(out)));

        int bits = headerHelper(headerFormat, outputStream);
        if(bits == -1) // if header is invalid, terminate
            return -1;
        bits += fileHelper(inputStream, outputStream);

        inputStream.close(); // close streams
        outputStream.close();

        return bits;
    }

    private int headerHelper(int headerFormat, BitOutputStream outputStream){
        int bits = 2 * BITS_PER_INT; // for header and magic number
        outputStream.writeBits(BITS_PER_INT, MAGIC_NUMBER);
        outputStream.writeBits(BITS_PER_INT, headerFormat);

        if(headerFormat == STORE_COUNTS){ // for SCF
            bits += countFormatHelper(outputStream);
            return bits;
        } else if(headerFormat == STORE_TREE){ // for STF
            outputStream.writeBits(BITS_PER_INT, hTree.size() + (hMap.size() * (BITS_PER_WORD + 1)));
            bits += treeFormatHelper(outputStream);
            bits += BITS_PER_INT;
            return bits;
        } else{
            showString("Error: Unknown Header Format");
        }
        return -1;
    }

    private int countFormatHelper(BitOutputStream outputStream){
        for(int i = 0; i < ALPH_SIZE; ++i)
            outputStream.writeBits(BITS_PER_INT, Objects.requireNonNullElse(fTable.get(i), 0)); // if not null, write it out, otherwise 0
        return ALPH_SIZE * BITS_PER_INT;
    }

    private int treeFormatHelper(BitOutputStream outputStream){
        int bits = BITS_PER_INT;
        Stack<TreeNode> stack = new Stack<>();

        stack.push(this.hTree.getRoot());
        while(!stack.isEmpty()){ // pre order traversal
            TreeNode current = stack.pop();
            if(!current.isLeaf()){ // if not leaf, write 0
                bits++;
                outputStream.writeBits(1, 0);
                if(current.getRight() != null)
                    stack.push(current.getRight());
                if(current.getLeft() != null)
                    stack.push(current.getLeft());
            } else{ // if leaf, 1
                outputStream.writeBits(1, 1);
                outputStream.writeBits(BITS_PER_WORD + 1, current.getValue()); //EOF
                bits += BITS_PER_WORD + 2;
            }
        }
        return bits;
    }

    private int fileHelper(BitInputStream inputStream, BitOutputStream outputStream) throws IOException {
        int bits = 0;
        int data;
        while((data = inputStream.readBits(BITS_PER_WORD)) != -1)
            bits += outputCompressed(hMap.get(data), outputStream);
        bits += outputCompressed(hMap.get(PSEUDO_EOF), outputStream);
        return bits;
    }

    private int outputCompressed(int data, BitOutputStream outputStream){
        int bitLength = (int)(Math.log(data) / Math.log(2));
        outputStream.writeBits(bitLength, data & ~(1 << (bitLength + 1))); // masks the most significant bit to 0 and ensures the correct number of bits is written
        return bitLength;
    }

    /**
     * Uncompress a previously compressed stream in, writing the
     * uncompressed bits/data to out.
     * @param in is the previously compressed data (not a BitInputStream)
     * @param out is the uncompressed file/stream
     * @return the number of bits written to the uncompressed file/stream
     * @throws IOException if an error occurs while reading from the input file or
     * writing to the output file.
     */
    public int uncompress(InputStream in, OutputStream out) throws IOException {
        BitInputStream inputStream = new BitInputStream(new BufferedInputStream(in));
        BitOutputStream outputStream = new BitOutputStream(new BufferedOutputStream(out));

        if(inputStream.readBits(BITS_PER_INT) != MAGIC_NUMBER){ // Handles error
            showString("Error: Cannot Uncompress File.");
            return -1;
        }

        int headerFormat = inputStream.readBits(BITS_PER_INT); // decompressed header info
        if(headerFormat == STORE_COUNTS){
            TreeMap<Integer, Integer> fTable = new TreeMap<>();
            for(int i = 0; i < ALPH_SIZE; ++i){
                int data = inputStream.readBits(BITS_PER_INT);
                if(data != 0)
                    fTable.put(i, data);
            }
            hTree = new HuffmanTree(fTable);
        } else if(headerFormat == STORE_TREE){
            hTree = new HuffmanTree(inputStream);
        } else
            showString("Invalid Header Format");

        int bits = 0;
        bits += uncompressHelper(inputStream, outputStream);

        inputStream.close(); // closed streams
        outputStream.close();

        return bits;
    }

    private int uncompressHelper(BitInputStream inputStream, BitOutputStream outputStream) throws IOException{
        int bits = 0;
        int data;
        while((data = getValue(inputStream)) != PSEUDO_EOF){ // simple output steam
            outputStream.writeBits(BITS_PER_WORD, data);
            bits += BITS_PER_WORD;
        }
        return bits;
    }

    private int getValue(BitInputStream inputStream) throws IOException { // traverses tree according to path
        TreeNode current = this.hTree.getRoot();
        while(!current.isLeaf()){
            int data = inputStream.readBits(1);
            if(data == 0)
                current = current.getLeft();
            else if(data == 1)
                current = current.getRight();
            else
                throw new IOException("Invalid File Data");
        }
        return current.getValue();
    }

    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    private void showString(String s){
        if(myViewer != null)
            myViewer.update(s);
    }
}
