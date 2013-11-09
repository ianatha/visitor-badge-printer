package models
import play.api.Play.current

object AppDB extends DBeable {
  lazy val database = getDb
  lazy val dal = getDal
}
