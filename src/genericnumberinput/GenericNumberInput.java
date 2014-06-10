package genericnumberinput;

import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Scanner;

/**
 *
 * @author Jason LaFrance
 */
public class GenericNumberInput {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Scanner kbd = new Scanner(System.in);

        // reference the System.out as a PrintSream so we can use
        // dependency injection for the getNumber method
        PrintStream console = System.out;

        // Change this type to another Number object type!
        //Long i = 0L;
        BigDecimal i = BigDecimal.ZERO;

        try {
            i = getNumber("Enter a " + i.getClass().getSimpleName() + ": ",
                    kbd,
                    console,
                    i.getClass());
        } catch (NoSuchFieldException |
                IllegalArgumentException |
                IllegalAccessException |
                NoSuchMethodException |
                InstantiationException |
                InvocationTargetException ex) {
            console.println("Bad number type!\t" + ex);
            return;
        }

        System.out.println("Got " + i.getClass().getSimpleName() + " " + i);

    }

    // Generic InputStream number parser with range validation...
    // only allow types that extend Number!
    // Also, dependency injected so that nothing in the method is 
    // system specific.  For example, the output PrintStream could actually
    // feed to a GUI text field instead of the system console.  getNumber
    // doesn't care where it goes as long as it's going into a PrintStream.
    // Same with the input Scanner object.  The input could easily be coming
    // in from a file.
    public static <T extends Number> T getNumber(
            String msg,
            Scanner input,
            PrintStream output,
            Class<T> clazz
    ) throws NoSuchFieldException,
            IllegalArgumentException,
            IllegalAccessException,
            NoSuchMethodException,
            InstantiationException,
            InvocationTargetException {

        // get the min and max values via reflection
        // Number subclasses that are missing these fields will throw exceptions!
        // BigInteger and BigDecimal don't need overflow validation anyways!
        BigDecimal min, max;
        if (clazz == BigInteger.class || clazz == BigDecimal.class) {
            // since BigInteger and BigDecimal are unbound, set these to null
            min = null;
            max = null;
        } else {
            // all other number types have a minimum and maximum
            max = BigDecimal.valueOf(
                    Double.parseDouble(
                            clazz.getField("MAX_VALUE").get(null).toString()
                    )
            );

            min = BigDecimal.valueOf(
                    Double.parseDouble(
                            clazz.getField("MIN_VALUE").get(null).toString()
                    )
            );

            // adjust for floating point non-signed min/max
            if (min.compareTo(BigDecimal.ZERO) >= 0) {
                //  min is greater than 0, so min is really -max
                min = max.negate();
            }

            // show the min/max for debugging...
            output.println("min: " + min.toPlainString()
                    + "\tmax: " + max.toPlainString());

        }

        // make sure that 'in' isn't null...
        // 'in' is a BigDecimal since it can handle all of the other types
        BigDecimal in = BigDecimal.ZERO;
        boolean ok;
        do {
            System.out.print(msg);
            try {
                in = input.nextBigDecimal();
                ok = true;
            } catch (Exception e) {
                // clear the scanner item
                input.next();
                ok = false;
            }

            // check bounds if not a BigInteger or BigDecimal
            if (clazz != BigInteger.class && clazz != BigDecimal.class) {
                if (in.compareTo(min) < 0 || in.compareTo(max) > 0) {
                    ok = false;
                }
            }
        } while (!ok);

        // create a new object of type T using a String constructor, if it has
        // one, otherwise this method will throw an exception.  All of the primary
        // Number types should work, though.
        T ret = null;
        try {
            ret = clazz.getConstructor(String.class).newInstance(in.toPlainString());
        } catch (InvocationTargetException | NumberFormatException ex) {
            // must be a decimal point going into an integer type...
            // get a copy of a string version of the number
            String num = in.toPlainString();
            // get the index of the decimal point
            int theDecimal = num.indexOf('.');
            // if indexOf returned -1 then no decimal point was found
            if (theDecimal == -1) {
                // let the calling process know that we had trouble parsing...
                throw new NumberFormatException();
            }
            // otherwise, chop off the decimal point and everything after it...
            num = num.substring(0, theDecimal);
            // and create the new Number object now
            ret = clazz.getConstructor(String.class).newInstance(num);
        }
        return ret;
    }

}
