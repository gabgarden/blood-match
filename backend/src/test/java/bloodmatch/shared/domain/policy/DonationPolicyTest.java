package bloodmatch.shared.domain.policy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DonationPolicyTest {

  @AfterEach
  void resetInterval() {
    DonationPolicy.setDonationIntervalInMonths(3);
  }

  @Test
  void testIntervalGetAndSet() {
    new DonationPolicy();
    assertEquals(3, DonationPolicy.getDonationIntervalInMonths());

    DonationPolicy.setDonationIntervalInMonths(4);
    assertEquals(4, DonationPolicy.getDonationIntervalInMonths());

    assertThrows(IllegalArgumentException.class, () -> DonationPolicy.setDonationIntervalInMonths(0));
    assertThrows(IllegalArgumentException.class, () -> DonationPolicy.setDonationIntervalInMonths(-1));
  }
}
