package com.caresync.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;

import static org.junit.jupiter.api.Assertions.*;

class AgeCalculationTest {

    private Integer calculateAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    @Test
    @DisplayName("Should return exact age when birthday has already occurred this year")
    void testAgeWhenBirthdayAlreadyPassedThisYear() {
        LocalDate today = LocalDate.now();
        // Birthday was 25 years ago, exactly 1 month before today
        LocalDate dob = today.minusYears(25).minusMonths(1);
        
        Integer age = calculateAge(dob);
        
        assertNotNull(age);
        assertEquals(25, age);
    }

    @Test
    @DisplayName("Should return age minus one when birthday has not yet occurred this year")
    void testAgeWhenBirthdayNotYetPassedThisYear() {
        LocalDate today = LocalDate.now();
        // Birthday is 25 years ago, but 1 month after today's month/day
        LocalDate dob = today.minusYears(25).plusMonths(1);
        
        Integer age = calculateAge(dob);
        
        assertNotNull(age);
        assertEquals(24, age, "Age must reflect that the birthday has not arrived yet this year");
    }

    @Test
    @DisplayName("Should return exact age when birthday is today")
    void testAgeWhenBirthdayIsToday() {
        LocalDate today = LocalDate.now();
        LocalDate dob = today.minusYears(30);
        
        Integer age = calculateAge(dob);
        
        assertNotNull(age);
        assertEquals(30, age);
    }

    @Test
    @DisplayName("Should return 0 for infant born earlier this year")
    void testAgeForInfant() {
        LocalDate today = LocalDate.now();
        LocalDate dob = today.minusMonths(3);
        
        Integer age = calculateAge(dob);
        
        assertNotNull(age);
        assertEquals(0, age);
    }

    @Test
    @DisplayName("Should return null when dateOfBirth is null")
    void testAgeWhenDobIsNull() {
        Integer age = calculateAge(null);
        assertNull(age);
    }
}
