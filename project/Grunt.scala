import sbt._
import play.sbt.PlayRunHook
import scala.sys.process.Process

/*
  Grunt runner
*/
object Grunt {
  def apply(base: File): PlayRunHook = {

    object GruntProcess extends PlayRunHook {

      var gruntRun: Option[Process] = None

      override def beforeStarted(): Unit = {
        val env = "dev"
        val log = ConsoleLogger()
        log.info(s"run npm install... $env")
        npmProcess(base, "install").!

        log.info("Starting default Grunt task..")
        gruntRun = Some(gruntProcess(base, env, "--verbose").run())
      }

      override def afterStopped(): Unit = {
        // Stop grunt when play run stops
        gruntRun.map(p => p.destroy())
        gruntRun = None
      }

    }

    GruntProcess
  }

  def gruntCommand(base: File) = Command.args("grunt", "<grunt-command>") { (state, args) =>
    gruntProcess(base, args:_*) !;
    state
  }
  def gruntProcess(base: File, args: String*) = Process("node" :: "node_modules/.bin/grunt" :: args.toList, base)
  def npmProcess(base: File, args: String*) = Process("npm" :: args.toList, base)

}
