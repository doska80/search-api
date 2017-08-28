package com.vivareal.search.api.model.query;

import static java.lang.String.valueOf;

public class LikeValue extends Value {

    private static final int SPACE_ASCII_CODE = 32;
    private static final int PERCENT_ASCII_CODE = 37;
    private static final int UNDERSCORE_ASCII_CODE = 95;
    private static final int CHAR_N_ASCII_CODE = 110;

    private static final String ASTERISK_ASCII_STRING = "*";
    private static final int ASTERISK_ASCII_CODE = 42;

    private static final String QUESTION_MARK_ASCII_STRING = "?";
    private static final int QUESTION_MARK_ASCII_CODE = 63;

    private static final String SCAPE_ASCII_STRING = "\\";
    private static final int SCAPE_ASCII_CODE = 92;

    public LikeValue(Value content) {
        super(normalizeQuery(valueOf(content.getContents(0))));
    }

    private static String normalizeQuery(final String query) {
        StringBuilder finalQuery = new StringBuilder();
        int size = query.length();

        for (int i = 0; i < size; i++) {

            char current = query.charAt(i);

            if (current >= 97 || current == SPACE_ASCII_CODE) {
                finalQuery.append(current);
                continue;
            }

            char previous = 0;
            char next = 0;

            if (i > 0) previous = query.charAt(i - 1);
            if ((i + 1) < size) next = query.charAt(i + 1);

            switch (current) {
                case SCAPE_ASCII_CODE:
                    if (next == PERCENT_ASCII_CODE || next == UNDERSCORE_ASCII_CODE) {
                        finalQuery.append(next);
                        i += 1;
                    } else if (next == CHAR_N_ASCII_CODE) {
                        finalQuery.append('\n');
                        i += 1;
                    } else {
                        finalQuery.append(current);
                    }
                    break;

                case PERCENT_ASCII_CODE:
                    finalQuery.append(ASTERISK_ASCII_STRING);
                    break;

                case ASTERISK_ASCII_CODE:
                    append(finalQuery, ASTERISK_ASCII_STRING, previous);
                    break;

                case QUESTION_MARK_ASCII_CODE:
                    append(finalQuery, QUESTION_MARK_ASCII_STRING, previous);
                    break;

                case UNDERSCORE_ASCII_CODE:
                    finalQuery.append(QUESTION_MARK_ASCII_STRING);
                    break;

                default:
                    finalQuery.append(current);
            }
        }

        return finalQuery.toString();
    }

    private static void append(StringBuilder finalQuery, String charToAppend, char previous) {
        if (previous != SCAPE_ASCII_CODE)
            finalQuery.append(SCAPE_ASCII_STRING);
        finalQuery.append(charToAppend);
    }

}
