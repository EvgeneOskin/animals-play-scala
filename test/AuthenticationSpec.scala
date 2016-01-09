import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._

import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class AuthenticationSpec extends Specification {

  "Authentication" should {
    "render the login page" in new WithApplication{
      val loginPage = route(FakeRequest(GET, "/login")).get

      status(loginPage) must equalTo(OK)
      contentType(loginPage) must beSome.which(_ == "text/html")
      contentAsString(loginPage) must contain ("Login")
    }
    "process login" in new WithApplication{
      val loginResponse = route(FakeRequest(POST, "/authenticate/userpass")).get

      status(loginResponse) must equalTo(OK)
      contentType(loginResponse) must beSome.which(_ == "text/html")
      contentAsString(loginResponse) must contain ("Login")
    }
  }
}
