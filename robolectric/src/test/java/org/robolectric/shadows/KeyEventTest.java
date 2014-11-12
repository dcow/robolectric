package org.robolectric.shadows;

import android.view.InputDevice;
import android.view.KeyEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.TestRunners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.robolectric.RobolectricBase.shadowOf;

@RunWith(TestRunners.WithDefaults.class)
public class KeyEventTest {
  @Test
  public void canSetInputDeviceOnKeyEvent() throws Exception {
    InputDevice myDevice = ShadowInputDevice.makeInputDeviceNamed("myDevice");
    KeyEvent keyEvent = new KeyEvent(1, 2);
    shadowOf(keyEvent).setDevice(myDevice);
    assertThat(keyEvent.getDevice().getName()).isEqualTo("myDevice");
  }
}
