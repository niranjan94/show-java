package com.njlabs.showjava.processor;

/**
 * Created by Niranjan on 29-05-2015.
 */
public class Processor {
    public static void extract(ProcessService processService) {
        JarExtractor jarExtractor = new JarExtractor(processService);
        jarExtractor.extract();
    }
}
