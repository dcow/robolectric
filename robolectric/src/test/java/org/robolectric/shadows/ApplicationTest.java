package org.robolectric.shadows;

import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import org.robolectric.fakes.RoboSensorManager;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.fakes.RoboVibrator;
import org.robolectric.manifest.AndroidManifest;
import org.robolectric.res.EmptyResourceLoader;
import org.robolectric.res.Fs;
import org.robolectric.res.ResName;
import org.robolectric.res.ResType;
import org.robolectric.res.ResourceExtractor;
import org.robolectric.res.ResourceIndex;
import org.robolectric.res.ResourceLoader;
import org.robolectric.res.TypedResource;
import org.robolectric.test.TemporaryFolder;
import org.robolectric.util.TestBroadcastReceiver;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunners.WithDefaults.class)
public class ApplicationTest {
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Test
  public void shouldBeAContext() throws Exception {
    assertThat(new Activity().getApplication()).isSameAs(RuntimeEnvironment.application);
    assertThat(new Activity().getApplication().getApplicationContext()).isSameAs(RuntimeEnvironment.application);
  }

  @Test
  public void shouldBeBindableToAResourceLoader() throws Exception {
    ResourceLoader resourceLoader1 = new EmptyResourceLoader() {
      @Override public TypedResource getValue(ResName resName, String qualifiers) {
        return new TypedResource("title from resourceLoader1", ResType.CHAR_SEQUENCE);
      }

      @Override public ResourceIndex getResourceIndex() {
        return new ImperviousResourceExtractor();
      }
    };
    ResourceLoader resourceLoader2 = new EmptyResourceLoader() {
      @Override public TypedResource getValue(ResName resName, String qualifiers) {
        return new TypedResource("title from resourceLoader2", ResType.CHAR_SEQUENCE);
      }

      @Override public ResourceIndex getResourceIndex() {
        return new ImperviousResourceExtractor();
      }
    };

    final Application app1 = new Application();
    final Application app2 = new Application();
    shadowOf(app1).bind(null, resourceLoader1);
    shadowOf(app2).bind(null, resourceLoader2);

    assertEquals("title from resourceLoader1", new ContextWrapper(app1).getResources().getString(R.string.howdy));
    assertEquals("title from resourceLoader2", new ContextWrapper(app2).getResources().getString(R.string.howdy));
  }

  @Test
  public void shouldProvideServices() throws Exception {
    checkSystemService(Context.ACTIVITY_SERVICE, android.app.ActivityManager.class);
    checkSystemService(Context.POWER_SERVICE, android.os.PowerManager.class);
    checkSystemService(Context.ALARM_SERVICE, android.app.AlarmManager.class);
    checkSystemService(Context.NOTIFICATION_SERVICE, android.app.NotificationManager.class);
    checkSystemService(Context.KEYGUARD_SERVICE, android.app.KeyguardManager.class);
    checkSystemService(Context.LOCATION_SERVICE, android.location.LocationManager.class);
    checkSystemService(Context.SEARCH_SERVICE, android.app.SearchManager.class);
    checkSystemService(Context.SENSOR_SERVICE, RoboSensorManager.class);
    checkSystemService(Context.STORAGE_SERVICE, android.os.storage.StorageManager.class);
    checkSystemService(Context.VIBRATOR_SERVICE, RoboVibrator.class);
    checkSystemService(Context.CONNECTIVITY_SERVICE, android.net.ConnectivityManager.class);
    checkSystemService(Context.WIFI_SERVICE, android.net.wifi.WifiManager.class);
    checkSystemService(Context.AUDIO_SERVICE, android.media.AudioManager.class);
    checkSystemService(Context.TELEPHONY_SERVICE, android.telephony.TelephonyManager.class);
    checkSystemService(Context.INPUT_METHOD_SERVICE, android.view.inputmethod.InputMethodManager.class);
    checkSystemService(Context.UI_MODE_SERVICE, android.app.UiModeManager.class);
    checkSystemService(Context.DOWNLOAD_SERVICE, android.app.DownloadManager.class);
    checkSystemService(Context.DEVICE_POLICY_SERVICE, android.app.admin.DevicePolicyManager.class);
    checkSystemService(Context.DROPBOX_SERVICE, android.os.DropBoxManager.class);
    checkSystemService(Context.MEDIA_ROUTER_SERVICE, android.media.MediaRouter.class);
    checkSystemService(Context.DISPLAY_SERVICE, android.hardware.display.DisplayManager.class);
    checkSystemService(Context.ACCESSIBILITY_SERVICE, android.view.accessibility.AccessibilityManager.class);
  }

  @Test public void shouldProvideLayoutInflater() throws Exception {
    Object systemService = RuntimeEnvironment.application.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    assertThat(systemService).isInstanceOf(RoboLayoutInflater.class);
  }

  private void checkSystemService(String name, Class expectedClass) {
    Object systemService = RuntimeEnvironment.application.getSystemService(name);
    assertThat(systemService).isInstanceOf(expectedClass);
    assertThat(systemService).isSameAs(RuntimeEnvironment.application.getSystemService(name));
  }

  @Test
  public void packageManager_shouldKnowPackageName() throws Exception {
    assertThat(RuntimeEnvironment.application.getPackageManager().getApplicationInfo("org.robolectric", 0).packageName)
        .isEqualTo("org.robolectric");
  }

  @Test
  public void packageManager_shouldKnowApplicationName() throws Exception {
    assertThat(RuntimeEnvironment.application.getPackageManager().getApplicationInfo("org.robolectric", 0).name)
        .isEqualTo("org.robolectric.TestApplication");
  }

  @Test
  public void bindServiceShouldCallOnServiceConnectedWhenNotPaused() {
    ShadowLooper.pauseMainLooper();
    ComponentName expectedComponentName = new ComponentName("", "");
    NullBinder expectedBinder = new NullBinder();
    Shadows.shadowOf(RuntimeEnvironment.application).setComponentNameAndServiceForBindService(expectedComponentName, expectedBinder);

    TestService service = new TestService();
    assertTrue(RuntimeEnvironment.application.bindService(new Intent(""), service, Context.BIND_AUTO_CREATE));

    assertNull(service.name);
    assertNull(service.service);

    ShadowLooper.unPauseMainLooper();

    assertEquals(expectedComponentName, service.name);
    assertEquals(expectedBinder, service.service);
  }

  @Test
  public void unbindServiceShouldCallOnServiceDisconnectedWhenNotPaused() {
    TestService service = new TestService();
    ComponentName expectedComponentName = new ComponentName("", "");
    NullBinder expectedBinder = new NullBinder();
    Shadows.shadowOf(RuntimeEnvironment.application).setComponentNameAndServiceForBindService(expectedComponentName, expectedBinder);
    RuntimeEnvironment.application.bindService(new Intent(""), service, Context.BIND_AUTO_CREATE);
    ShadowLooper.pauseMainLooper();

    RuntimeEnvironment.application.unbindService(service);
    assertNull(service.nameUnbound);
    ShadowLooper.unPauseMainLooper();
    assertEquals(expectedComponentName, service.nameUnbound);
  }

  @Test
  public void unbindServiceAddsEntryToUnboundServicesCollection() {
    TestService service = new TestService();
    ComponentName expectedComponentName = new ComponentName("", "");
    NullBinder expectedBinder = new NullBinder();
    final ShadowApplication shadowApplication = Shadows.shadowOf(RuntimeEnvironment.application);
    shadowApplication.setComponentNameAndServiceForBindService(expectedComponentName, expectedBinder);
    RuntimeEnvironment.application.bindService(new Intent(""), service, Context.BIND_AUTO_CREATE);
    RuntimeEnvironment.application.unbindService(service);
    assertEquals(1, shadowApplication.getUnboundServiceConnections().size());
    assertEquals(service, shadowApplication.getUnboundServiceConnections().get(0));
  }

  @Test
  public void declaringServiceUnbindableMakesBindServiceReturnFalse() {
    ShadowLooper.pauseMainLooper();
    TestService service = new TestService();
    ComponentName expectedComponentName = new ComponentName("", "");
    NullBinder expectedBinder = new NullBinder();
    final ShadowApplication shadowApplication = Shadows.shadowOf(RuntimeEnvironment.application);
    shadowApplication.setComponentNameAndServiceForBindService(expectedComponentName, expectedBinder);
    shadowApplication.declareActionUnbindable("refuseToBind");
    assertFalse(RuntimeEnvironment.application.bindService(new Intent("refuseToBind"), service, Context.BIND_AUTO_CREATE));
    ShadowLooper.unPauseMainLooper();
    assertNull(service.name);
    assertNull(service.service);
    assertNull(shadowApplication.peekNextStartedService());
  }

  @Test
  public void shouldHaveStoppedServiceIntentAndIndicateServiceWasntRunning() {
    ShadowApplication shadowApplication = Shadows.shadowOf(RuntimeEnvironment.application);

    Activity activity = new Activity();

    Intent intent = getSomeActionIntent("some.action");

    boolean wasRunning = activity.stopService(intent);

    assertFalse(wasRunning);
    assertEquals(intent, shadowApplication.getNextStoppedService());
  }

  private Intent getSomeActionIntent(String action) {
    Intent intent = new Intent();
    intent.setAction(action);
    return intent;
  }

  @Test
  public void shouldHaveStoppedServiceIntentAndIndicateServiceWasRunning() {
    ShadowApplication shadowApplication = shadowOf(RuntimeEnvironment.application);

    Activity activity = new Activity();

    Intent intent = getSomeActionIntent("some.action");

    activity.startService(intent);

    boolean wasRunning = activity.stopService(intent);

    assertTrue(wasRunning);
    assertEquals(intent, shadowApplication.getNextStoppedService());
  }

  @Test
  public void shouldHaveStoppedServiceByStartedComponent() {
    ShadowApplication shadowApplication = shadowOf(RuntimeEnvironment.application);

    Activity activity = new Activity();

    ComponentName componentName = new ComponentName("package.test", "package.test.TestClass");
    Intent startServiceIntent = new Intent().setComponent(componentName);

    ComponentName startedComponent = activity.startService(startServiceIntent);
    assertThat(startedComponent.getPackageName()).isEqualTo("package.test");
    assertThat(startedComponent.getClassName()).isEqualTo("package.test.TestClass");

    Intent stopServiceIntent = new Intent().setComponent(startedComponent);
    boolean wasRunning = activity.stopService(stopServiceIntent);

    assertTrue(wasRunning);
    assertThat(shadowApplication.getNextStoppedService()).isEqualTo(startServiceIntent);
  }

  @Test
  public void shouldClearStartedServiceIntents() {
    ShadowApplication shadowApplication = shadowOf(RuntimeEnvironment.application);
    shadowApplication.startService(getSomeActionIntent("some.action"));
    shadowApplication.startService(getSomeActionIntent("another.action"));

    shadowApplication.clearStartedServices();

    assertNull(shadowApplication.getNextStartedService());
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowIfContainsRegisteredReceiverOfAction() {
    Activity activity = new Activity();
    activity.registerReceiver(new TestBroadcastReceiver(), new IntentFilter("Foo"));

    shadowOf(RuntimeEnvironment.application).assertNoBroadcastListenersOfActionRegistered(activity, "Foo");
  }

  @Test
  public void shouldNotThrowIfDoesNotContainsRegisteredReceiverOfAction() {
    Activity activity = new Activity();
    activity.registerReceiver(new TestBroadcastReceiver(), new IntentFilter("Foo"));

    shadowOf(RuntimeEnvironment.application).assertNoBroadcastListenersOfActionRegistered(activity, "Bar");
  }

  @Test
  public void canAnswerIfReceiverIsRegisteredForIntent() throws Exception {
    BroadcastReceiver expectedReceiver = new TestBroadcastReceiver();
    ShadowApplication shadowApplication = shadowOf(RuntimeEnvironment.application);
    assertFalse(shadowApplication.hasReceiverForIntent(new Intent("Foo")));
    RuntimeEnvironment.application.registerReceiver(expectedReceiver, new IntentFilter("Foo"));

    assertTrue(shadowApplication.hasReceiverForIntent(new Intent("Foo")));
  }

  @Test
  public void canFindAllReceiversForAnIntent() throws Exception {
    BroadcastReceiver expectedReceiver = new TestBroadcastReceiver();
    ShadowApplication shadowApplication = shadowOf(RuntimeEnvironment.application);
    assertFalse(shadowApplication.hasReceiverForIntent(new Intent("Foo")));
    RuntimeEnvironment.application.registerReceiver(expectedReceiver, new IntentFilter("Foo"));
    RuntimeEnvironment.application.registerReceiver(expectedReceiver, new IntentFilter("Foo"));

    assertTrue(shadowApplication.getReceiversForIntent(new Intent("Foo")).size() == 2);
  }

  @Test
  public void broadcasts_shouldBeLogged() {
    Intent broadcastIntent = new Intent("foo");
    RuntimeEnvironment.application.sendBroadcast(broadcastIntent);

    List<Intent> broadcastIntents = shadowOf(RuntimeEnvironment.application).getBroadcastIntents();
    assertTrue(broadcastIntents.size() == 1);
    assertEquals(broadcastIntent, broadcastIntents.get(0));
  }

  private static class NullBinder implements IBinder {
    @Override
    public String getInterfaceDescriptor() throws RemoteException {
      return null;
    }

    @Override
    public boolean pingBinder() {
      return false;
    }

    @Override
    public boolean isBinderAlive() {
      return false;
    }

    @Override
    public IInterface queryLocalInterface(String descriptor) {
      return null;
    }

    @Override
    public void dump(FileDescriptor fd, String[] args) throws RemoteException {
    }

    @Override
    public void dumpAsync(FileDescriptor fileDescriptor, String[] strings) throws RemoteException {
    }

    @Override
    public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
      return false;
    }

    @Override
    public void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException {
    }

    @Override
    public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
      return false;
    }
  }

  @Test
  public void shouldRememberResourcesAfterLazilyLoading() throws Exception {
    Application application = new DefaultTestLifecycle().createApplication(null, newConfigWith("com.wacka.wa", ""), null);
    assertSame(application.getResources(), application.getResources());
  }

  @Test
  public void shouldBeAbleToResetResources() throws Exception {
    Application application = new DefaultTestLifecycle().createApplication(null,
        newConfigWith("com.wacka.wa", ""), null);
    Resources res = application.getResources();
    shadowOf(application).resetResources();
    assertFalse(res == application.getResources());
  }

  @Test
  public void checkPermission_shouldTrackGrantedAndDeniedPermissions() throws Exception {
    Application application = new DefaultTestLifecycle().createApplication(null,
        newConfigWith("com.wacka.wa", ""), null);
    shadowOf(application).grantPermissions("foo", "bar");
    shadowOf(application).denyPermissions("foo", "qux");
    assertThat(application.checkPermission("foo", -1, -1)).isEqualTo(PERMISSION_DENIED);
    assertThat(application.checkPermission("bar", -1, -1)).isEqualTo(PERMISSION_GRANTED);
    assertThat(application.checkPermission("baz", -1, -1)).isEqualTo(PERMISSION_DENIED);
    assertThat(application.checkPermission("qux", -1, -1)).isEqualTo(PERMISSION_DENIED);
  }

  @Test
  public void startActivity_whenActivityCheckingEnabled_checksPackageManagerResolveInfo() throws Exception {
    Application application = new DefaultTestLifecycle().createApplication(null,
        newConfigWith("com.wacka.wa", ""), null);
    shadowOf(application).checkActivities(true);

    String action = "com.does.not.exist.android.app.v2.mobile";

    try {
      application.startActivity(new Intent(action));
      fail("Expected startActivity to throw ActivityNotFoundException!");
    } catch (ActivityNotFoundException e) {
      assertThat(e.getMessage()).contains(action);
      assertThat(shadowOf(application).getNextStartedActivity()).isNull();
    }
  }
  /////////////////////////////

  public AndroidManifest newConfigWith(String contents) throws IOException {
    return newConfigWith("org.robolectric", contents);
  }

  private AndroidManifest newConfigWith(String packageName, String contents) throws IOException {
    File f = temporaryFolder.newFile("whatever.xml",
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
            "          package=\"" + packageName + "\">\n" +
            "    " + contents + "\n" +
            "<application/>\n" +
            "</manifest>\n");
    return new AndroidManifest(Fs.newFile(f), null, null);
  }

  private static class ImperviousResourceExtractor extends ResourceExtractor {
    @Override
    public ResName getResName(int resourceId) {
      return new ResName("", "", "");
    }
  }
}
