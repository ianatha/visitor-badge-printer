package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index())
  }

  def nda(to: String) = Action {
    Ok(views.html.nda(to))
  }

  def guest = Action {
    Ok(views.html.guest())
  }

  def event = Action {
    Ok(views.html.event())
  }

  def interview = Action {
    Ok(views.html.interview())
  }

  def present = Action {
    Ok(views.html.present())
  }
}