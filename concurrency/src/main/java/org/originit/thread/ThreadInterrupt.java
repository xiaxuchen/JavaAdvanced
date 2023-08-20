package org.originit.thread;

public class ThreadInterrupt {

    public static class BadUseRunnable implements Runnable {


        @Override
        public void run() {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                try {
                    // 让线程处于sleep状态下然后由interrupt打破
                    Thread.sleep(10000);
                } catch (InterruptedException ignored) {

                }
            }
        }
    }

    public static class GoodUseRunnable implements Runnable {

        @Override
        public void run() {
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    try {
                        // 让线程先不要死
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {

                    }
                    break;
                }
                try {
                    // 让线程处于sleep状态下然后由interrupt打破
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
