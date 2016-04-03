import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import org.specs2.execute.AsResult

import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends PlaySpecification {

  "Application" should {

    "send 404 on a bad request" in new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase()))  {
      route(FakeRequest(GET, "/boum")) must beSome.which (status(_) == NOT_FOUND)
    }

    "render the index page" in new WithApplication(FakeApplication(additionalConfiguration = inMemoryDatabase()))  {
      val home = route(FakeRequest(GET, "/")).get

      status(home) must equalTo(OK)
      contentType(home) must beSome.which(_ == "text/html")
      contentAsString(home) must contain ("Your new application is ready.")
    }
  }
}
