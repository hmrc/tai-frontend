import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.Keys._
import scala.sys.process.Process

/**
 * Build of UI in JavaScript
 */
object JavaScriptBuild {

  import play.sbt.PlayImport.PlayKeys._
  import play.sbt.routes.RoutesKeys._

  val jsDirectory = SettingKey[File]("js-directory")

  val gruntBuild = TaskKey[Int]("grunt-build")
  val gruntWatch = TaskKey[Int]("grunt-watch")
  val npmInstall = TaskKey[Int]("npm-install")


  val javaScriptUiSettings = Seq(

    // the JavaScript application resides in "ui"
    jsDirectory := baseDirectory.value /"app" / "assets" / "js",

    // add "npm" and "grunt" commands in sbt
    commands ++= jsDirectory { base => Seq(Grunt.gruntCommand(base), npmCommand(base))}.value,

    npmInstall := {
      val result = Grunt.npmProcess(jsDirectory.value, "install").run().exitValue()
      if (result != 0)
        throw new Exception("Npm install failed.")
      result
    },

    gruntBuild := {
      val result = Grunt.gruntProcess(jsDirectory.value, "prod").run().exitValue()
      if(result != 0)
        throw new Exception("Grunt build failed.")
      result
    },

    gruntWatch := {
      val result = Grunt.gruntProcess(jsDirectory.value, "watch").run().exitValue()
      if(result != 0)
        throw new Exception("Grunt watch failed.")
      result
    },

    gruntBuild := (gruntBuild dependsOn npmInstall).value,

    // runs grunt before staging the application
    dist := (dist dependsOn gruntBuild).value,

    // integrate JavaScript build into play build
    playRunHooks += jsDirectory.map(ui => Grunt(ui)).value
  )

  def npmCommand(base: File) = Command.args("npm", "<npm-command>") { (state, args) =>
    if (sys.props("os.name").toLowerCase contains "windows") {
      Process("cmd" :: "/c" :: "npm" :: args.toList, base) !
    } else {
      Process("npm" :: args.toList, base) !
    }
    state
  }
}