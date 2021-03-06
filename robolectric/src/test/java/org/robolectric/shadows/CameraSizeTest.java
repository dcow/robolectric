package org.robolectric.shadows;


import android.hardware.Camera;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.TestRunners;
import org.robolectric.internal.Shadow;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(TestRunners.WithDefaults.class)
public class CameraSizeTest {

  private Camera.Size cameraSize;

  @Before
  public void setUp() throws Exception {
    cameraSize = Shadow.newInstanceOf(Camera.class).new Size(480, 320);
  }

  @Test
  public void testConstructor() throws Exception {
    assertThat(cameraSize.width).isEqualTo(480);
    assertThat(cameraSize.height).isEqualTo(320);
  }

  @Test
  public void testSetWidth() throws Exception {
    assertThat(cameraSize.width).isNotEqualTo(640);
    cameraSize.width = 640;
    assertThat(cameraSize.width).isEqualTo(640);
  }

  @Test
  public void testSetHeight() throws Exception {
    assertThat(cameraSize.height).isNotEqualTo(480);
    cameraSize.height = 480;
    assertThat(cameraSize.height).isEqualTo(480);
  }

}
