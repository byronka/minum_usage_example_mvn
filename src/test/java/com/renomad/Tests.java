package com.renomad;

import com.renomad.sampledomain.ListPhotosTests;
import minum.Constants;
import minum.Context;
import minum.logging.TestLogger;
import minum.utils.*;
import minum.web.*;

import java.io.IOException;
import java.nio.file.Path;

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
      MyThread.sleep(100);
      ex.printStackTrace();
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
    Context context = buildContext();

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

  private Context buildContext() {
    Context context = new Context();
    TestLogger logger = new TestLogger(context, "_unit_test_logger");
    context.setLogger(logger);
    return context;
  }

  private void handleShutdown(Context context) throws IOException {
    var logger2 = (TestLogger) context.getLogger();
    logger2.writeTestReport("unit_tests");
    FileUtils.deleteDirectoryRecursivelyIfExists(Path.of(constants.DB_DIRECTORY), logger2);
    new ActionQueueKiller(context).killAllQueues();
    context.getExecutorService().shutdownNow();
    context.getLogger().stop();
  }

  private Context buildContextFunctionalTests() throws IOException {
    System.out.println("Starting a soup-to-nuts tests of the full system");
    var context = new Context();
    TestLogger logger = new TestLogger(context, "integration_test_logger");
    new FullSystem(context, logger).start();
    new TheRegister(context).registerDomains();
    return context;
  }

  private void shutdownFunctionalTests(Context context) throws IOException {
    // delay a sec so our system has time to finish before we start deleting files
    MyThread.sleep(500);
    FileUtils.deleteDirectoryRecursivelyIfExists(Path.of(context.getConstants().DB_DIRECTORY), context.getLogger());
    var fs2 = context.getFullSystem();
    fs2.close();
    context.getExecutorService().shutdownNow();
    ((TestLogger)context.getLogger()).writeTestReport("functional_tests");
    context.getLogger().stop();
  }
}
