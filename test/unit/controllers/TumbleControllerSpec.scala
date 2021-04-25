package unit.controllers

import akka.stream.Materializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import controllers.TumbleController
import models.TumbleDTO
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Configuration
import play.api.mvc.{ControllerComponents, DefaultActionBuilder}
import play.api.test.Helpers.{POST, contentAsString}
import play.api.test.{FakeRequest, _}
import services.JobCoinTumbler

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class TumbleControllerSpec extends PlaySpec with MockitoSugar with GuiceOneAppPerSuite {

  implicit lazy val materializer: Materializer = app.materializer
  implicit val timeout: Timeout = Timeout(5.seconds)

  val tumbler = mock[JobCoinTumbler]
  val cc = mock[ControllerComponents]

  when(tumbler.initializeTumble(any[TumbleDTO])) thenReturn(Future.successful("UUIDResult"))

  "TumbleController" should {
    "return an error when the body is missing" in {

      val request = FakeRequest(POST, "/v1/initializeTumble")
      val tumbleController = new TumbleController(Helpers.stubControllerComponents(), tumbler)
      val response = tumbleController.initializeTumble()(request)
      contentAsString(response) mustEqual("Payload Missing From Request")
    }
    "return a fresh UUID when requested" in {

      //TODO: figure out why the controller doesn't seem to be able to get the body
      val fakeRequest = FakeRequest(Helpers.POST, "/v1/initializeTumble", FakeHeaders(Seq[(String, String)](("Content-type", "application/json; charset=utf-8"),("",""))), """{"amount":1.0, "fromAddress":"Bob", "toAddresses":["Alice"]}""")
        .withBody[String]("""{"amount":1.0, "fromAddress":"Bob", "toAddresses":["Alice"]}""")
      val tumbleController = new TumbleController(Helpers.stubControllerComponents(), tumbler)
      val response = tumbleController.initializeTumble()(fakeRequest)
      contentAsString(response) mustEqual("UUIDResult")
    }
  }
}
