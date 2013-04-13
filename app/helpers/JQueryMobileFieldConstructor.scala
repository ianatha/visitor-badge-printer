package helpers

import views.html.helper.FieldConstructor

object JQueryMobileFieldConstructor {
  implicit val jqueryMobileField = FieldConstructor(views.html.jqueryMobileFieldConstructor.f)
}
