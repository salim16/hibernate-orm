/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.sqm.mutation;

import java.sql.SQLException;

import org.hibernate.annotations.QueryHints;
import org.hibernate.query.IllegalMutationQueryException;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ExpectedException;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SqlResultSetMapping;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.CONTACTS, annotatedClasses = BasicMutationQueryTests.SillyEntity.class )
@SessionFactory
public class BasicMutationQueryTests {
	@Test
	void basicHqlDeleteTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "delete Contact" ).executeUpdate();
		} );
	}

	@Test
	void basicNativeDeleteTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createNativeMutationQuery( "delete from contacts" ).executeUpdate();
		} );
	}

	@Test
	void basicNamedHqlDeleteTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createNamedMutationQuery( "valid-hql" ).executeUpdate();
		} );
	}

	@Test
	void basicNamedNativeDeleteTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createNamedMutationQuery( "valid-native" ).executeUpdate();
		} );
	}

	@Test
	@ExpectedException( IllegalMutationQueryException.class )
	void basicNonMutationQueryTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createMutationQuery( "select c from Contact c" );
		} );
	}

	@Test
	@ExpectedException( IllegalMutationQueryException.class )
	void basicInvalidNamedHqlDeleteTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createNamedMutationQuery( "invalid-hql" );
		} );
	}

	@Test
	void basicInvalidNamedNativeDeleteTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			try {
				// we do not know unequivocally whether this is a select or mutation
				//		- this will be an execution exception
				session.createNamedMutationQuery( "invalid-native" ).executeUpdate();
				fail( "Expecting query to fail" );
			}
			catch (PersistenceException expected) {
				assertThat( expected.getCause() ).isNotNull();
				assertThat( expected.getCause().getCause() ).isNotNull();
				assertThat( expected.getCause().getCause() ).isInstanceOf( SQLException.class );
			}
		} );
	}

	@Test
	@ExpectedException( IllegalMutationQueryException.class )
	void basicUnequivocallyInvalidNamedNativeDeleteTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createNamedMutationQuery( "invalid-native-result" );
		} );
	}

	@Entity( name = "SillyEntity" )
	@Table( name = "SillyEntity" )
	@NamedQuery(
			name = "valid-hql",
			query = "delete Contact"
	)
	@NamedQuery(
			name = "invalid-hql",
			query = "select c from Contact c"
	)
	@NamedNativeQuery(
			name = "valid-native",
			query = "delete from contacts"
	)
	@NamedNativeQuery(
			name = "invalid-native",
			query = "select * from contacts"
	)
	@NamedNativeQuery(
			name = "invalid-native-result",
			query = "select * from contacts",
			resultClass = BasicMutationQueryTests.SillyEntity.class
	)
	public static class SillyEntity {
	    @Id
	    private Integer id;
	    @Basic
		private String name;

		private SillyEntity() {
			// for use by Hibernate
		}

		public SillyEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}