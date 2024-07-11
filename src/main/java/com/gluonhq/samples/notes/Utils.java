package com.gluonhq.samples.notes;

import java.util.List;

public class Utils {

    public static String formatList(List<Integer> elements, String separator){

        if(elements == null || elements.size() == 0){
            return "";
        }

        StringBuilder sb = new StringBuilder();

        elements.forEach(el -> {
            sb.append(el);
            sb.append(separator);
        });

        return sb.substring(0, sb.length() - 1);
    }

}
