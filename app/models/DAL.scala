package models

import slick.driver.ExtendedProfile
import play.api.Logger

class DAL(override val profile: ExtendedProfile)
  extends Storage
  with UserStorageComponent
  with PersonStorageComponent {
  val entities = Seq(
     Users
    ,Persons
  )
  import profile.simple._

  def drop(implicit session: Session): Unit = {
    entities.map { entity =>
      Logger.debug("Dropping %s".format(entity))
      entity.ddl.drop
    }
  }

  def create(implicit session: Session): Unit = {
    entities.map { entity =>
      Logger.debug("Creating %s".format(entity))
      entity.ddl.create
    }
  }
}
