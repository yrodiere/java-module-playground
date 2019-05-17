package org.hibernate.sandbox.java.service.provider;

import org.hibernate.sandbox.java.service.consumer.MyService;

public class MyServiceImpl implements MyService {

	@Override
	public String toString() {
		return getClass().getName();
	}

}
