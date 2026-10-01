package org.jlab.adm.presentation.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FunctionsTest {

  @Test
  void addS() {
    assertEquals("s", Functions.addS(0));
    assertEquals("", Functions.addS(1));
    assertEquals("s", Functions.addS(2));
  }

  @Test
  void millisToHumanReadable() {
    assertEquals("0 seconds", Functions.millisToHumanReadable(999, false));
    assertEquals("1 second", Functions.millisToHumanReadable(1_000, false));
    assertEquals("59 seconds", Functions.millisToHumanReadable(59_999, false));
    assertEquals("1 minute", Functions.millisToHumanReadable(60_000, false));
    assertEquals("2 minutes", Functions.millisToHumanReadable(179_999, false));
    assertEquals("1 hour 0 minutes", Functions.millisToHumanReadable(3_600_000, false));
    assertEquals("2 hours 3 minutes", Functions.millisToHumanReadable(7_380_000, false));
  }

  @Test
  void millisToHumanReadableStacked() {
    assertEquals("1 hour \n1 minute", Functions.millisToHumanReadable(3_660_000, true));
    assertEquals("59 minutes", Functions.millisToHumanReadable(3_599_999, true));
  }

  @Test
  void millisToAbbreviatedHumanReadable() {
    assertEquals("0m", Functions.millisToAbbreviatedHumanReadable(59_999));
    assertEquals("59m", Functions.millisToAbbreviatedHumanReadable(3_599_999));
    assertEquals("1h 1m", Functions.millisToAbbreviatedHumanReadable(3_660_000));
  }
}
