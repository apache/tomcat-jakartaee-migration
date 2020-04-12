package org.apache.tomcat.jakartaee;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EESpecProfileTest {

	@Test
	public void testConvert_withTomcatProfile() {

		// prepare
		EESpecProfile profile = EESpecProfile.TOMCAT;

		// test: classes moved to jakarta.* packages
		testConverted("javax.annotation.PostConstruct", profile);
		testConverted("javax.ejb.EJB", profile);
		testConverted("javax.el.ELContext", profile);
		testConverted("javax.mail.Session", profile);
		testConverted("javax.persistence.PersistenceContext", profile);
		testConverted("javax.security.auth.message.MessageInfo", profile);
		testConverted("javax.servlet.ServletContext", profile);
		testConverted("javax.transaction.Transaction", profile);
		testConverted("javax.websocket.Endpoint", profile);

		// test: classes not moved to jakarta.* packages
		// (these classes are still part of the JDK - even in Java 11 and greater)
		testNotConverted("javax.annotation.processing.Processor", profile);
		testNotConverted("javax.transaction.xa.XAResource", profile);

	}

	private void testConverted(String className, EESpecProfile profile) {
		String result = profile.convert(className);
		String expectedResult = className.replace("javax.", "jakarta.");
		assertEquals(expectedResult, result);
	}

	private void testNotConverted(String className, EESpecProfile profile) {
		String result = profile.convert(className);
		assertEquals(className, result);
	}

}