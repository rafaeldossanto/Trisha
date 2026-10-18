package com.trisha.app.point;

import com.trisha.app.stub.PointOfInterestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PointOfInterest")
class PointOfInterestTest {

    private PointOfInterest withDescription(String description) {
        return PointOfInterestStub.aPoint().description(description).build();
    }

    @Test
    @DisplayName("confidenceLevel 1: without description, no matter how many validators")
    void levelOneWithoutDescription() {
        assertThat(withDescription(null).confidenceLevel(0)).isEqualTo(1);
        assertThat(withDescription(null).confidenceLevel(5)).isEqualTo(1);
    }

    @Test
    @DisplayName("confidenceLevel 1: a blank description counts as no description")
    void levelOneWithBlankDescription() {
        assertThat(withDescription("   ").confidenceLevel(3)).isEqualTo(1);
    }

    @Test
    @DisplayName("confidenceLevel 2: description without validators")
    void levelTwoWithDescriptionOnly() {
        assertThat(withDescription("Cachoeira").confidenceLevel(0)).isEqualTo(2);
    }

    @Test
    @DisplayName("confidenceLevel 3: description and one validator")
    void levelThreeWithOneValidator() {
        assertThat(withDescription("Cachoeira").confidenceLevel(1)).isEqualTo(3);
    }

    @Test
    @DisplayName("confidenceLevel 4: description and two validators")
    void levelFourWithTwoValidators() {
        assertThat(withDescription("Cachoeira").confidenceLevel(2)).isEqualTo(4);
    }

    @Test
    @DisplayName("confidenceLevel 5: description and three or more validators")
    void levelFiveWithThreeOrMoreValidators() {
        assertThat(withDescription("Cachoeira").confidenceLevel(3)).isEqualTo(5);
        assertThat(withDescription("Cachoeira").confidenceLevel(10)).isEqualTo(5);
    }
}
