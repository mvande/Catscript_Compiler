package edu.montana.csci.csci468;

import edu.montana.csci.csci468.CatscriptTestBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CapstoneTests extends CatscriptTestBase
{

    // This test ensures that the compiler successfully handles variable names,
    // function calls, and print statements
    @Test
    void Test1() {
        assertEquals("57\n", compile("var y = 57\n" +
                "function bingus() {}" +
                "bingus()" +
                "print(y)"));
    }

    // This test ensures that strings can be assigned as variables, called by functions,
    // and printed successfully
    @Test
    void Test2() {
        assertEquals("dreeb\n", compile("function bingus(y) { print(y) }" +
                "bingus(\"dreeb\")"));
    }

    // This test ensures that the compiler can correctly take object type parameters and
    // handle printing of passed values in that scenario.
    @Test
    void Test3() {
        assertEquals("bungus\n", compile("function scrungus(x : object) { print(x) }" +
                "scrungus(\"bungus\")"));
    }

}
