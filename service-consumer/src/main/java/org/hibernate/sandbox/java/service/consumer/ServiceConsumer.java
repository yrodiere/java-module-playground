package org.hibernate.sandbox.java.service.consumer;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.ServiceContributor;

public final class ServiceConsumer {

	private ServiceConsumer() {
	}

	public static List<MyService> loadMyServices() {
		List<MyService> result = new ArrayList<>();
		ServiceLoader.load( MyService.class ).forEach( result::add );
		return result;
	}

	public static List<?> loadHibernateOrmServices() {
		List<Object> result = new ArrayList<>();
		ServiceLoader.load( Integrator.class ).forEach( result::add );
		ServiceLoader.load( ServiceContributor.class ).forEach( result::add );
		return result;
	}

}
