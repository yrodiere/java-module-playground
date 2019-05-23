package org.hibernate.sandbox.java.main;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.sandbox.java.service.consumer.MyService;
import org.hibernate.sandbox.java.service.consumer.ServiceConsumer;
import org.hibernate.search.mapper.orm.impl.HibernateSearchContextService;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceRegistryImplementor;

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
		List<?> hibernateOrmServicesFromConsumer = ServiceConsumer.loadHibernateOrmServices();
		System.out.println( "===== RESULT:" );
		System.out.println( "Hibernate ORM services loaded from consumer module (automatic module without module-info.java): " );
		System.out.println( hibernateOrmServicesFromConsumer );

		System.out.println();
		System.out.println();
		System.out.println( "Booting Hibernate ORM service registry..." );
		List<?> hibernateOrmServicesFromHibernateOrmServiceRegistry = startHibernateOrmServiceRegistryAndGetServices();
		System.out.flush();
		System.out.println( "Finished booting Hibernate ORM service registry." );
		System.out.println();
		System.out.println();
		System.out.println( "===== RESULT:" );
		System.out.println( "Hibernate services loaded from Hibernate ORM module by bootstrapping a service registry only: " );
		System.out.println( hibernateOrmServicesFromHibernateOrmServiceRegistry );

		System.out.println();
		System.out.println();
		System.out.println( "Booting Hibernate ORM session factory..." );
		List<?> hibernateOrmServicesFromHibernateOrmSessionFactory = startHibernateOrmSessionFactoryAndGetServices();
		System.out.flush();
		System.out.println( "Finished booting Hibernate ORM session factory." );
		System.out.println();
		System.out.println();
		System.out.println( "===== RESULT:" );
		System.out.println( "Hibernate services loaded from Hibernate ORM module by bootstrapping a whole session factory: " );
		System.out.println( hibernateOrmServicesFromHibernateOrmSessionFactory );

		System.out.println();
		System.out.println();
		System.out.println( "======================================================" );
		System.out.println( "                   ENDING EXECUTION                   " );
		System.out.println( "======================================================" );
	}

	private static List<?> startHibernateOrmServiceRegistryAndGetServices() {
		List<Object> loaded = new ArrayList<>();

		try ( ServiceRegistryImplementor serviceRegistry = createServiceRegistry() ) {
			collectOrmServices( loaded, serviceRegistry );
		}

		return loaded;
	}

	private static List<?> startHibernateOrmSessionFactoryAndGetServices() {
		List<Object> loaded = new ArrayList<>();
		MetadataSources metadataSources = new MetadataSources( createServiceRegistry() );
		Metadata metadata = metadataSources.buildMetadata();
		SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();

		try ( SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) sessionFactoryBuilder.build() ) {
			ServiceRegistryImplementor serviceRegistry = sessionFactory.getServiceRegistry();
			collectOrmServices( loaded, serviceRegistry );
		}

		return loaded;
	}

	private static ServiceRegistryImplementor createServiceRegistry() {
		StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
		registryBuilder.applySetting( AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect" );
		return (ServiceRegistryImplementor) registryBuilder.build();
	}

	private static void collectOrmServices(List<Object> loaded, ServiceRegistryImplementor serviceRegistry) {
		IntegratorService integratorService = serviceRegistry.getService( IntegratorService.class );
		if ( integratorService != null ) {
			integratorService.getIntegrators().forEach( loaded::add );
		}

		ServiceBinding<HibernateSearchContextService> searchContextServiceBinding =
				serviceRegistry.locateServiceBinding( HibernateSearchContextService.class );
		if ( searchContextServiceBinding != null ) {
			loaded.add( searchContextServiceBinding.getService() );
		}
	}

}
