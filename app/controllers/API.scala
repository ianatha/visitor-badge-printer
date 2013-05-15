package controllers

import play.api.libs.json._

import play.api._
import play.api.mvc._
import templates.Html
import models._
import models.Person
import org.joda.time.{DateTimeZone, DateTime}
import play.api.data.format.Formats.jodaDateTimeFormat
import java.util.UUID

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import org.joda.time.format.{ISODateTimeFormat, DateTimeFormat, DateTimeFormatter}
import org.joda.time.DateTimeZone._

object API extends Controller {
 def redb = Action { implicit request =>
    AppDB.database.withSession { implicit session: scala.slick.session.Session =>
      try {
        AppDB.dal.drop
      } catch {
        case e => println(e)
      }

      try {
        AppDB.dal.create
      } catch {
        case e => println(e)
      }
    }

    Ok("")
 }

  def personsToJsons(visitor: Seq[Person]) = Json.toJson(visitor.map { v =>
    Map(
      "id" -> v.id.toString
      ,"name" -> v.first_name
      ,"last" -> v.last_name
      ,"host" -> v.host
      ,"created_at" -> v.created_at.toDateTime(DateTimeZone.forID("America/Los_Angeles")).toString(ISODateTimeFormat.dateTime())
    )
  })

  def present(location: String) = Action {
    val visitor = AppDB.database.withSession { implicit session: scala.slick.session.Session =>
      AppDB.dal.Persons.present(location)
    }

    Ok(personsToJsons(visitor))
  }

  def visitor(id: java.util.UUID) = Action {
    val visitor = AppDB.database.withSession { implicit session: scala.slick.session.Session =>
      AppDB.dal.Persons.get(id)
    }.headOption

    Ok(personsToJsons(visitor.toList))
  }

  def last_few_events() = Action {
    Ok(AppDB.database.withSession { implicit session: scala.slick.session.Session =>
      AppDB.dal.Persons.last_few_events()
    }.toString)
  }

  def printed(needle: java.util.UUID) = Action {
    val persons = AppDB.database.withSession { implicit session: scala.slick.session.Session =>
      AppDB.dal.Persons.signout(needle)
    }

    Ok("")
  }

  def signout(needle: java.util.UUID) = Action {
    val persons = AppDB.database.withSession { implicit session: scala.slick.session.Session =>
      AppDB.dal.Persons.signout(needle)
    }

    Ok("")
  }
}