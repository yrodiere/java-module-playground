package org.hibernate.sandbox.java.service.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public final class ServiceConsumer {

	private ServiceConsumer() {
	}

	public static List<MyService> loadMyServices() {
		List<MyService> result = new ArrayList<>();
		ServiceLoader.load( MyService.class ).forEach( result::add );
		return result;
	}

}
