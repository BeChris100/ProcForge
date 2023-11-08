package net.bc100dev.pfc.cg;

class Utils {

    public static String toString(String[] strings, String spliterator) {
        if (strings == null)
            return "";

        if (strings.length == 0)
            return "";

        StringBuilder str = new StringBuilder();
        for (String string : strings)
            str.append(string).append(spliterator);

        return str.substring(0, str.toString().length() - spliterator.length());
    }

}
