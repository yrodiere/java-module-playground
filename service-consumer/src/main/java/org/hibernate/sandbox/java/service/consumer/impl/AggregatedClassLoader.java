/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sandbox.java.service.consumer.impl;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.hibernate.AssertionFailure;

public class AggregatedClassLoader extends ClassLoader {

	private static final Method SERVICE_LOADER_STREAM_METHOD;
	private static final Method PROVIDER_TYPE_METHOD;

	static {
		Class<?> serviceLoaderClass = ServiceLoader.class;
		Method serviceLoaderStreamMethod = null;
		Method providerTypeMethod = null;
		try {
			// JDK 9 introduced the stream() method on ServiceLoader
			serviceLoaderStreamMethod = serviceLoaderClass.getMethod( "stream" );
			Class<?> providerClass = Class.forName( serviceLoaderClass.getName() + "$Provider" );
			providerTypeMethod = providerClass.getMethod( "type" );
		}
		catch (NoSuchMethodException | ClassNotFoundException e) {
			// ignore, we will deal with that later.
		}

		SERVICE_LOADER_STREAM_METHOD = serviceLoaderStreamMethod;
		PROVIDER_TYPE_METHOD = providerTypeMethod;
	}

	private final ClassLoader[] individualClassLoaders;
	private final TcclLookupPrecedence tcclLookupPrecedence;

	public AggregatedClassLoader(final LinkedHashSet<ClassLoader> orderedClassLoaderSet, TcclLookupPrecedence precedence) {
		super( null );
		individualClassLoaders = orderedClassLoaderSet.toArray( new ClassLoader[orderedClassLoaderSet.size()] );
		tcclLookupPrecedence = precedence;
	}

	private Iterator<ClassLoader> newClassLoaderIterator() {
		final ClassLoader threadClassLoader = locateTCCL();
		if ( tcclLookupPrecedence == TcclLookupPrecedence.NEVER || threadClassLoader == null ) {
			return newTcclNeverIterator();
		}
		else if ( tcclLookupPrecedence == TcclLookupPrecedence.AFTER ) {
			return newTcclAfterIterator(threadClassLoader);
		}
		else if ( tcclLookupPrecedence == TcclLookupPrecedence.BEFORE ) {
			return newTcclBeforeIterator(threadClassLoader);
		}
		else {
			throw new RuntimeException( "Unknown precedence: "+tcclLookupPrecedence );
		}
	}

	private Iterator<ClassLoader> newTcclBeforeIterator(final ClassLoader threadContextClassLoader) {
		final ClassLoader systemClassLoader = locateSystemClassLoader();
		return new Iterator<ClassLoader>() {
			private int currentIndex = 0;
			private boolean tcCLReturned = false;
			private boolean sysCLReturned = false;

			@Override
			public boolean hasNext() {
				if ( !tcCLReturned ) {
					return true;
				}
				else if ( currentIndex < individualClassLoaders.length ) {
					return true;
				}
				else if ( !sysCLReturned && systemClassLoader != null ) {
					return true;
				}

				return false;
			}

			@Override
			public ClassLoader next() {
				if ( !tcCLReturned ) {
					tcCLReturned = true;
					return threadContextClassLoader;
				}
				else if ( currentIndex < individualClassLoaders.length ) {
					currentIndex += 1;
					return individualClassLoaders[ currentIndex - 1 ];
				}
				else if ( !sysCLReturned && systemClassLoader != null ) {
					sysCLReturned = true;
					return systemClassLoader;
				}
				throw new IllegalStateException( "No more item" );
			}
		};
	}

	private Iterator<ClassLoader> newTcclAfterIterator(final ClassLoader threadContextClassLoader) {
		final ClassLoader systemClassLoader = locateSystemClassLoader();
		return new Iterator<ClassLoader>() {
			private int currentIndex = 0;
			private boolean tcCLReturned = false;
			private boolean sysCLReturned = false;

			@Override
			public boolean hasNext() {
				if ( currentIndex < individualClassLoaders.length ) {
					return true;
				}
				else if ( !tcCLReturned ) {
					return true;
				}
				else if ( !sysCLReturned && systemClassLoader != null ) {
					return true;
				}

				return false;
			}

			@Override
			public ClassLoader next() {
				if ( currentIndex < individualClassLoaders.length ) {
					currentIndex += 1;
					return individualClassLoaders[ currentIndex - 1 ];
				}
				else if ( !tcCLReturned ) {
					tcCLReturned = true;
					return threadContextClassLoader;
				}
				else if ( !sysCLReturned && systemClassLoader != null ) {
					sysCLReturned = true;
					return systemClassLoader;
				}
				throw new IllegalStateException( "No more item" );
			}
		};
	}

	private Iterator<ClassLoader> newTcclNeverIterator() {
		final ClassLoader systemClassLoader = locateSystemClassLoader();
		return new Iterator<ClassLoader>() {
			private int currentIndex = 0;
			private boolean sysCLReturned = false;

			@Override
			public boolean hasNext() {
				if ( currentIndex < individualClassLoaders.length ) {
					return true;
				}
				else if ( !sysCLReturned && systemClassLoader != null ) {
					return true;
				}

				return false;
			}

			@Override
			public ClassLoader next() {
				if ( currentIndex < individualClassLoaders.length ) {
					currentIndex += 1;
					return individualClassLoaders[ currentIndex - 1 ];
				}
				else if ( !sysCLReturned && systemClassLoader != null ) {
					sysCLReturned = true;
					return systemClassLoader;
				}
				throw new IllegalStateException( "No more item" );
			}
		};
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		final LinkedHashSet<URL> resourceUrls = new LinkedHashSet<URL>();
		final Iterator<ClassLoader> clIterator = newClassLoaderIterator();
		while ( clIterator.hasNext() ) {
			final ClassLoader classLoader = clIterator.next();
			final Enumeration<URL> urls = classLoader.getResources( name );
			while ( urls.hasMoreElements() ) {
				resourceUrls.add( urls.nextElement() );
			}
		}

		return new Enumeration<URL>() {
			final Iterator<URL> resourceUrlIterator = resourceUrls.iterator();

			@Override
			public boolean hasMoreElements() {
				return resourceUrlIterator.hasNext();
			}

			@Override
			public URL nextElement() {
				return resourceUrlIterator.next();
			}
		};
	}

	@Override
	protected URL findResource(String name) {
		final Iterator<ClassLoader> clIterator = newClassLoaderIterator();
		while ( clIterator.hasNext() ) {
			final ClassLoader classLoader = clIterator.next();
			final URL resource = classLoader.getResource( name );
			if ( resource != null ) {
				return resource;
			}
		}
		return super.findResource( name );
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		final Iterator<ClassLoader> clIterator = newClassLoaderIterator();
		while ( clIterator.hasNext() ) {
			final ClassLoader classLoader = clIterator.next();
			try {
				return classLoader.loadClass( name );
			}
			catch (Exception ignore) {
			}
			catch (LinkageError ignore) {
			}
		}

		throw new ClassNotFoundException( "Could not load requested class : " + name );
	}

	public <S> AggregatedServiceLoader<S> createAggregatedServiceLoader(Class<S> serviceContract) {
		if ( SERVICE_LOADER_STREAM_METHOD != null ) {
			return new ClassPathAndModulePathAggregatedServiceLoader<>( serviceContract );
		}
		else {
			return new ClassPathOnlyAggregatedServiceLoader<>( serviceContract );
		}
	}

	private static ClassLoader locateSystemClassLoader() {
		try {
			return ClassLoader.getSystemClassLoader();
		}
		catch (Exception e) {
			return null;
		}
	}

	private static ClassLoader locateTCCL() {
		try {
			return Thread.currentThread().getContextClassLoader();
		}
		catch (Exception e) {
			return null;
		}
	}

	public class ClassPathOnlyAggregatedServiceLoader<S> implements AggregatedServiceLoader<S> {
		private final ServiceLoader<S> delegate;

		private ClassPathOnlyAggregatedServiceLoader(Class<S> serviceContract) {
			this.delegate = ServiceLoader.load( serviceContract, AggregatedClassLoader.this );
		}

		@Override
		public Collection<S> getAll() {
			final Set<S> services = new LinkedHashSet<>();
			for ( S service : delegate ) {
				services.add( service );
			}
			return services;
		}

		@Override
		public void reload() {
			delegate.reload();
		}
	}

	public class ClassPathAndModulePathAggregatedServiceLoader<S> implements AggregatedServiceLoader<S> {
		private final List<ServiceLoader<S>> delegates;

		private ClassPathAndModulePathAggregatedServiceLoader(Class<S> serviceContract) {
			this.delegates = new ArrayList<>();
			// Always try the aggregated class loader first
			this.delegates.add( ServiceLoader.load( serviceContract, AggregatedClassLoader.this ) );

			// Then also try the individual class loaders,
			// because only them can instantiate services provided by jars in the module path
			final Iterator<ClassLoader> clIterator = newClassLoaderIterator();
			while ( clIterator.hasNext() ) {
				this.delegates.add(
						ServiceLoader.load( serviceContract, clIterator.next() )
				);
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public Collection<S> getAll() {
			final Set<String> loadedTypes = new LinkedHashSet<>();
			final Set<S> services = new LinkedHashSet<>();
			delegates.stream()
					// Each loader's stream() method returns a stream of service providers: flatten that into a single stream
					.flatMap( delegate -> {
						try {
							return (Stream<? extends Supplier<S>>) SERVICE_LOADER_STREAM_METHOD.invoke( delegate );
						}
						catch (RuntimeException | IllegalAccessException | InvocationTargetException e) {
							throw new AssertionFailure( "Error calling ServiceLoader.stream()", e );
						}
					} )
					.forEach( provider -> {
						Class<?> type;
						try {
							type = (Class<?>) PROVIDER_TYPE_METHOD.invoke( provider );
						}
						catch (RuntimeException | IllegalAccessException | InvocationTargetException e) {
							throw new AssertionFailure( "Error calling ServiceLoader.Provider.type()", e );
						}
						String typeName = type.getName();
						// Only instantiate the first encountered instance of each type
						if ( loadedTypes.add( typeName ) ) {
							services.add( provider.get() );
						}
					} );
			return services;
		}

		@Override
		public void reload() {
			for ( ServiceLoader<S> delegate : delegates ) {
				delegate.reload();
			}
		}
	}


}
