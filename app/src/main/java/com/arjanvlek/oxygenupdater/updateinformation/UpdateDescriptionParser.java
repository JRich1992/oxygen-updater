package com.arjanvlek.oxygenupdater.updateinformation;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.arjanvlek.oxygenupdater.internal.logger.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static android.graphics.Typeface.BOLD;

public class UpdateDescriptionParser {

    private static final String TAG = "UpdateDescriptionParser";
    private static final String EMPTY_STRING = "";
    private static final String NEWLINE = "\n";
    private static final String OXYGEN_OS_BETA_IDENTIFIER = "O2_Open_";
    private static final String OXYGEN_OS_BETA_DISPLAY_PREFIX = "OxygenOS open beta ";
    private static final String OXYGEN_OS_VERSION_PREFIX_UPPERCASE = "OS Version: ";
    private static final String OXYGEN_OS_VERSION_PREFIX_LOWERCASE = "OS version: ";

    private enum UpdateDescriptionElement {

        // Grammar for OnePlus update descriptions
        HEADING_1,              //      #(char*)\n
        HEADING_2,              //      ##(char*)\n
        HEADING_3,              //      ###(char*)\n
        LINE_SEPARATORS,        //      \(\*)\n
        LIST_ITEM,              //      *(char*)\n
        LINK,                   //      [(char*)](space*)((char*))\n
        TEXT,                   //      (char*)
        EMPTY;                  //

        // Finds the element type for a given line of OnePlus formatted text.
        private static UpdateDescriptionElement of(String inputLine) {
            // The empty string gets parsed as EMPTY
            Logger.logVerbose(TAG, "Input line: " + inputLine);

            if (inputLine == null || inputLine.isEmpty()) {
                Logger.logVerbose(TAG, "Matched type: EMPTY");
                return EMPTY;
            } else if(inputLine.contains("###")) {
                Logger.logVerbose(TAG, "Matched type: HEADING_3");
                return HEADING_3;
            } else if (inputLine.contains("##")) {
                Logger.logVerbose(TAG, "Matched type: HEADING_2");
                return HEADING_2;
            } else if (inputLine.contains("#")) {
                Logger.logVerbose(TAG, "Matched type: HEADING_1");
                return HEADING_1;
            } else if(inputLine.contains("*")) {
                Logger.logVerbose(TAG, "Matched type: LIST_ITEM");
                return LIST_ITEM;
            } else if (inputLine.contains("\\")) {
                Logger.logVerbose(TAG, "Matched type: LINE_SEPARATORS");
                return LINE_SEPARATORS;
            } else if (inputLine.contains("[") && inputLine.contains("]") && inputLine.contains("(") && inputLine.contains(")")) {
                Logger.logVerbose(TAG, "Matched type: LINK");
                return LINK;
            } else {
                Logger.logVerbose(TAG, "Matched type: TEXT");
                return TEXT;
            }
        }

        // Finds the type of element of a modified line by looking it back up in the original text.
        public static UpdateDescriptionElement find(String modifiedLine, String originalText) throws IOException {
            BufferedReader reader = new BufferedReader(new StringReader(originalText));
            String currentLine;

            // As almost all the modifications that are made are substrings, this means the original text should always contain the modified line.
            // Then, the "of" function can be used with the belonging line to lookup the element type of the modified line.
            // The only exception is the empty line, but it's no problem that one is returned as TEXT, because the empty line is not needed for SpannableString.
            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.contains(modifiedLine)) return of(currentLine);
            }

            return TEXT;
        }
    }

    public static boolean containsHeaderLine(String updateDescription) {
        String firstLine = getFirstLine(updateDescription);
        return firstLine.contains(OXYGEN_OS_VERSION_PREFIX_UPPERCASE) || firstLine.contains(OXYGEN_OS_VERSION_PREFIX_LOWERCASE);
    }

    public static String getFirstLine(String updateDescription) {
        if (updateDescription == null || updateDescription.isEmpty()) return EMPTY_STRING;

        BufferedReader reader = new BufferedReader(new StringReader(updateDescription));

        try {
            String line = reader.readLine();
            return line == null ? EMPTY_STRING : line;
        } catch (IOException e) {
            return EMPTY_STRING;
        }
    }

    public static boolean isBeta(String updateTitle) {
        return updateTitle != null && updateTitle.contains(OXYGEN_OS_BETA_IDENTIFIER);
    }

    public static String getFormattedUpdateTitle(String updateDescription) {
        try {
            if (isBeta(getFirstLine(updateDescription))) {
                return OXYGEN_OS_BETA_DISPLAY_PREFIX + updateDescription.substring(updateDescription.indexOf(OXYGEN_OS_BETA_IDENTIFIER) + OXYGEN_OS_BETA_IDENTIFIER.length(), updateDescription.indexOf(NEWLINE));
            } else if (updateDescription.contains(OXYGEN_OS_VERSION_PREFIX_UPPERCASE)) {
                return updateDescription.substring(updateDescription.indexOf(OXYGEN_OS_VERSION_PREFIX_UPPERCASE) + OXYGEN_OS_VERSION_PREFIX_UPPERCASE.length(), updateDescription.indexOf(NEWLINE));
            } else if (updateDescription.contains(OXYGEN_OS_VERSION_PREFIX_LOWERCASE)) {
                return updateDescription.substring(updateDescription.indexOf(OXYGEN_OS_VERSION_PREFIX_LOWERCASE) + OXYGEN_OS_VERSION_PREFIX_LOWERCASE.length(), updateDescription.indexOf(NEWLINE));
            } else {
                return updateDescription;
            }
        } catch (Exception e) {
            Logger.logError(TAG, "Failed to format update title: ", e);
            return updateDescription;
        }
    }

    public static Spanned parse(String updateDescription) {
        SpannableString result;

        final Map<String, String> links = new HashMap<>();

        String currentLine;
        try {
            BufferedReader reader = new BufferedReader(new StringReader(updateDescription));
            String modifiedUpdateDescription = EMPTY_STRING;

            // First, loop through all lines and modify them were needed.
            // This consists of removing heading symbols, making list items, adding line separators and displaying link texts.
            while ((currentLine = reader.readLine()) != null) {
                UpdateDescriptionElement element = UpdateDescriptionElement.of(currentLine);
                String modifiedLine = EMPTY_STRING;

                // If the current line contains the OxygenOS version number, skip it as it will be displayed as the update title.
                if(currentLine.contains(OXYGEN_OS_VERSION_PREFIX_LOWERCASE) || currentLine.contains(OXYGEN_OS_VERSION_PREFIX_UPPERCASE)) continue;

                switch (element) {
                    case HEADING_3:
                        modifiedLine = currentLine.replace("###", "");
                        break;
                    case HEADING_2:
                        modifiedLine = currentLine.replace("##", "");
                        break;
                    case HEADING_1:
                        modifiedLine = currentLine.replace("#", "");
                        break;
                    case LIST_ITEM:
                        modifiedLine = currentLine.replace("*", "•");
                    case LINE_SEPARATORS:
                        // There could also be multiple OnePlus line separators in this line.
                        // Replace each OnePlus line separator with an actual line separator.
                        char[] chars = currentLine.toCharArray();
                        for (char c : chars) {
                            if (c == '\\') {
                                modifiedLine = modifiedLine + "\n";
                            }
                        }
                        break;
                    case LINK:
                        String linkTitle = currentLine.substring(currentLine.indexOf("[") + 1, currentLine.lastIndexOf("]"));
                        String linkAddress = currentLine.substring(currentLine.indexOf("(") + 1, currentLine.lastIndexOf(")"));

                        // We need to save the full URL somewhere, to point the browser to it when clicked...
                        links.put(linkTitle, linkAddress);

                        // The link title will be displayed. It will also be used to look up the full url when clicked.
                        modifiedLine = linkTitle;
                        break;
                    default:
                        modifiedLine = currentLine;
                }

                modifiedUpdateDescription = modifiedUpdateDescription.concat(modifiedLine + (element.equals(UpdateDescriptionElement.LINE_SEPARATORS) ? "" : "\n"));
            }

            // Finally, loop through the modified update description and set formatting attributes for the headers and links.
            reader = new BufferedReader(new StringReader(modifiedUpdateDescription));
            result = new SpannableString(modifiedUpdateDescription);


            while ((currentLine = reader.readLine()) != null) {
                if (currentLine.isEmpty()) continue;

                UpdateDescriptionElement element = UpdateDescriptionElement.find(currentLine, updateDescription);

                int startPosition = modifiedUpdateDescription.indexOf(currentLine);
                int endPosition = startPosition + currentLine.length();

                switch (element) {
                    case HEADING_1:
                        // Heading 1 should be made bold and pretty large.
                        result.setSpan(new RelativeSizeSpan(1.3f), startPosition, endPosition, 0);
                        result.setSpan(new StyleSpan(BOLD), startPosition, endPosition, 0);
                        break;
                    case HEADING_2:
                        // Heading 2 should be made bold and a bit larger than normal, but smaller than heading 1.
                        result.setSpan(new RelativeSizeSpan(1.1f), startPosition, endPosition, 0);
                        result.setSpan(new StyleSpan(BOLD), startPosition, endPosition, 0);
                        break;
                    case HEADING_3:
                        // Heading 3 is the same size as normal text but will be displayed in bold.
                        result.setSpan(new StyleSpan(BOLD), startPosition, endPosition, 0);
                        break;
                    case LINK:
                        // A link should be made clickable and must be displayed as a hyperlink.
                        result.setSpan(new FormattedURLSpan(links.get(currentLine)), startPosition, endPosition, 0);
                        break;
                }
            }


        } catch (Exception e) {
            // If an error occurred, log it and return the original / unmodified update description
            Logger.logError(TAG, "Error parsing update description", e);
            return new SpannableString(updateDescription);
        }

        return result;
    }

}
