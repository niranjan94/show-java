package com.njlabs.showjava.utils;


import com.njlabs.showjava.processor.ProcessService;

import java.io.IOException;
import java.io.OutputStream;

public class ProgressStream extends OutputStream {


    ProcessService.Processor task;
	
	public ProgressStream(ProcessService.Processor task)
	{
		this.task=task;
	}
	public void write(byte[] data,int i1,int i2) 
	{
		String str = new String(data);
		str = str.replace("\n", "").replace("\r", "");
		if(str==""||str==null||str==" ")
		{
						
		}
		else
		{
			task.doProgress(" "+str);	
		}
	}
	@Override
	public void write(int arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
