package dk.statsbiblioteket.mediestream.larmharvester.anonymiser;

import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Created by baj on 1/18/17.
 */
public class TestAnonymiser {

    public static String input_example1title = "Dilemma 7: Lars kontaktet dem - Mads introducere dilemma og får derefter lars med på telefon.";
    public static String input_example1description = "Lars boet i lille lejlighed på Vesterbro. Mathias flytter fra lejlighed og Lars og Camilla " +
            "skal have ny roomate. De forelsker sig i Cecilie. Cecilie skulle bare lige vide at Lars engang imellem går nøgen rundt - bare lige " +
            "hurtigt ind i køkken eller andet. Hun accepterer det - men nu vil hun have at han dækker sig til. Skal han bøje sig for hendes ønske?";
    @Test
    public void testAnonymise() throws IOException {
        System.out.println("input_example1title");
        System.out.println(new Anonymiser().anonymise(input_example1title));
        System.out.println("input_example1description");
        System.out.println(new Anonymiser().anonymise(input_example1description));
    }
}
