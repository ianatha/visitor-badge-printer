package controllers

import play.api._
import play.api.mvc._
import templates.Html
import models._
import models.Person
import org.joda.time.DateTime
import play.api.data.format.Formats.jodaDateTimeFormat

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index())
  }

  def nda(to: String) = Action {
    Ok(views.html.nda(to))
  }

  import play.api.libs.json._

  def apipresent = Action {
    val visitor = AppDB.database.withSession { implicit session: scala.slick.session.Session =>
      AppDB.dal.Persons.get()
    }

    Ok(Json.toJson(visitor.head))
  }


  def visitor(id: Int) = Action {
    val visitor = AppDB.database.withSession { implicit session: scala.slick.session.Session =>
      AppDB.dal.Persons.get()
    }

    Ok(Json.toJson(visitor))
  }

  def showAfterNdaOnly(html: Html): Action[AnyContent] = {
    Action { implicit request =>
      if (request.getQueryString("nda").isDefined) {
        Ok(html)
      } else {
        Redirect(routes.Application.index())
      }
    }
  }

  import play.api.data._
  import play.api.data.Forms._
  import play.api.data.validation.Constraints._

  val guestForm = Form(
    mapping(
       "first_name" -> nonEmptyText
      ,"last_name" -> nonEmptyText
      ,"company" -> optional(text)
      ,"host" -> nonEmptyText
      ,"phone" -> optional(text)
      ,"email" -> optional(email)
      ,"nda_accepted" -> boolean
      ,"created_at" -> default(of[DateTime], DateTime.now())
    )(Person.apply)(Person.unapply)
  )

  def guest = showAfterNdaOnly(views.html.guest(guestForm))

  def guestReceive = Action { implicit request =>
    guestForm.bindFromRequest.fold(
      formWithErrors => Ok(views.html.guest(formWithErrors)),
      value => {
        AppDB.database.withSession { implicit session: scala.slick.session.Session =>
          AppDB.dal.Persons.add(value)
        }
        Ok(views.html.success())
      }
    )
  }

  def event = showAfterNdaOnly(views.html.event())

  def eventReceive = Action { implicit request =>
    val persons = AppDB.database.withSession { implicit session: scala.slick.session.Session =>
      AppDB.dal.Persons.get()
    }
    Ok(views.html.present(persons))

  }

  def interview = showAfterNdaOnly(views.html.interview())

  def interviewReceive = Action { implicit request =>
    val persons = AppDB.database.withSession { implicit session: scala.slick.session.Session =>
      AppDB.dal.Persons.get()
    }
    Ok(views.html.present(persons))

  }

  def present = Action {
    val persons = AppDB.database.withSession { implicit session: scala.slick.session.Session =>
      AppDB.dal.Persons.get()
    }
    Ok(views.html.present(persons))
  }

  def signout(first_name: String, last_name: String) = Action {
    val persons = AppDB.database.withSession { implicit session: scala.slick.session.Session =>

      AppDB.dal.Persons.signout(first_name, last_name)

      AppDB.dal.Persons.get()
    }

    Ok(views.html.present(persons))
  }
}