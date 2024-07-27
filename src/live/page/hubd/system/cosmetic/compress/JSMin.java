/* Project pack:tag >> https://github.com/galan/packtag */
package live.page.hubd.system.cosmetic.compress;

import java.io.*;


/**
 * Pack algorithm for JavaScript
 *
 * @author Daniel Galán y Martins
 * @author John Reilly
 * @author Douglas Crockford
 * @version $Revision : 1.3 $ <p> JSMin.java 2006-02-13 <p> Updated 2007-08-20 with updates from jsmin.c (2007-05-22) (see here http://sourceforge.net/tracker/index.php?func=detail&aid=1865077&group_id=189328&atid=928887 and here http://www.inconspicuous.org/2007/08/21/update-to-jsminjava/) <p> Copyright (c) 2006 John Reilly (www.inconspicuous.org) <p> This work is a translation from C to Java of jsmin.c published by Douglas Crockford.  Permission is hereby granted to use the Java version under the same conditions as the jsmin.c on which it is based. <p> <p> jsmin.c 2003-04-21 <p> Copyright (c) 2002 Douglas Crockford (www.crockford.com) <p> Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: <p> The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software. <p> The Software shall be used for Good, not Evil. <p> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class JSMin {

    /**
     * The constant EOF.
     */
    private static final int EOF = -1;

    /**
     * The In.
     */
    private final PushbackReader in;

    /**
     * The Out.
     */
    private final Writer out;

    /**
     * The The a.
     */
    private int theA;

    /**
     * The The b.
     */
    private int theB;


    /**
     * Instantiates a new Js min.
     *
     * @param in  the in
     * @param out the out
     */
    public JSMin(final Reader in, final Writer out) {
        this.in = new PushbackReader(in);
        this.out = out;
    }

    /**
     * Minify string.
     *
     * @param js the js
     * @return the string
     */
    public static String minify(String js) {
        try (StringReader reader = new StringReader(js); StringWriter str = new StringWriter()) {
            new JSMin(reader, str).jsmin();
            return str.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return js;
    }

    /**
     * isAlphanum -- return true if the character is a letter, digit,
     * underscore, dollar sign, or non-ASCII character.
     *
     * @param c the c
     * @return the boolean
     */
    private static boolean isAlphanum(final int c) {
        return ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$' || c == '\\' || c > 126);
    }


    /**
     * get -- return the next character from stdin. Watch out for lookahead. If
     * the character is a control character, translate it to a space or
     * linefeed.
     *
     * @return the int
     * @throws IOException the io exception
     */
    private int get() throws IOException {
        int c = in.read();

        if (c >= ' ' || c == '\n' || c == EOF) {
            return c;
        }

        if (c == '\r') {
            return '\n';
        }

        return ' ';
    }


    /**
     * Get the next character without getting it.
     *
     * @return the int
     * @throws IOException the io exception
     */
    private int peek() throws IOException {
        int lookaheadChar = in.read();
        in.unread(lookaheadChar);
        return lookaheadChar;
    }


    /**
     * next -- get the next character, excluding comments. peek() is used to see
     * if a '/' is followed by a '/' or '*'.
     *
     * @return the int
     * @throws IOException                  the io exception
     * @throws UnterminatedCommentException the unterminated comment exception
     */
    private int next() throws IOException, UnterminatedCommentException {
        int c = get();
        if (c == '/') {
            switch (peek()) {
                case '/':
                    for (; ; ) {
                        c = get();
                        if (c <= '\n') {
                            return c;
                        }
                    }

                case '*':
                    get();
                    for (; ; ) {
                        switch (get()) {
                            case '*':
                                if (peek() == '/') {
                                    get();
                                    return ' ';
                                }
                                break;
                            case EOF:
                                throw new UnterminatedCommentException();
                        }
                    }

                default:
                    return c;
            }

        }
        return c;
    }


    /**
     * action -- do something! What you do is determined by the argument: 1
     * Output A. Copy B to A. Get the next B. 2 Copy B to A. Get the next B.
     * (Delete A). 3 Get the next B. (Delete B). action treats a string as a
     * single character. Wow! action recognizes a regular expression if it is
     * preceded by ( or , or =.
     *
     * @param d the d
     * @throws IOException                        the io exception
     * @throws UnterminatedRegExpLiteralException the unterminated reg exp literal exception
     * @throws UnterminatedCommentException       the unterminated comment exception
     * @throws UnterminatedStringLiteralException the unterminated string literal exception
     */
    private void action(final int d) throws IOException, UnterminatedRegExpLiteralException, UnterminatedCommentException, UnterminatedStringLiteralException {
        switch (d) {
            case 1:
                out.write(theA);
            case 2:
                theA = theB;

                if (theA == '\'' || theA == '"') {
                    for (; ; ) {
                        out.write(theA);
                        theA = get();
                        if (theA == theB) {
                            break;
                        }
                        if (theA <= '\n') {
                            throw new UnterminatedStringLiteralException();
                        }
                        if (theA == '\\') {
                            out.write(theA);
                            theA = get();
                        }
                    }
                }

            case 3:
                theB = next();
                if (theB == '/'
                        && (theA == '(' || theA == ',' || theA == '=' || theA == ':' || theA == '[' || theA == '!' || theA == '&' || theA == '|' || theA == '?'
                        || theA == '{' || theA == '}' || theA == ';' || theA == '\n')) {
                    out.write(theA);
                    out.write(theB);
                    for (; ; ) {
                        theA = get();
                        if (theA == '/') {
                            break;
                        } else if (theA == '\\') {
                            out.write(theA);
                            theA = get();
                        } else if (theA <= '\n') {
                            throw new UnterminatedRegExpLiteralException();
                        }
                        out.write(theA);
                    }
                    theB = next();
                }
        }
    }


    /**
     * jsmin -- Copy the input to the output, deleting the characters which are
     * insignificant to JavaScript. Comments will be removed. Tabs will be
     * replaced with spaces. Carriage returns will be replaced with linefeeds.
     * Most spaces and linefeeds will be removed.
     *
     * @throws IOException                        the io exception
     * @throws UnterminatedRegExpLiteralException the unterminated reg exp literal exception
     * @throws UnterminatedCommentException       the unterminated comment exception
     * @throws UnterminatedStringLiteralException the unterminated string literal exception
     */
    public void jsmin() throws IOException, UnterminatedRegExpLiteralException, UnterminatedCommentException, UnterminatedStringLiteralException {
        theA = '\n';
        action(3);
        while (theA != EOF) {
            switch (theA) {
                case ' ':
                    if (isAlphanum(theB)) {
                        action(1);
                    } else {
                        action(2);
                    }
                    break;
                case '\n':
                    switch (theB) {
                        case '{':
                        case '[':
                        case '(':
                        case '+':
                        case '-':
                            action(1);
                            break;
                        case ' ':
                            action(3);
                            break;
                        default:
                            if (isAlphanum(theB)) {
                                action(1);
                            } else {
                                action(2);
                            }
                    }
                    break;
                default:
                    switch (theB) {
                        case ' ':
                            if (isAlphanum(theA)) {
                                action(1);
                                break;
                            }
                            action(3);
                            break;
                        case '\n':
                            switch (theA) {
                                case '}':
                                case ']':
                                case ')':
                                case '+':
                                case '-':
                                case '"':
                                case '\'':
                                    action(1);
                                    break;
                                default:
                                    if (isAlphanum(theA)) {
                                        action(1);
                                    } else {
                                        action(3);
                                    }
                            }
                            break;
                        default:
                            action(1);
                            break;
                    }
            }
        }
        out.flush();
    }

    /**
     * The type Unterminated comment exception.
     */
    private static class UnterminatedCommentException extends Exception {

        /**
         * The constant serialVersionUID.
         */
        private static final long serialVersionUID = 7971352218559346169L;
    }

    /**
     * The type Unterminated string literal exception.
     */
    private static class UnterminatedStringLiteralException extends Exception {

        /**
         * The constant serialVersionUID.
         */
        private static final long serialVersionUID = 3813645314180522143L;
    }

    /**
     * The type Unterminated reg exp literal exception.
     */
    private static class UnterminatedRegExpLiteralException extends Exception {

        /**
         * The constant serialVersionUID.
         */
        private static final long serialVersionUID = -5088088141334641219L;
    }

}
