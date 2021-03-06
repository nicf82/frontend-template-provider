/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.frontendtemplateprovider

import akka.actor.{ActorSystem, Cancellable}
import org.scalatest.{Matchers, WordSpec}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.frontendtemplateprovider.controllers.GovUkTemplateRendererController
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSGet
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.renderer.MustacheRendererTrait

import scala.concurrent.{ExecutionContext, Future}

class MainSpec extends WordSpec with Matchers  with Results with WithFakeApplication {

  implicit val hc = HeaderCarrier()

  val fakeRequest = FakeRequest("GET", "/")

  "Main" should {
    "not add a main class for main tag if non specified SDT 571" in new Setup {
      val renderedHtml: String = localTemplateRenderer.parseTemplate(Html(""), Map()).body
      val main = mainTagRegex.findFirstIn(renderedHtml).get
      main should not include("class")
    }

    "allow main tag to have it's mainClass SDT 571" in new Setup {
      val renderedHtml: String = localTemplateRenderer.parseTemplate(Html(""), Map(
        "mainClass" -> "clazz"
      )).body
      val main = mainTagRegex.findFirstIn(renderedHtml).get
      main should include("""class="clazz"""")
    }

    "allow main attributes to be specified in main tag SDT 572" in new Setup {
      val attribute = "id=\"main\""
      val renderedHtml: String = localTemplateRenderer.parseTemplate(Html(""), Map(
        "mainAttributes" -> attribute
      )).body
      val main = mainTagRegex.findFirstIn(renderedHtml).get
      main should include(attribute)
    }

    "not show beta banner if there is no service name SDT 476" in new Setup {
      val renderedHtml: String = localTemplateRenderer.parseTemplate(Html(""), Map()).body
      renderedHtml should not include("""<div class="beta-banner">""")
    }

    "show beta banner when you specify a service name SDT 476" in new Setup {
      val renderedHtml: String = localTemplateRenderer.parseTemplate(Html(""), Map(
        "betaBanner" -> Map("feedbackIdentifier" -> "PTA")
      )).body
      renderedHtml should include("""<div class="beta-banner">""")
      renderedHtml should include("""href="beta-feedback-unauthenticated?service=PTA"""")
    }

    "show beta banner with no feedback link if you don't specify a feedbackIdentifier SDT 476" in new Setup {
      val renderedHtml: String = localTemplateRenderer.parseTemplate(Html(""), Map(
        "betaBanner" -> true
      )).body
      renderedHtml should include("""<div class="beta-banner">""")
      renderedHtml should not include("""href="beta-feedback-unauthenticated?service=PTA"""")
      renderedHtml should include("This is a new service.")
    }

    "hmrc branding included if set SDT-482" in new Setup {
      val renderedHtml: String = localTemplateRenderer.parseTemplate(Html(""), Map(
        "includeHMRCBranding" -> true
      )).body
      renderedHtml should include("""div class="logo">""")
      renderedHtml should include("""<span class="organisation-logo organisation-logo-medium">HM Revenue &amp; Customs</span>""")
    }

    "hmrc branding not included if not set SDT-482" in new Setup {
      val renderedHtml: String = localTemplateRenderer.parseTemplate(Html(""), Map()).body
      renderedHtml should not include("""div class="logo">""")
      renderedHtml should not include("""<span class="organisation-logo organisation-logo-medium">HM Revenue &amp; Customs</span>""")
    }

    "Do not show login information if userDisplayName is not set SDT-481" in new Setup {
      val renderedHtml: String = localTemplateRenderer.parseTemplate(Html(""), Map()).body
      renderedHtml should not include("this is the first time you have logged in")
      renderedHtml should not include("you last signed in")
      renderedHtml should not include("Sign out")
    }

    "Show 'first time logged in' if userDisplayName is set and previouslyLoggedInAt not set SDT-481" in new Setup {
      val renderedHtml: String = localTemplateRenderer.parseTemplate(Html(""), Map(
        "showLastLogInStatus" -> Map(
          "userDisplayName" -> "Bob"
        )
      )).body
      renderedHtml should include("Bob, this is the first time you have logged in")
      renderedHtml should not include("you last signed in")
      renderedHtml should not include("Sign out")
    }

    // put all in map for showLastLoginTime
    "Show 'you last signed in' if userDisplayName and previouslyLoggedInAt are set SDT-481" in new Setup {
      val userDisplayName = "Bob"
      val previouslyLoggedInAt = "1st November 2016"
      val renderedHtml: String = localTemplateRenderer.parseTemplate(Html(""), Map(
        "showLastLogInStatus" -> Map(
          "userDisplayName" -> userDisplayName,
          "previouslyLoggedInAt" -> previouslyLoggedInAt
        )
      )).body
      renderedHtml should not include(s"$userDisplayName, this is the first time you have logged in")
      renderedHtml should include(s"${userDisplayName}, you last signed in $previouslyLoggedInAt")
      renderedHtml should not include("Sign out")
    }

    "Show Sign out link for user if logout url is specified SDT-481" in new Setup {
      val logoutUrl = "www.example.com/logout"
      val renderedHtml: String = localTemplateRenderer.parseTemplate(Html(""), Map(
        "showLastLogInStatus" -> Map(
          "logoutUrl" -> logoutUrl
        )
      )).body
      renderedHtml should include(s"""<br><a id="logOutStatusHref" href="$logoutUrl">Sign out</a>""")
    }

    "Show article when passed in" in new Setup {
      val article = "<p>hello world</p>"
      val renderedHtml: String = localTemplateRenderer.parseTemplate(Html(article), Map()).body
      renderedHtml should include(article)
    }
  }


  trait Setup {

    val result: Future[Result] = GovUkTemplateRendererController.serveMustacheTemplate()(fakeRequest)
    val bodyText: String = contentAsString(result)
    status(result) shouldBe OK

    val mainTagRegex = "<main\\b[^>]*>".r

    val localTemplateRenderer = new MustacheRendererTrait {
      override lazy val templateServiceAddress: String = ???
      override lazy val connection: WSGet = ???
      override def scheduleGrabbingTemplate()(implicit ec: ExecutionContext): Cancellable = ???
      override lazy val akkaSystem: ActorSystem = ???

      override protected def getTemplate: String = bodyText
    }
  }
}
