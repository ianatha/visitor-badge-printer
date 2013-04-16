package models

import slick.driver.ExtendedProfile

trait Storage {
  val profile: ExtendedProfile
}
