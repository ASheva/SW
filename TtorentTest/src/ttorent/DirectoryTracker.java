package ttorent;

/**
 * Created with IntelliJ IDEA.
 * User: Shevchik
 * Date: 21.12.13
 * Time: 9:40
 * To change this template use File | Settings | File Templates.
 */
import jargs.gnu.CmdLineParser;

import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.BasicConfigurator;
import com.turn.ttorrent.client.Client;
import com.turn.ttorrent.client.SharedTorrent;
import com.turn.ttorrent.common.Torrent;
import com.turn.ttorrent.tracker.TrackedTorrent;
import com.turn.ttorrent.tracker.Tracker;

public class DirectoryTracker
{
    public static final int DEFAULT_TRACKER_PORT = 6969;

    private static void usage(PrintStream s, String msg)
    {
        if (msg != null) {
            s.println(msg);
            s.println();
        }

        s.println("usage: DirectoryTracker [options] [directory]");
        s.println("Create a tracker for each files in directory.");
        s.println("Note: .torrent files are created or regenerated.");
        s.println();
        s.println("Available options:");
        s.println(" -h,--help                     Show this help and exit.");
        s.println(" -p,--port PORT                Bind to port PORT.");
        s.println(" -t,--torrent FILE             Use FILE to read/write torrent file.");
        s.println();
    }

    public static void main(String[] args)
    {
        BasicConfigurator.configure();

        CmdLineParser parser = new CmdLineParser();
        CmdLineParser.Option help = parser.addBooleanOption('h', "help");
        CmdLineParser.Option port = parser.addIntegerOption('p', "port");
        CmdLineParser.Option filename = parser.addStringOption('t', "torrent");


        try {
            parser.parse(args);
        } catch (CmdLineParser.OptionException oe) {
            System.err.println(oe.getMessage());
            usage(System.err, "Torrent file must be provided!");
            System.exit(1);
        }

        // Display help and exit if requested
        if (Boolean.TRUE.equals((Boolean)parser.getOptionValue(help))) {
            usage(System.out, "Torrent file must be provided!");
            System.exit(0);
        }

        String filenameValue = (String)parser.getOptionValue(filename);
        if (filenameValue == null) {
            usage(System.err, "Torrent file must be provided!");
            System.exit(1);
        }

        //Integer portValue = (Integer) parser.getOptionValue(port, Integer.valueOf(DEFAULT_TRACKER_PORT));
        String[] otherArgs = parser.getRemainingArgs();

        if (otherArgs.length > 1) {
            usage(System.err, "Torrent file must be provided!");
            System.exit(1);
        }

        // Get directory from command-line argument or default to current
        // directory
        String directory = otherArgs.length > 0 ? otherArgs[0] : ".";

        // Create the Tracker
        Tracker tracker = null;
        OutputStream fos = null;
        try {
            tracker = new Tracker(InetAddress.getLocalHost());
            tracker.start();
            System.out.println("Tracker started.");

            // Parse files in directory
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    List<String> accepted_ends = Arrays.asList(".avi", ".txt", ".mp3");
                    for (String end : accepted_ends) {
                        if (name.endsWith(end)) {
                            return true;
                        }
                    }

                    return false;
                }
            };

            File parent = new File(directory);
            System.out.println("Analysing directory: " + directory);
            for (File f : parent.listFiles(filter)) {
                try {
                    // Try to generate the .torrent file
                    if (filenameValue != null) {
                        fos = new FileOutputStream(filenameValue);
                    } else {
                        fos = System.out;
                    }
                    //File torrent_file = new File(f.getParentFile(), f.getName() + ".torrent");
                    Torrent torrent = Torrent.create(new File(f.getAbsolutePath()), new URI(tracker.getAnnounceUrl().toString()), "createdByTtorrent");
                    System.out.println("Created torrent "+torrent.getName()+" for file: "+f.getAbsolutePath());
                    torrent.save(fos);

                    // Announce file to tracker
                    TrackedTorrent tt = new TrackedTorrent(torrent);
                    tracker.announce(tt);
                    System.out.println("Torrent "+torrent.getName()+" announced");

                    // Share torrent
                    System.out.println("Sharing "+torrent.getName()+"...");
                    Client seeder = new Client(InetAddress.getLocalHost(), new SharedTorrent(torrent, parent, true));
                    seeder.share();

                } catch (Exception e) {
                    System.err.println("Unable to describe, announce or share file: "+f.toString());
                    e.printStackTrace(System.err);
                }
            }

            // Wait for user signal
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                reader.readLine();
            } finally {
                reader.close();
            }

        } catch (Exception e) {
            System.err.println("Unable to start tracker.");
            e.printStackTrace(System.err);
            System.exit(1);
        } finally {
            if (tracker != null) {
                tracker.stop();
                System.out.println("Tracker stopped.");
            }
        }
    }

}