package org.robolectric;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.View;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowDisplay;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowView;
import org.robolectric.shadows.StubViewRoot;
import org.robolectric.internal.Shadow;
import org.robolectric.util.TestOnClickListener;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(TestRunners.WithDefaults.class)
public class RobolectricTest {

  private PrintStream originalSystemOut;
  private ByteArrayOutputStream buff;
  private String defaultLineSeparator;

  @Before
  public void setUp() {
    originalSystemOut = System.out;
    defaultLineSeparator = System.getProperty("line.separator");

    System.setProperty("line.separator", "\n");
    buff = new ByteArrayOutputStream();
    PrintStream testOut = new PrintStream(buff);
    System.setOut(testOut);
  }

  @After
  public void tearDown() throws Exception {
    System.setProperty("line.separator", defaultLineSeparator);
    System.setOut(originalSystemOut);
  }

  @Test(expected = RuntimeException.class)
  public void clickOn_shouldThrowIfViewIsDisabled() throws Exception {
    View view = new View(RuntimeEnvironment.application);
    view.setEnabled(false);
    ShadowView.clickOn(view);
  }

  @Test
  public void shouldResetBackgroundSchedulerBeforeTests() throws Exception {
    assertThat(ShadowApplication.getInstance().getBackgroundScheduler().isPaused()).isFalse();
    ShadowApplication.getInstance().getBackgroundScheduler().pause();
  }

  @Test
  public void shouldResetBackgroundSchedulerAfterTests() throws Exception {
    assertThat(ShadowApplication.getInstance().getBackgroundScheduler().isPaused()).isFalse();
    ShadowApplication.getInstance().getBackgroundScheduler().pause();
  }

  @Test
  public void idleMainLooper_executesScheduledTasks() {
    final boolean[] wasRun = new boolean[]{false};
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        wasRun[0] = true;
      }
    }, 2000);

    assertFalse(wasRun[0]);
    ShadowLooper.idleMainLooper(1999);
    assertFalse(wasRun[0]);
    ShadowLooper.idleMainLooper(1);
    assertTrue(wasRun[0]);
  }

  @Test
  public void shouldUseSetDensityForContexts() throws Exception {
    assertThat(new Activity().getResources().getDisplayMetrics().density).isEqualTo(1.0f);
    ShadowApplication.setDisplayMetricsDensity(1.5f);
    assertThat(new Activity().getResources().getDisplayMetrics().density).isEqualTo(1.5f);
  }

  @Test
  public void shouldUseSetDisplayForContexts() throws Exception {
    assertThat(new Activity().getResources().getDisplayMetrics().widthPixels).isEqualTo(480);
    assertThat(new Activity().getResources().getDisplayMetrics().heightPixels).isEqualTo(800);

    Display display = Shadow.newInstanceOf(Display.class);
    ShadowDisplay shadowDisplay = Shadows.shadowOf(display);
    shadowDisplay.setWidth(100);
    shadowDisplay.setHeight(200);
    ShadowApplication.setDefaultDisplay(display);

    assertThat(new Activity().getResources().getDisplayMetrics().widthPixels).isEqualTo(100);
    assertThat(new Activity().getResources().getDisplayMetrics().heightPixels).isEqualTo(200);
  }

  @Test
  public void clickOn_shouldCallClickListener() throws Exception {
    View view = new View(RuntimeEnvironment.application);
    shadowOf(view).setMyParent(new StubViewRoot());
    TestOnClickListener testOnClickListener = new TestOnClickListener();
    view.setOnClickListener(testOnClickListener);
    ShadowView.clickOn(view);
    assertTrue(testOnClickListener.clicked);
  }

  @Test(expected = ActivityNotFoundException.class)
  public void checkActivities_shouldSetValueOnShadowApplication() throws Exception {
    ShadowApplication.getInstance().checkActivities(true);
    RuntimeEnvironment.application.startActivity(new Intent("i.dont.exist.activity"));
  }

  @Test
  public void setupActivity_returnsAVisibleActivity() throws Exception {
    LifeCycleActivity activity = Robolectric.setupActivity(LifeCycleActivity.class);

    assertThat(activity.isCreated()).isTrue();
    assertThat(activity.isStarted()).isTrue();
    assertThat(activity.isResumed()).isTrue();
    assertThat(activity.isVisible()).isTrue();
  }

  @Implements(View.class)
  public static class TestShadowView {
    @SuppressWarnings({"UnusedDeclaration"})
    @Implementation
    public Context getContext() {
      return null;
    }
  }

  private static class LifeCycleActivity extends Activity {
    private boolean created;
    private boolean started;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      created = true;
    }

    @Override
    protected void onStart() {
      super.onStart();
      started = true;
    }

    public boolean isStarted() {
      return started;
    }

    public boolean isCreated() {
      return created;
    }

    public boolean isVisible() {
      return getWindow().getDecorView().getWindowToken() != null;
    }
  }
}
