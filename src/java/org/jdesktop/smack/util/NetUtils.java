/* $Id$
 *
 * Common util.
 *
 * Released under Gnu Public License
 * Copyright © 2015 Michael G. Binz
 */
package org.jdesktop.smack.util;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Net related utility classes.
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class NetUtils
{
    /**
     * Get the name of the local host.
     *
     * @return The name of the local host.
     */
    public static String getLocalHostName()
    {
        try
        {
            return InetAddress.getLocalHost().getHostName();
        }
        catch ( IOException e )
        {
            return getLocalHostAddress();
        }
    }

    /**
     * Get the name of the local host.
     *
     * @return The name of the local host.
     */
    public static String getLocalHostAddress()
    {
        try
        {
            return InetAddress.getLocalHost().getHostAddress();
        }
        catch ( IOException ee )
        {
            return "127.0.0.1";
        }
    }

    /**
     * Creates a socket based on the normal hostname:port notation.
     *
     * @param hostPort The socket address based on hostname:port notation.
     * @return A newly allocated socket.
     * @throws MalformedURLException In case the syntax of hostname:port is wrong.
     * @throws IOException If the port could not be opened.
     */
    public static Socket createSocket( String hostPort ) throws IOException
    {
        String[] args = hostPort.split( ":" );
        if ( args.length != 2 )
            throw new MalformedURLException( hostPort );

        args[0] = args[0].trim();

        if ( args[0].isEmpty() )
            throw new MalformedURLException( hostPort );

        try
        {
            return new Socket( args[0], Integer.parseInt( args[1] ) );
        }
        catch ( NumberFormatException e )
        {
            throw new MalformedURLException( hostPort );
        }
    }

    /**
     * Read a byte with a timeout from a socket.
     *
     * @param socket The socket to read from.
     * @param timeoutMs The timeout in non-zero microseconds.
     * @return null if no byte could be read in the timeout, otherwise a byte.
     * @throws IOException If socket communication failed.
     */
    public static Byte readSocket( Socket socket, int timeoutMs )
        throws IOException
    {
        int originalTimeout = socket.getSoTimeout();

        try
        {
            socket.setSoTimeout( timeoutMs );

            int result = socket.getInputStream().read();

            socket.setSoTimeout( originalTimeout );
            return Byte.valueOf( (byte)result );
        }
        catch ( SocketTimeoutException e )
        {
            socket.setSoTimeout( originalTimeout );
            return null;
        }
    }
}
