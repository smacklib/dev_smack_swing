package org.smack.swing.application.converters;

import java.awt.Point;
import java.util.List;

import org.smack.swing.application.ResourceConverter;
import org.smack.swing.application.ResourceMap;

public class PointStringConverter extends ResourceConverter {

    public PointStringConverter() {
        super(Point.class);
    }

    @Override
    public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
        List<Double> xy = ConverterUtils.parseDoubles(s, 2, "invalid x,y Point string");
        Point p = new Point();
        p.setLocation(xy.get(0), xy.get(1));
        return p;
    }
}

