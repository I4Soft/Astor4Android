package com.example.root.bugapp2;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class ExampleUnitTest {
    @Test
    public void multy() throws Exception {
        assertEquals(Calculator.multiply(5,5), 25, 0);
    }
}
