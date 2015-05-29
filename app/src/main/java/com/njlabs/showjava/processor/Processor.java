package com.njlabs.showjava.processor;

import android.os.Handler;

import com.njlabs.showjava.utils.ExceptionHandler;

/**
 * Created by Niranjan on 29-05-2015.
 */
public class Processor {
    public static void extract(ProcessService processService, Handler UIHandler, String packageDir, String packageID, ExceptionHandler exceptionHandler){
        JarExtractor jarExtractor = new JarExtractor(processService,UIHandler,packageDir,packageID,exceptionHandler);
        jarExtractor.extract();
    }
}
