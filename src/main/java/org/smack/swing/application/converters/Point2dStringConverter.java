package org.smack.swing.application.converters;

import java.awt.geom.Point2D;
import java.util.List;

import org.smack.swing.application.ResourceConverter;
import org.smack.swing.application.ResourceMap;

public class Point2dStringConverter extends ResourceConverter {

    public Point2dStringConverter() {
        super(Point2D.class);
    }

    @Override
    public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
        List<Double> xy = ConverterUtils.parseDoubles(s, 2, "invalid x,y Point string");
        return new Point2D.Double(xy.get(0), xy.get(1));
    }
}
