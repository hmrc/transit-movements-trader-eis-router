package models

import models.RoutingOption.{Gb, Reject, Xi}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class RoutingOptionSpec extends AnyFreeSpec with Matchers {

  "parseRoutingOption" - {
    "must return Gb if input is \"Gb\"" in {
      RoutingOption.parseRoutingOption("Gb") mustBe Gb
    }

    "must return Xi if input is \"Xi\"" in {
      RoutingOption.parseRoutingOption("Xi") mustBe Xi
    }

    "must return Reject if input is \"Reject\"" in {
      RoutingOption.parseRoutingOption("Reject") mustBe Reject
    }

    "must throw an exception for any other input" in {
      assertThrows[Exception](RoutingOption.parseRoutingOption("Apples"))
    }
  }
}
