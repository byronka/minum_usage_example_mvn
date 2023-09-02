package com.renomad;

import com.renomad.sampledomain.ListPhotosTests;
import com.renomad.minum.Constants;
import com.renomad.minum.Context;
import com.renomad.minum.logging.TestLogger;
import com.renomad.minum.utils.*;
import com.renomad.minum.web.*;

import java.io.IOException;
import java.nio.file.Path;

import static com.renomad.minum.testing.TestFramework.buildTestingContext;

public class Tests {


  public static void main(String[] args) {
    var tests = new Tests();
    tests.testEverything();
  }

  public void testEverything() {
    try {
      unitAndIntegrationTests();
      testFullSystem_Soup_To_Nuts();
      indicateTestsFinished();
    } catch (Exception ex) {
      String exceptionString = StacktraceUtils.stackTraceToString(ex);
      System.out.println(exceptionString);
    }
  }

  private final Constants constants;

  public Tests() {
    constants = new Constants();
  }

  private void indicateTestsFinished() {
    MyThread.sleep(20);
    System.out.println();
    System.out.println("-------------------------");
    System.out.println("----  Tests finished ----");
    System.out.println("-------------------------");
    System.out.println();
    System.out.println("See test report at out/reports/tests/tests.xml\n");
  }

  /**
   * These tests range in size from focusing on very small elements (unit tests)
   * to larger combinations of methods and classes (integration tests) but
   * stop short of running {@link FullSystem}.
   */
  private void unitAndIntegrationTests() throws Exception {
    Context context = buildTestingContext("_unit_test");

    new ListPhotosTests(context).tests();

    handleShutdown(context);
  }

  /**
   * Run a test of the entire system.  In particular, runs code
   * from {@link FullSystem}
   */
  private void testFullSystem_Soup_To_Nuts() throws Exception {
    Context context = buildContextFunctionalTests();

    new FunctionalTests(context).test();

    shutdownFunctionalTests(context);
  }

  private void handleShutdown(Context context) throws IOException {
    var logger = (TestLogger) context.getLogger();
    context.getFileUtils().deleteDirectoryRecursivelyIfExists(Path.of(constants.DB_DIRECTORY), logger);
    new ActionQueueKiller(context).killAllQueues();
    context.getExecutorService().shutdownNow();
    context.getLogger().stop();
  }

  private Context buildContextFunctionalTests() throws IOException {
    System.out.println("Starting a soup-to-nuts tests of the full system");
    var context = buildTestingContext("_integration_test");
    new FullSystem(context).start();
    new TheRegister(context).registerDomains();
    return context;
  }

  private void shutdownFunctionalTests(Context context) throws IOException {
    // delay a sec so our system has time to finish before we start deleting files
    MyThread.sleep(300);
    context.getFileUtils().deleteDirectoryRecursivelyIfExists(Path.of(context.getConstants().DB_DIRECTORY), context.getLogger());
    var fs = context.getFullSystem();
    fs.close();
    context.getLogger().stop();
    context.getExecutorService().shutdownNow();
  }

}
