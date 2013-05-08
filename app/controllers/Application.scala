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

  def index = Action {
    Ok(views.html.index())
  }

  def nda(to: String) = Action {
    val nda_text = """NDA"""
    Ok(views.html.nda(to, nda_text))
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


  val eventForm = formForEventType(VisitType.Event)

  def event = showAfterNdaOnly(views.html.event(eventForm))

  def eventReceive = Action { implicit request =>
    guestForm.bindFromRequest.fold(
      formWithErrors => Ok(views.html.event(formWithErrors)),
      value => {
        AppDB.database.withSession { implicit session: scala.slick.session.Session =>
          AppDB.dal.Persons.add(value)
        }
        Ok(views.html.success())
      }
    )
  }

  val interviewForm = formForEventType(VisitType.Interview)

  def interview = showAfterNdaOnly(views.html.interview(interviewForm))

  def interviewReceive = Action { implicit request =>
    guestForm.bindFromRequest.fold(
      formWithErrors => Ok(views.html.interview(formWithErrors)),
      value => {
        AppDB.database.withSession { implicit session: scala.slick.session.Session =>
          AppDB.dal.Persons.add(value)
        }
        Ok(views.html.success())
      }
    )
  }

  def present = Action {
    val persons = AppDB.database.withSession { implicit session: scala.slick.session.Session =>
      AppDB.dal.Persons.present()
    }
    Ok(views.html.present(persons))
  }


}