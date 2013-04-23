package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.test.TestServer
import org.fluentlenium.core.filter.FilterConstructor._

/**
 * add your integration spec here.
 * An integration test will fire up a whole play application in a real (or headless) browser
 */
class IntegrationSpec extends Specification {
  
  "Application" should {

    "work from within a browser" in {
      running(TestServer(3333), HTMLUNIT) { browser =>

        browser.goTo("http://localhost:3333/")

        Seq("meeting someone", "interviewing at", "attending an event").map {
          browser.pageSource must contain(_)
        }

        browser.$("a", withId("guest")).click()

        browser.pageSource must contain("Non-Disclosure Agreement")

        browser.$("a", withText.contains("Next")).click()

        browser.pageSource must contain("Person Visiting")
      }
    }

  }
  
}