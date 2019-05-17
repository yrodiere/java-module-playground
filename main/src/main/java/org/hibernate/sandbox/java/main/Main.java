package org.hibernate.sandbox.java.main;

import java.util.List;

import org.hibernate.sandbox.java.service.consumer.MyService;
import org.hibernate.sandbox.java.service.consumer.ServiceConsumer;

public class Main {

	public static void main(String[] args) {
		System.out.println( "======================================================" );
		System.out.println( "                  STARTING EXECUTION                  " );
		System.out.println( "======================================================" );
		System.out.println();
		System.out.println();

		ModuleLayer.boot().modules().stream()
				.filter( m -> m.getName().startsWith( "org.hibernate." ) )
				.map( Module::getDescriptor )
				.forEach( System.out::println );

		System.out.println();
		System.out.println();
		List<MyService> myServicesFromConsumer = ServiceConsumer.loadMyServices();
		System.out.println( "===== RESULT:" );
		System.out.println( "My services loaded from consumer module (automatic module without module-info.java): " );
		System.out.println( myServicesFromConsumer );

		System.out.println();
		System.out.println();
		System.out.println( "======================================================" );
		System.out.println( "                   ENDING EXECUTION                   " );
		System.out.println( "======================================================" );
	}

}
