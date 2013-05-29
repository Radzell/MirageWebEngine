package com.server;

import java.util.Vector;
import java.util.concurrent.Callable;

import com.utils.Config;

@SuppressWarnings("rawtypes")
public class CallBackTask implements Callable {
	private RecognitionProcess callBack;
	private int begin = 0, end = 0;
	private Job job;

	public CallBackTask() {
	}

	public CallBackTask(int i, int begin, int end,Job job) {
		this.begin = begin;
		this.end = end;
		this.job = job;

	}

	public Object call() {
		Vector<Integer> ids = null;
//		ids = Matcher.match("/home/diego/Desktop/Mirage/uploads/"+job.getFilename(), begin, end);
//		ids = Matcher.match("/home/diego/MirageFiles/uploads/"+job.getFilename(), begin, end);
		
		
		ids = Matcher.match(Config.getPathUploads()+job.getFilename(), begin, end);
		
		
		callBack.callBack(ids, job);
		return null;
	}

	public static long fib(int n) {
		if (n <= 1)
			return n;
		else
			return fib(n - 1) + fib(n - 2);
	}

	public void setCaller(RecognitionProcess callBack) {
		this.callBack = callBack;
	}

	public RecognitionProcess getCaller() {
		return callBack;
	}
}