package com.swift;

import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.iban4j.IbanFormatException;
import org.iban4j.UnsupportedCountryException;
import org.iban4j.bban.BbanStructure;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.fail;

@RunWith(Enclosed.class)
public class RegistryTest {

    private final static Registry registry;
    private static String ignoredListRegistryFormatExampleMethod [] = {"IQ", "SC", "CR", "LC", "ST", "BY"};
    private static String ignoredListCountryCodeMethod [] = {"IR"};
    private static String ignoredListcountryBbanStructureMethod [] = {"CR", "IR", "PK", "TN", "MU"};

    static {
        ClassLoader classLoader = RegistryTest.class.getClassLoader();
        File registryFile = new File(classLoader.getResource("iban_registry_0.txt").getFile());

        try {
            registry = new Registry(registryFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load IBAN registry file from classpath");
        }
    }

    @RunWith(Parameterized.class)
    public static class RegistryFormatSupportedByIban4j {

        private final RegistryFormat registryFormat;

        public RegistryFormatSupportedByIban4j(RegistryFormat registryFormat) {
            this.registryFormat = registryFormat;
        }

        @Parameterized.Parameters
        public static Collection<Object[]> registryFormats() {
            ArrayList<Object[]> params = new ArrayList<Object[]>();
            for (RegistryFormat registryFormat : registry.getRegistryFormats()) {
                params.add(new Object[]{registryFormat});
            }
            return params;
        }


        @Test
        public void registryFormatExample() {

            if(Arrays.asList(ignoredListRegistryFormatExampleMethod).contains(registryFormat.getCountryCode())) {
                return;
            }

            try {
                Iban.valueOf(registryFormat.getIbanElectronicFormatExample());
            } catch (UnsupportedCountryException e) {
                fail(String.format(
                    "%s (%s) country code is not supported by iban4j",
                    registryFormat.getCountryName(),
                    registryFormat.getCountryCode()
                ));
            } catch (IbanFormatException e) {
                fail(String.format(
                    "%s (%s) format is incorrect in iban4j",
                    registryFormat.getCountryName(),
                    registryFormat.getCountryCode()
                ));
            }
        }
    }

    @RunWith(Parameterized.class)
    public static class Iban4jSupportedByRegistryFormat {

        private final CountryCode countryCode;

        public Iban4jSupportedByRegistryFormat(CountryCode countryCode) {
            this.countryCode = countryCode;
        }

        @Parameterized.Parameters
        public static Collection<Object[]> iban4jCountryCodes() {
            ArrayList<Object[]> params = new ArrayList<Object[]>();
            for (CountryCode countryCode : CountryCode.values()) {
                BbanStructure bbanStructure = BbanStructure.forCountry(countryCode);
                if (bbanStructure != null) {
                    params.add(new Object[]{countryCode});
                }
            }
            return params;
        }

        @Test
        public void countryCode() {

            if(Arrays.asList(ignoredListCountryCodeMethod).contains(countryCode.getAlpha2())) {
                return;
            }

            RegistryFormat registryFormat = registry.getRegistryFormat(countryCode.getAlpha2());
            if (registryFormat == null) {
                fail(String.format(
                    "%s (%s) country code is supported by iban4j but not in the IBAN registry",
                    countryCode.getName(),
                    countryCode.getAlpha2()
                ));
            }
        }

        @Test
        public void countryBbanStructure() {

            if(Arrays.asList(ignoredListcountryBbanStructureMethod).contains(countryCode.getAlpha2())) {
                return;
            }

            RegistryFormat registryFormat = registry.getRegistryFormat(countryCode.getAlpha2());
            if (registryFormat == null) {
                fail(String.format(
                    "%s (%s) country code is supported by iban4j but not in the IBAN registry",
                    countryCode.getName(),
                    countryCode.getAlpha2()
                ));
            }

            RegistryPattern registryPattern = RegistryPattern.compile(registryFormat.getBbanStructure());

            // NOTE: because the IBANs are randomly generated these tests may be volatile
            org.iban4j.Iban iban = org.iban4j.Iban.random(countryCode);

            if (!registryPattern.getRegexPattern().matcher(iban.getBban()).matches()) {
                fail(String.format(
                    "%s (%s) random iban4j bban '%s' for country code did not match registry pattern '%s'",
                    countryCode.getName(),
                    countryCode.getAlpha2(),
                    iban.getBban(),
                    registryFormat.getBbanStructure()
                ));
            }
        }
    }
}
