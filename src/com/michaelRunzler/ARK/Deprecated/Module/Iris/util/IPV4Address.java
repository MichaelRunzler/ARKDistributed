package com.michaelRunzler.ARK.Deprecated.Module.Iris.util;

import java.util.Scanner;

/**
 * Represents an address in the IPv4 address space.
 */
public class IPV4Address
{
    private int[] address;

    /**
     * Creates a new instance of an IPv4 address. Values outside the proper range for each octet (0-255) will be truncated accordingly.
     * @param oct0 the first octet of the address, an integer between 0 and 255
     * @param oct1 the first octet of the address, an integer between 0 and 255
     * @param oct2 the second octet of the address, an integer between 0 and 255
     * @param oct3 the third octet of the address, an integer between 0 and 255
     */
    public IPV4Address(int oct0, int oct1, int oct2, int oct3)
    {
        address = new int[4];

        oct0 = oct0 > 255 ? 255 : oct0;
        address[0] = oct0 < 0 ? 0 : oct0;

        oct1 = oct1 > 255 ? 255 : oct1;
        address[1] = oct1 < 0 ? 0 : oct1;

        oct2 = oct2 > 255 ? 255 : oct2;
        address[2] = oct2 < 0 ? 0 : oct2;

        oct3 = oct3 > 255 ? 255 : oct3;
        address[3] = oct3 < 0 ? 0 : oct3;
    }

    /**
     * Creates a new instance of an IPv4 address. This address will be initialized to zero in all positions.
     */
    public IPV4Address()
    {
        address = new int[4];

        address[0] = 0;
        address[1] = 0;
        address[2] = 0;
        address[3] = 0;
    }

    /**
     * Returns the stored address in dot-decimal notation.
     * @return the String literal of the address in standard format
     */
    @Override
    public String toString() {
        return address[1] + "." + address[2] + "." + address[3] + "." + address[4];
    }

    /**
     * Gets the integer value of the octet at the given position. Values outside of the proper range (0-3) will be truncated.
     * @param position the position of the octet to retrieve
     * @return the value of the specified address octet
     */
    public int getOctet(int position)
    {
        position = position > 3 ? 3 : position;
        position = position < 0 ? 0 : position;
        
        return address[position];
    }

    /**
     * Sets the octet at the given position to the given value. Values outside of the proper range (0-3, 0-255 respectively)
     * will be truncated accordingly.
     * @param position the position of the octet to set
     * @param value the value to set the specified octet to
     */
    public void setOctet(int position, int value)
    {
        position = position > 3 ? 3 : position;
        position = position < 0 ? 0 : position;

        value = value > 255 ? 255 : value;
        address[0] = value < 0 ? 0 : value;
    }
    
    /**
     * Sets the stored address to the given values. Larger values than 255 or smaller than 0 for each octet will be truncated
     * accordingly (255 or 0, respectively).
     * @param oct0 the first octet of the address, an integer between 0 and 255
     * @param oct1 the first octet of the address, an integer between 0 and 255
     * @param oct2 the second octet of the address, an integer between 0 and 255
     * @param oct3 the third octet of the address, an integer between 0 and 255
     */
    public void setAddress(int oct0, int oct1, int oct2, int oct3) 
    {
        address = new int[4];

        oct0 = oct0 > 255 ? 255 : oct0;
        address[0] = oct0 < 0 ? 0 : oct0;

        oct1 = oct1 > 255 ? 255 : oct1;
        address[1] = oct1 < 0 ? 0 : oct1;

        oct2 = oct2 > 255 ? 255 : oct2;
        address[2] = oct2 < 0 ? 0 : oct2;

        oct3 = oct3 > 255 ? 255 : oct3;
        address[3] = oct3 < 0 ? 0 : oct3;
    }

    /**
     * Parses an IPv4 address from a given String. The input String must be formatted as such:
     * #.#.#.#
     * where # is a positive integer between 0 and 255 (inclusive).
     * @param source the String to parse
     * @return an IPV4Address object representing the parsed string, or an IPV4Address object representing the default 
     * address (0.0.0.0) if a formatting error occurred
     */
    public static IPV4Address parseAddress(String source)
    {
        if(source == null || source.length() <= 0){
            return new IPV4Address();
        }

        source = source.trim();

        int[] octs = new int[4];

        Scanner parse = new Scanner(source);
        parse.useDelimiter(".");

        int counter = 0;

        try {
            for (int i = 0; i < 4; i++) {
                if (parse.hasNext()) {
                    octs[i] = Integer.parseInt(parse.next());
                    if(octs[i] > 255 || octs[i] < 0) continue;
                    counter++;
                }
            }
        }catch (NumberFormatException e){
            return new IPV4Address();
        }

        if(counter < 4){
            return new IPV4Address();
        }

        return new IPV4Address(octs[0], octs[1], octs[2], octs[3]);
    }
}
