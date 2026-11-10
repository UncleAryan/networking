import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CRC16Bits {
    // the generator x^16 + x^12 + x^5 + 1 with the top x^16 bit left out
    private static final int POLY = 0x1021;

    // main method
    public static void main(String[] args) {
        // stop the program if no file name was given on the command line
        if (args.length == 0) {
            System.out.println("There is no file passed in!");
            return;
        }
        // the test file name is the first command line argument
        Path file = Paths.get(args[0]);
        // this list will hold every line of the test file
        List<String> lines;

        // try to read the file and catch any problem with reading it
        try {
            // read every line of the test file
            // because it's a txt file, I am using ASCII so every char is 1 byte
            lines = Files.readAllLines(file, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            // tell user the file could not be read and stop the program
            System.out.println("could not read the file: " + e.getMessage());
            return;
        }

        // the output file all results are written to
        String outName = "crc_output.txt";
        // this writer sends the results into the output file
        PrintWriter out;

        // try to open the output file and catch any problem with creating it
        try {
            out = new PrintWriter(outName, StandardCharsets.US_ASCII);
        } catch (IOException e) {
            // tell user the output file could not be made and stop the program
            System.out.println("could not create the output file: " + e.getMessage());
            return;
        }

        // this array holds the code word made for each line
        byte[][] codeWords = new byte[lines.size()][];

        // print the heading for the crc creation part
        out.println("===== CRC Creation =====");
        // build the crc for each line on its own
        for (int n = 0; n < lines.size(); n++) {
            // turn the line into its data word bytes
            byte[] dataWord = lines.get(n).getBytes(StandardCharsets.US_ASCII);
            // compute the crc of the data word
            int crc = crc16(dataWord);
            // make the code word two bytes longer than the data word to hold the crc
            byte[] codeWord = new byte[dataWord.length + 2];
            // copy the data word into the front of the code word
            System.arraycopy(dataWord, 0, codeWord, 0, dataWord.length);
            // put the high byte of the crc after the data
            codeWord[dataWord.length] = (byte) (crc >> 8);
            // put the low byte of the crc at the very end
            codeWord[dataWord.length + 1] = (byte) crc;
            // save the code word so it can be tested later
            codeWords[n] = codeWord;
            // print the line, its data in hex, its crc and the code word
            out.println("line " + (n + 1) + ": " + lines.get(n));
            out.println("  data (hex): " + toHex(dataWord));
            out.println("  crc  (hex): " + String.format("%04X", crc));
            out.println("  code word : " + toHex(codeWord));
            out.println();
        }

        out.println("===== Code Word Testing =====");
        // test each code word on its own
        for (int n = 0; n < codeWords.length; n++) {
            // divide the whole code word by the generator
            int remainder = crc16(codeWords[n]);
            // zero remainder means the code word has no error
            String result = remainder == 0 ? "no error detected" : "error detected";
            // print the code word, the remainder and the result
            out.println("line " + (n + 1) + " code word: " + toHex(codeWords[n]));
            out.println("  remainder (hex): " + String.format("%04X", remainder) + "  -> " + result);
            out.println();
        }

        // close the file so everything is saved
        out.close();
        // tell user where the results were written
        System.out.println("results written to " + outName);
    }

    // divides data by generator and returns 16-bit remainder
    static int crc16(byte[] data) {
        // register starts empty for plain polynomial division
        int crc = 0x0000;

        for (byte b : data) {
            // xor byte into top 8 bits of register
            crc ^= (b & 0xFF) << 8;

            for (int i = 0; i < 8; i++) {
                // check if top bit is one
                if ((crc & 0x8000) != 0) {
                    // shift left and subtract the generator
                    crc = (crc << 1) ^ POLY;
                } else {
                    // shift left only because the top bit is zero
                    crc = crc << 1;
                }

                // keep register at sixteen bits
                crc &= 0xFFFF;
            }
        }

        // return the remainder which is the crc
        return crc;
    }

    // this method turns an array of bytes into hex text separated by spaces
    // this is the only method I used AI to generate
    static String toHex(byte[] data) {
        // a builder is used to join the hex values together
        StringBuilder sb = new StringBuilder();
        // add each byte as two hex digits
        for (byte b : data) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        // remove the last space and return the text
        return sb.toString().trim();
    }
}
