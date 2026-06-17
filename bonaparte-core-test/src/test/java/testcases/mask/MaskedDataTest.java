package testcases.mask;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.jpaw.bonaparte.pojos.maskedTest.Child;
import de.jpaw.bonaparte.pojos.maskedTest.Superclass;

public class MaskedDataTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaskedDataTest.class);
	private static final UUID apiKey = UUID.randomUUID();

    @Test
    public void testMaskedData() {
    	final Superclass data = new Superclass(3, "secret", apiKey);
		final String maskedString = data.toString();
		LOGGER.info("Masked string: {}", maskedString);
		
		// Verify that the masked string does not contain the original values
		assertFalse(maskedString.contains("secret"), "Masked string should not contain the original string");
		assertFalse(maskedString.contains("12345"), "Masked string should not contain the original integer");
		
		// Verify that the masked string is not empty
		assertFalse(maskedString.isEmpty(), "Masked string should not be empty");
    }

    @Test
    public void testMaskedDataInSuperclass() {
    	final Child data = new Child(3, "secret", apiKey, "more");
		final String maskedString = data.toString();
		LOGGER.info("Masked string: {}", maskedString);
		
		// Verify that the masked string does not contain the original values
		assertFalse(maskedString.contains("secret"), "Masked string should not contain the original string");
		assertFalse(maskedString.contains("12345"), "Masked string should not contain the original integer");
		
		// Verify that the masked string is not empty
		assertFalse(maskedString.isEmpty(), "Masked string should not be empty");
    }
}
