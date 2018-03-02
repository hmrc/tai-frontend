package tfe

import org.scalatest.time.{Millis, Seconds, Span}
import uk.gov.hmrc.play.it.{ExternalService, ExternalServiceRunner, MicroServiceEmbeddedServer, ServiceSpec}

class TFeServer(override val testName: String) extends MicroServiceEmbeddedServer {

  val datastream = ExternalServiceRunner.runFromJar("datastream")
  val ptsStubs = ExternalServiceRunner.runFromJar("personal-tax-summary")

  override protected val externalServices: Seq[ExternalService] = Seq(datastream, ptsStubs)

  override def additionalConfig = Map(
    "application.router" -> "app.Routes"
  )
}

class TaiFrontendBaseISpec(testName: String) extends ServiceSpec {

  private val defaultPatienceConfig: PatienceConfig =
    PatienceConfig(
      timeout = scaled(Span(60, Seconds)),
      interval = scaled(Span(150, Millis))
    )

  implicit override val patienceConfig: PatienceConfig = defaultPatienceConfig

  override protected val server = new TFeServer(testName)
}