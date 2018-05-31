import ch.qos.logback.classic.AsyncAppender
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender

import static ch.qos.logback.classic.Level.INFO

appender("STDOUT", ConsoleAppender) {
  encoder(PatternLayoutEncoder) {
    pattern = "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p --- [%15.15t] %-40.40logger{39} : %m%n"
  }
}
appender("ASYNC", AsyncAppender) {
  discardingThreshold = 0
  queueSize = 512
  appenderRef("STDOUT")
}
logger("reactivemongo", INFO)
root(INFO, ["STDOUT"])
def logDir = "./logs"