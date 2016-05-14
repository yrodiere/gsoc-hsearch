package io.github.mincongh.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CalculatorServiceTest {

	@Test
	public void additionTest() {
		final int a = 1;
		final int b = 2;
		CalculatorService calculatorService = new CalculatorService();
		assertEquals(a + b, calculatorService.addition(a, b));
	}
}
