package com.server;

import java.util.Vector;
import java.util.concurrent.Callable;


@SuppressWarnings("rawtypes")
public class CallBackTest implements Callable {
	private Server callBack;
	private int begin = 0, end = 0;
	private Job job;

	public CallBackTest() {
	}

	public CallBackTest(int i, int begin, int end,Job job) {
		this.begin = begin;
		this.end = end;
		this.job = job;

	}

	public Object call() {
		Vector<Integer> ids = null;
//		ids = Matcher.match("/home/diego/Desktop/Mirage/uploads/"+job.getFilename(), begin, end);
//		ids = Matcher.match("/home/diego/MirageFiles/uploads/"+job.getFilename(), begin, end);
		
		//ids = Matcher.match(job.getFilename(), begin, end);
		
		callBack.callback(ids);
		return null;
	}

	public static long fib(int n) {
		if (n <= 1)
			return n;
		else
			return fib(n - 1) + fib(n - 2);
	}

	public void setCaller(Server callBack) {
		this.callBack = callBack;
	}

	public Server getCaller() {
		return callBack;
	}
}