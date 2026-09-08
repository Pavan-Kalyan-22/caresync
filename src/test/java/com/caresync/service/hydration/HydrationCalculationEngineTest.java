package com.caresync.service.hydration;

import com.caresync.dto.hydration.HydrationResponse;
import com.caresync.dto.weather.WeatherResponse;
import com.caresync.entity.User;
import com.caresync.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class HydrationCalculationEngineTest {

    private HydrationCalculationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new HydrationCalculationEngine();
    }

    private User createSampleUser(Double weight, String occupation) {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .fullName("John Doe")
                .weight(weight)
                .occupation(occupation)
                .age(30)
                .build();
    }

    private WeatherResponse createSampleWeather(Double temp, Integer humidity) {
        return WeatherResponse.builder()
                .location("Bengaluru")
                .country("IN")
                .temperature(temp)
                .humidity(humidity)
                .weatherCondition("Clear")
                .build();
    }

    @Test
    @DisplayName("Scenario 1: Normal user (70 kg, sedentary) + normal weather (22°C, 50% hum) returns exactly 2450 ml (2.45 L)")
    void testCalculate_NormalUser_NormalWeather() {
        User user = createSampleUser(70.0, "Software Engineer");
        WeatherResponse weather = createSampleWeather(22.0, 50);

        HydrationResponse response = engine.calculate(user, weather);

        assertNotNull(response);
        assertEquals(2450, response.getDailyWaterTargetMl());
        assertEquals(2.45, response.getDailyWaterTargetLitres());
        assertEquals(2450, response.getBaseRequirementMl());
        assertEquals(0, response.getActivityAdjustmentMl());
        assertEquals(0, response.getTemperatureAdjustmentMl());
        assertEquals(0, response.getHumidityAdjustmentMl());
        assertEquals(70.0, response.getWeightKg());
        assertEquals("SEDENTARY", response.getActivityLevel());
        assertNotNull(response.getExplanation());
        assertTrue(response.getExplanation().contains("Base requirement of 2450 ml"));
    }

    @ParameterizedTest(name = "Weight {0} kg should produce base requirement {1} ml")
    @CsvSource({
            "50.0, 1750, 1.75",
            "60.0, 2100, 2.10",
            "70.0, 2450, 2.45",
            "80.0, 2800, 2.80",
            "90.0, 3150, 3.15",
            "100.0, 3500, 3.50"
    })
    @DisplayName("Scenario 2: Different body weights calculate base requirement accurately at 35 ml/kg")
    void testCalculate_DifferentWeights(double weight, int expectedMl, double expectedLitres) {
        User user = createSampleUser(weight, "Desk Worker");
        WeatherResponse weather = createSampleWeather(20.0, 50);

        HydrationResponse response = engine.calculate(user, weather);

        assertEquals(expectedMl, response.getDailyWaterTargetMl());
        assertEquals(expectedLitres, response.getDailyWaterTargetLitres());
        assertEquals(expectedMl, response.getBaseRequirementMl());
    }

    @ParameterizedTest(name = "Approved Occupation \"{0}\" -> Activity \"{1}\" (+{2} ml)")
    @CsvSource({
            // Approved SEDENTARY (+0 ml)
            "Desk jobs, SEDENTARY, 0",
            "Software Engineer, SEDENTARY, 0",
            "Developer, SEDENTARY, 0",
            "Student, SEDENTARY, 0",
            "Accountant, SEDENTARY, 0",
            "Driver, SEDENTARY, 0",
            "Office Worker, SEDENTARY, 0",
            "Manager, SEDENTARY, 0",
            "Clerk, SEDENTARY, 0",

            // Approved LIGHTLY_ACTIVE (+300 ml)
            "Teacher, LIGHTLY_ACTIVE, 300",
            "Doctor, LIGHTLY_ACTIVE, 300",
            "Nurse, LIGHTLY_ACTIVE, 300",
            "Retail, LIGHTLY_ACTIVE, 300",
            "Sales, LIGHTLY_ACTIVE, 300",
            "Chef, LIGHTLY_ACTIVE, 300",
            "Pharmacist, LIGHTLY_ACTIVE, 300",

            // Approved MODERATELY_ACTIVE (+600 ml)
            "Field worker, MODERATELY_ACTIVE, 600",
            "Delivery, MODERATELY_ACTIVE, 600",
            "Farmer, MODERATELY_ACTIVE, 600",
            "Factory Worker, MODERATELY_ACTIVE, 600",
            "Police, MODERATELY_ACTIVE, 600",
            "Fitness Trainer, MODERATELY_ACTIVE, 600",

            // Approved VERY_ACTIVE (+900 ml)
            "Construction worker, VERY_ACTIVE, 900",
            "Athlete, VERY_ACTIVE, 900",
            "Military, VERY_ACTIVE, 900",
            "Miner, VERY_ACTIVE, 900",
            "Manual Laborer, VERY_ACTIVE, 900"
    })
    @DisplayName("Scenario 3: Every approved occupation maps strictly to the correct activity tier and adjustment")
    void testCalculate_AllApprovedOccupations(String occupation, String expectedTier, int expectedAdjustment) {
        User user = createSampleUser(70.0, occupation);
        WeatherResponse weather = createSampleWeather(20.0, 50);

        HydrationResponse response = engine.calculate(user, weather);

        assertEquals(expectedTier, response.getActivityLevel());
        assertEquals(expectedAdjustment, response.getActivityAdjustmentMl());
        assertEquals(2450 + expectedAdjustment, response.getDailyWaterTargetMl());
    }

    @ParameterizedTest(name = "Case variation \"{0}\" -> Activity \"{1}\"")
    @CsvSource({
            "software engineer, SEDENTARY, 0",
            "SOFTWARE ENGINEER, SEDENTARY, 0",
            "sOfTwArE eNgInEeR, SEDENTARY, 0",
            "teacher, LIGHTLY_ACTIVE, 300",
            "TEACHER, LIGHTLY_ACTIVE, 300",
            "delivery, MODERATELY_ACTIVE, 600",
            "DELIVERY, MODERATELY_ACTIVE, 600",
            "construction worker, VERY_ACTIVE, 900",
            "CONSTRUCTION WORKER, VERY_ACTIVE, 900"
    })
    @DisplayName("Scenario 3b: Occupation matching is strictly case-insensitive")
    void testCalculate_CaseInsensitiveOccupationMatching(String occupation, String expectedTier, int expectedAdjustment) {
        User user = createSampleUser(70.0, occupation);
        WeatherResponse weather = createSampleWeather(20.0, 50);

        HydrationResponse response = engine.calculate(user, weather);

        assertEquals(expectedTier, response.getActivityLevel());
        assertEquals(expectedAdjustment, response.getActivityAdjustmentMl());
    }

    @ParameterizedTest(name = "Removed synonym \"{0}\" must safely default to SEDENTARY (+0 ml)")
    @CsvSource({
            "mining",
            "heavy labor",
            "farming",
            "carpenter",
            "plumber",
            "electrician",
            "mechanic",
            "warehouse",
            "healthcare",
            "cook",
            "waiter",
            "waitress",
            "bartender",
            "hospitality"
    })
    @DisplayName("Scenario 3c: Removed synonyms default to SEDENTARY (+0 ml)")
    void testCalculate_RemovedSynonyms_DefaultToSedentary(String removedSynonym) {
        User user = createSampleUser(70.0, removedSynonym);
        WeatherResponse weather = createSampleWeather(20.0, 50);

        HydrationResponse response = engine.calculate(user, weather);

        assertEquals("SEDENTARY", response.getActivityLevel());
        assertEquals(0, response.getActivityAdjustmentMl());
        assertEquals(2450, response.getDailyWaterTargetMl());
    }

    @Test
    @DisplayName("Scenario 4: Null, blank, or unrecognized occupation safely defaults to SEDENTARY (+0 ml)")
    void testCalculate_UnrecognizedOccupation_DefaultsToSedentary() {
        User userNull = createSampleUser(70.0, null);
        User userBlank = createSampleUser(70.0, "   ");
        User userUnknown = createSampleUser(70.0, "Astronaut");
        WeatherResponse weather = createSampleWeather(20.0, 50);

        assertEquals("SEDENTARY", engine.calculate(userNull, weather).getActivityLevel());
        assertEquals(0, engine.calculate(userNull, weather).getActivityAdjustmentMl());

        assertEquals("SEDENTARY", engine.calculate(userBlank, weather).getActivityLevel());
        assertEquals(0, engine.calculate(userBlank, weather).getActivityAdjustmentMl());

        assertEquals("SEDENTARY", engine.calculate(userUnknown, weather).getActivityLevel());
        assertEquals(0, engine.calculate(userUnknown, weather).getActivityAdjustmentMl());
    }

    @ParameterizedTest(name = "Temperature {0}°C -> Adjustment +{1} ml")
    @CsvSource({
            "10.0, 0",
            "20.0, 0",
            "24.9, 0",
            "25.0, 250",
            "29.9, 250",
            "30.0, 500",
            "34.9, 500",
            "35.0, 750",
            "42.0, 750"
    })
    @DisplayName("Scenario 5: Temperature thresholds correctly apply tiered adjustments")
    void testCalculate_TemperatureThresholds(double temp, int expectedAdjustment) {
        User user = createSampleUser(70.0, "Engineer");
        WeatherResponse weather = createSampleWeather(temp, 50);

        HydrationResponse response = engine.calculate(user, weather);

        assertEquals(expectedAdjustment, response.getTemperatureAdjustmentMl());
        assertEquals(2450 + expectedAdjustment, response.getDailyWaterTargetMl());
    }

    @Test
    @DisplayName("Scenario 6: Hot and humid weather (>=28°C and >=70% hum) applies both temperature and humidity adjustments")
    void testCalculate_HotAndHumid() {
        User user = createSampleUser(70.0, "Engineer");
        WeatherResponse weather = createSampleWeather(32.0, 75); // Hot (+500), Humid (+200)

        HydrationResponse response = engine.calculate(user, weather);

        assertEquals(500, response.getTemperatureAdjustmentMl());
        assertEquals(200, response.getHumidityAdjustmentMl());
        assertEquals(2450 + 500 + 200, response.getDailyWaterTargetMl()); // 3150 ml
        assertEquals(3.15, response.getDailyWaterTargetLitres());
    }

    @Test
    @DisplayName("Scenario 7: Dry heat weather (>=25°C and <=30% hum) applies dry heat humidity adjustment (+150 ml)")
    void testCalculate_DryHeat() {
        User user = createSampleUser(70.0, "Engineer");
        WeatherResponse weather = createSampleWeather(30.0, 25); // Hot (+500), Dry (+150)

        HydrationResponse response = engine.calculate(user, weather);

        assertEquals(500, response.getTemperatureAdjustmentMl());
        assertEquals(150, response.getHumidityAdjustmentMl());
        assertEquals(2450 + 500 + 150, response.getDailyWaterTargetMl()); // 3100 ml
        assertEquals(3.10, response.getDailyWaterTargetLitres());
    }

    @Test
    @DisplayName("Scenario 8: Normal humidity (40-60%) applies 0 ml humidity adjustment")
    void testCalculate_NormalHumidity_NoAdjustment() {
        User user = createSampleUser(70.0, "Engineer");
        WeatherResponse weather = createSampleWeather(30.0, 50);

        HydrationResponse response = engine.calculate(user, weather);

        assertEquals(500, response.getTemperatureAdjustmentMl());
        assertEquals(0, response.getHumidityAdjustmentMl());
        assertEquals(2950, response.getDailyWaterTargetMl());
    }

    @Test
    @DisplayName("Scenario 9: Low weight produces total below 1500 ml -> clamped to application minimum limit (1500 ml)")
    void testCalculate_MinimumBoundaryClamping() {
        User user = createSampleUser(30.0, "Student"); // 30 * 35 = 1050 ml
        WeatherResponse weather = createSampleWeather(18.0, 50);

        HydrationResponse response = engine.calculate(user, weather);

        assertEquals(1050, response.getBaseRequirementMl());
        assertEquals(1500, response.getDailyWaterTargetMl());
        assertEquals(1.50, response.getDailyWaterTargetLitres());
        assertTrue(response.getExplanation().contains("Target clamped to application minimum limit of 1500 ml"));
    }

    @Test
    @DisplayName("Scenario 10: High weight + extreme activity + extreme weather exceeds 4500 ml -> clamped to application maximum limit (4500 ml)")
    void testCalculate_MaximumBoundaryClamping() {
        User user = createSampleUser(110.0, "Construction Worker"); // Base: 110*35 = 3850 ml, Activity: +900 ml
        WeatherResponse weather = createSampleWeather(38.0, 80); // Temp: +750 ml, Humidity: +200 ml
        // Raw total = 3850 + 900 + 750 + 200 = 5700 ml

        HydrationResponse response = engine.calculate(user, weather);

        assertEquals(3850, response.getBaseRequirementMl());
        assertEquals(900, response.getActivityAdjustmentMl());
        assertEquals(750, response.getTemperatureAdjustmentMl());
        assertEquals(200, response.getHumidityAdjustmentMl());
        assertEquals(4500, response.getDailyWaterTargetMl());
        assertEquals(4.50, response.getDailyWaterTargetLitres());
        assertTrue(response.getExplanation().contains("Target clamped to application maximum limit of 4500 ml"));
    }

    @Test
    @DisplayName("Scenario 11: Weight below 20.0 kg throws BadRequestException")
    void testCalculate_InvalidWeight_TooLow() {
        User user = createSampleUser(19.9, "Student");
        WeatherResponse weather = createSampleWeather(22.0, 50);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> engine.calculate(user, weather));
        assertTrue(ex.getMessage().contains("Weight must be between 20.0 kg and 300.0 kg"));
    }

    @Test
    @DisplayName("Scenario 12: Weight above 300.0 kg throws BadRequestException")
    void testCalculate_InvalidWeight_TooHigh() {
        User user = createSampleUser(301.0, "Student");
        WeatherResponse weather = createSampleWeather(22.0, 50);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> engine.calculate(user, weather));
        assertTrue(ex.getMessage().contains("Weight must be between 20.0 kg and 300.0 kg"));
    }

    @Test
    @DisplayName("Scenario 13: Null weight throws BadRequestException")
    void testCalculate_MissingWeight() {
        User user = createSampleUser(null, "Student");
        WeatherResponse weather = createSampleWeather(22.0, 50);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> engine.calculate(user, weather));
        assertTrue(ex.getMessage().contains("weight is required for hydration calculation"));
    }

    @Test
    @DisplayName("Scenario 14: Null weather or null weather metrics throw BadRequestException")
    void testCalculate_MissingWeatherMetrics() {
        User user = createSampleUser(70.0, "Student");

        assertThrows(BadRequestException.class, () -> engine.calculate(user, null));

        WeatherResponse nullTemp = createSampleWeather(null, 50);
        assertThrows(BadRequestException.class, () -> engine.calculate(user, nullTemp));

        WeatherResponse nullHum = createSampleWeather(25.0, null);
        assertThrows(BadRequestException.class, () -> engine.calculate(user, nullHum));
    }

    @Test
    @DisplayName("Scenario 15: Out of range weather metrics throw BadRequestException")
    void testCalculate_OutOfRangeWeatherMetrics() {
        User user = createSampleUser(70.0, "Student");

        WeatherResponse extremeTemp = createSampleWeather(65.0, 50);
        assertThrows(BadRequestException.class, () -> engine.calculate(user, extremeTemp));

        WeatherResponse invalidHum = createSampleWeather(25.0, 105);
        assertThrows(BadRequestException.class, () -> engine.calculate(user, invalidHum));
    }
}
