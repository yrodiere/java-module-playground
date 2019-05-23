/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sandbox.java.service.consumer.impl;

import java.util.Collection;

/**
 * A service loader bound to an {@link AggregatedClassLoader}.
 * @param <S> The type of the service contract.
 */
public interface AggregatedServiceLoader<S> {

	Collection<S> getAll();

	void reload();

}
