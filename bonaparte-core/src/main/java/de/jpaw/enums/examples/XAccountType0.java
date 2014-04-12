package de.jpaw.enums.examples;

import de.jpaw.enums.AbstractXEnumBase;
import de.jpaw.enums.XEnum;
import de.jpaw.enums.XEnumFactory;

/** Extendable enum instance type. This class has immutable instances. Also, after static initialization, no additional members can be created. */ 
public class XAccountType0 extends AbstractXEnumBase<XAccountType0> {
	public static final int MAX_TOKEN_LENGTH = 1;
	public static final String PQON = "base.XAccountType0";
	public static final int NUM_VALUES_TOTAL = AccountType0.values().length;
	
	// root class builds the factory
	public static final XEnumFactory<XAccountType0> myFactory = new XEnumFactory<XAccountType0>(MAX_TOKEN_LENGTH, XAccountType0.class, PQON);
	static {
		// create all the instances
		// create a factory instance
		AccountType0 [] values = AccountType0.values();
		for (int i = 0; i < values.length; ++i) {
			AccountType0 e = values[i];
			myFactory.publishInstance(new XAccountType0(e, i, e.name(), e.getToken(), myFactory));
		}
		myFactory.register(PQON);
	}
	@Override
	public String getPqon() {
		return PQON;
	}
	
	// constructor may not be accessible from the outside
	protected XAccountType0(Enum<?> enumVal, int ordinal, String name, String token, XEnumFactory<XAccountType0> myFactory) {
		super(enumVal, ordinal, name, token, myFactory);
	}
}
