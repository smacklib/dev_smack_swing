package org.jdesktop.util.converters;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import org.jdesktop.util.ResourceMap;


class ConverterUtils
{
    /**
    *
    * @param s
    * @param resourceMap
    * @return
    * @throws Exception
    */
   public static ImageIcon loadImageIcon(String s, ResourceMap resourceMap)
           throws Exception {
       String rPath = resourcePath(s, resourceMap);
       if (rPath == null) {
           String msg = String.format("invalid image/icon path \"%s\"", s);
           throw new Exception(msg);
       }
       URL url = resourceMap.getClassLoader().getResource(rPath);
       if (url != null) {
           return new ImageIcon(url);
       } else {
           String msg = String.format("couldn't find Icon resource \"%s\"", s);
           throw new Exception(msg);
       }
   }

   /**
    * If path doesn't have a leading "/" then the resourcesDir
    * is prepended, otherwise the leading "/" is removed.
    */
   private static String resourcePath(final String path, ResourceMap resourceMap) {
       if (path == null) {
           return null;
       } else if (path.startsWith("/")) {
           return (path.length() > 1) ? path.substring(1) : null;
       } else {
           return resourceMap.getResourceDir() + path;
       }
   }

   /**
    * String s is assumed to contain n number substrings separated by
    * commas.  Return a list of those integers or null if there are too
    * many, too few, or if a substring can't be parsed.  The format
    * of the numbers is specified by Double.valueOf().
    */
   public static List<Double> parseDoubles(String s, int n, String errorMsg) throws Exception {
       String[] doubleStrings = s.split(",", n + 1);
       if (doubleStrings.length != n) {
           throw new Exception(errorMsg + ": " + s);
       } else {
           List<Double> doubles = new ArrayList<Double>(n);
           for (String doubleString : doubleStrings) {
               try {
                   doubles.add(Double.valueOf(doubleString));
               } catch (NumberFormatException e) {
                   throw new Exception(errorMsg +": " + s, e);
               }
           }
           return doubles;
       }
   }
}
