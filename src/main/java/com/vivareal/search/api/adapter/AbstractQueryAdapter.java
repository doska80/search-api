package com.vivareal.search.api.adapter;

import org.elasticsearch.common.regex.Regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class AbstractQueryAdapter {

    // (\s+OR\s+)([a-z0-9_\-]+)\s*:\s* // OR separator
    // ^\s*([a-z0-9_\-]+)\s*:\s*(.*)\s*$ // Field/Value
    protected static final String FIELD = "\\s*([a-z0-9_\\-]+)\\s*:";
    protected static final Pattern FIELD_VALUE = Pattern.compile("^\\s*([a-z0-9_\\-]+)\\s*:\\s*(.*)\\s*$", Pattern.CASE_INSENSITIVE);
    protected static final Pattern OR = Pattern.compile("(.+)(\\s+OR\\s+)([a-z0-9_\\-]+\\s*:\\s*.+)", Pattern.CASE_INSENSITIVE);

//    Pattern.compile("(\\s+OR\\s+)([a-z0-9_\\-]+)\\s*:\\s*");

    public static void main(String[] args) {
//        Matcher matches = OR.matcher("Campo1: valor or campoDois:lallaala");
//        matches.groupCount();
////        matches.group
//        System.out.println(matches);
//        String[] splitted = OR.split("Campo1: valor or campoDois:lallaala  or campoTres: lelele");
//        System.out.println(splitted);


//        Pattern fdp = Pattern.compile("(\\s+OR\\s+)([a-z0-9_\\-]+\\s*:.*)", Pattern.CASE_INSENSITIVE);
        String filterQuery = "Campo1: valor or campoDois:lallaala or campo3:valor4";
        do {
            Matcher matchedFieldValue = FIELD_VALUE.matcher(filterQuery);
            if (!matchedFieldValue.matches())
                break;
            String field = matchedFieldValue.group(1);
            String value = matchedFieldValue.group(2);
            Matcher splittedValue = OR.matcher(value);
            if (splittedValue.find()) {
                value = splittedValue.group(1);
                filterQuery = splittedValue.group(2);
                System.out.println(field + ": " + value);
            } else {
                System.out.println(field + ": " + value);
                break;
            }
        } while (true);
//        Matcher matchedFieldValue = FIELD_VALUE.matcher("Campo1: valor or campoDois:lallaala or campo3:valor4");
////        if
//
////Matcher matcher = pattern.matcher(s);
//        while (fdp2.find()) {
//            System.out.println(fdp2.group(1));
//            System.out.println(fdp2.group(2));
//        }
//
////
////
//System.out.println(fdp2.find());
//System.out.println(fdp2.groupCount());
//System.out.println(fdp2.group(1));







    }
}
