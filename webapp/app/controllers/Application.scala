package controllers

import play.api._
import play.api.mvc._
import templates.Html
import models._
import org.joda.time.DateTime
import java.util.UUID

import play.api.data._
import play.api.data.Forms._
import models.Person
import scala.Some

object Application extends Controller {

  def ActionWithLocation(f: Request[AnyContent] => Result): Action[AnyContent] = {
    Action { request =>
      if (request.cookies.get("location").isEmpty) {
        Redirect(routes.Application.setLocation(None))
      } else {
        f(request)
      }
    }
  }

  def standardize_location(in: String): String = in.toUpperCase().replaceAll("[^A-Z]", "-")

  def setLocation(location: Option[String]) = Action { request =>
    if (location.isDefined) {
      Redirect(routes.Application.index()).withCookies(Cookie("location", location.get, maxAge = Some(2000000000)))
    } else {
      Ok(views.html.setlocation(request.cookies.get("location").getOrElse(Cookie("location", "")).value))
    }
  }

  def index = ActionWithLocation { request =>
    Ok(views.html.index())
  }

  def nda(to: String) = ActionWithLocation { request =>
    val nda_text = """NDA"""
    Ok(views.html.nda(to, nda_text))
  }

  def showAfterNdaOnly(html: Html): Action[AnyContent] = { ActionWithLocation { implicit request =>
      if (request.getQueryString("nda").isDefined) {
        Ok(html)
      } else {
        Redirect(routes.Application.index())
      }
    }
  }

  def formForEventType(v: VisitType.VisitType) = {
    Form[Person](
      mapping(
        "first_name" -> nonEmptyText
        ,"last_name" -> nonEmptyText
        ,"company" -> optional(text)
        ,"host" -> nonEmptyText
        ,"phone" -> optional(text)
        ,"email" -> optional(email)
        ,"nda_accepted" -> boolean
      )
        ((f,l,c,h,p,e,n) => Person(UUID.randomUUID(),f,l,c,h,p,e,v,n, DateTime.now()))
        (p => Some(p.first_name, p.last_name, p.company, p.host, p.phone, p.email, p.nda_accepted))
    )
  }

  val guestForm = formForEventType(VisitType.Meeting)

  def guest = showAfterNdaOnly(views.html.guest(guestForm))

  def guestReceive = ActionWithLocation { implicit request =>
    val loc = request.cookies.get("location").get.value
    guestForm.bindFromRequest.fold(
      formWithErrors => Ok(views.html.guest(formWithErrors)),
      value => {
        AppDB.database.withSession { implicit session: scala.slick.session.Session =>
          AppDB.dal.Persons.add(value.copy(location = loc))
        }
        Ok(views.html.success())
      }
    )
  }


  val eventForm = formForEventType(VisitType.Event)

  def event = showAfterNdaOnly(views.html.event(eventForm))

  def eventReceive = ActionWithLocation { implicit request =>
    val loc = request.cookies.get("location").get.value
    guestForm.bindFromRequest.fold(
      formWithErrors => Ok(views.html.event(formWithErrors)),
      value => {
        AppDB.database.withSession { implicit session: scala.slick.session.Session =>
          AppDB.dal.Persons.add(value.copy(location = loc))
        }
        Ok(views.html.success())
      }
    )
  }

  val interviewForm = formForEventType(VisitType.Interview)

  def interview = showAfterNdaOnly(views.html.interview(interviewForm))

  def interviewReceive = ActionWithLocation { implicit request =>
    val loc = request.cookies.get("location").get.value
    guestForm.bindFromRequest.fold(
      formWithErrors => Ok(views.html.interview(formWithErrors)),
      value => {
        AppDB.database.withSession { implicit session: scala.slick.session.Session =>
          AppDB.dal.Persons.add(value.copy(location = loc))
        }
        Ok(views.html.success())
      }
    )
  }

  def present = ActionWithLocation { request =>
    val persons = AppDB.database.withSession { implicit session: scala.slick.session.Session =>
      AppDB.dal.Persons.present(request.cookies.get("location").getOrElse(Cookie("location", "")).value)
    }
    Ok(views.html.present(persons, request.cookies.get("location").getOrElse(Cookie("location", "")).value))
  }


}