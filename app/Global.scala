import models.{User, DBeable, AppDB}
import play.api.db.DB
import play.api.GlobalSettings

import play.api.Application
import slick.session.Session

object Global extends GlobalSettings with DBeable {
  override def onStart(app: Application) {
    implicit val application = app
    lazy val database = getDb
    lazy val dal = getDal
    database.withSession {
      implicit session: Session =>
//        dal.create
    }
  }
}
