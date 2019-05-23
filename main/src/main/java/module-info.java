module org.hibernate.sandbox.java.main {
	requires org.hibernate.sandbox.java.service.consumer;

	requires org.hibernate.orm.core;
	requires org.hibernate.search.mapper.orm;
	requires java.persistence;

	/*
	 * Apparently this is necessary in order to use ORM,
	 * otherwise we get compile-time errors for some modules such as "cannot access javax.naming.Referenceable",
	 * or runtime errors for other modules such as "java.lang.NoClassDefFoundError: javax/xml/bind/JAXBException".
	 */
	requires java.naming;
	requires java.sql;
	requires java.xml.bind;
	requires net.bytebuddy;
	// Strangely, this is not required
	//requires h2;

	exports org.hibernate.sandbox.java.main;
}