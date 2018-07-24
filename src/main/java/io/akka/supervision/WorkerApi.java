package io.akka.supervision;

public interface WorkerApi {
    Object Start = "Start";
    Object Do = "Do";

    class Progress {
        public final double percent;

        public Progress(double percent) {
            this.percent = percent;
        }

        public String toString() {
            return String.format("%s(%s)", getClass().getSimpleName(), percent);
        }
    }
}