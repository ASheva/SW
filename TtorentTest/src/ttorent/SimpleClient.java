package ttorent;

/**
 * Created with IntelliJ IDEA.
 * User: Shevchik
 * Date: 21.12.13
 * Time: 8:01
 * To change this template use File | Settings | File Templates.
 */
import jargs.gnu.CmdLineParser;
import java.io.File;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.BasicConfigurator;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.Client.ClientState;
import com.turn.ttorrent.client.SharedTorrent;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.PatternLayout;

public class SimpleClient
{
    //public static final String DEFAULT_TRACKER_URI = "http://localhost:6969/announce";

    private static void usage(PrintStream s)
    {
        s.println("usage: SimpleClient [options] torrent");
        s.println("Leech and seed this torrent file.");
        s.println();
        s.println("Available options:");
        s.println("  -h,--help                  Show this help and exit.");
        s.println("  -o,--output DIR            Read/write data to directory DIR.");
        s.println();
    }

    public static void main(String[] args)
    {
        BasicConfigurator.configure(new ConsoleAppender(
                new PatternLayout("%d [%-25t] %-5p: %m%n")));

        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option help = parser.addBooleanOption('h', "help");
        CmdLineParser.Option outputString = parser.addStringOption('o', "output");

        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException oe) {
            System.err.println(oe.getMessage());
            usage(System.err);
            System.exit(1);
        }

        // Display help and exit if requested
        if (Boolean.TRUE.equals((Boolean)parser.getOptionValue(help))) {
            usage(System.out);
            System.exit(0);
        }

        // Get options
        File output = new File((String) parser.getOptionValue(outputString, "."));

        // Check that it's the correct usage
        String[] otherArgs = parser.getRemainingArgs();
        if (otherArgs.length != 1) {
            usage(System.err);
            System.exit(1);
        }

        // Get the .torrent file path
        File torrentPath = new File(otherArgs[0]);

        // Start downloading file
        try {
            SharedTorrent torrent = SharedTorrent.fromFile(torrentPath, output);
            System.out.println("Starting client for torrent: "+torrent.getName());
            Client client = new Client(InetAddress.getLocalHost(), torrent);

            try {
                System.out.println("Start to download: "+torrent.getName());
                client.share();
                // client.download()    // DONE for completion signal

                while (!ClientState.SEEDING.equals(client.getState())) {
                    // Check if there's an error
                    if (ClientState.ERROR.equals(client.getState())) {
                        throw new Exception("ttorrent client Error State");
                    }

                    // Display statistics
                    System.out.printf("%f %% - %d bytes downloaded - %d bytes uploaded\n", torrent.getCompletion(), torrent.getDownloaded(), torrent.getUploaded());

                    // Wait one second
                    TimeUnit.SECONDS.sleep(1);
                }

                System.out.println("download completed.");
            } catch (Exception e) {
                System.err.println("An error occurs...");
                e.printStackTrace(System.err);
            } finally {
                System.out.println("stop client.");
                client.stop();
            }
        } catch (Exception e) {
            System.err.println("An error occurs...");
            e.printStackTrace(System.err);
        }
    }
}