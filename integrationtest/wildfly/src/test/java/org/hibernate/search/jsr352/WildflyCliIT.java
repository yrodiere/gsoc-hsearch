/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352;

import java.io.IOException;

import org.hibernate.search.jsr352.test.entity.Company;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Issue #150 & JBERET-263
 * Cannot start the job "massIndex" from WildFly CLI because it is not part of
 * the deployment.
 *
 * @author Mincong Huang
 */
@RunWith(Arquillian.class)
public class WildflyCliIT {

	@Deployment
	public static WebArchive createDeployment() {
		WebArchive war = ShrinkWrap.create( WebArchive.class, "WildflyCli.war"  )
				.addAsResource( "META-INF/persistence.xml" )
				.addAsResource( "META-INF/batch-jobs/make-deployment-as-batch-app.xml" ) // WFLY-7000
				.addAsWebInfResource( "jboss-deployment-structure.xml" )
				.addAsWebInfResource( EmptyAsset.INSTANCE, "beans.xml" )
				.addPackage( Company.class.getPackage() );
		return war;
	}

	@Test
	public void testJob() throws InterruptedException, IOException {
		// do nothing.
	}
}