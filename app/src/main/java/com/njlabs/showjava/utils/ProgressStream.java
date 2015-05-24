package com.njlabs.showjava.utils;


import java.io.IOException;
import java.io.OutputStream;

public class ProgressStream extends OutputStream {

	public ProgressStream()
	{
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
		}
	}
	@Override
	public void write(int arg0) throws IOException {
		// TODO Auto-generated method stub

	}

}
