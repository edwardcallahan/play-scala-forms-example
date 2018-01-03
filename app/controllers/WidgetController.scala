package controllers

import javax.inject.Inject

import models.WidgetSize
import play.api.data._
import play.api.i18n._
import play.api.mvc._

/**
 * The classic WidgetController using MessagesAbstractController.
 *
 * Instead of MessagesAbstractController, you can use the I18nSupport trait,
 * which provides implicits that create a Messages instances from
 * a request using implicit conversion.
 *
 * See https://www.playframework.com/documentation/2.6.x/ScalaForms#passing-messagesprovider-to-form-helpers
 * for details.
 */
class WidgetController @Inject()(cc: MessagesControllerComponents) extends MessagesAbstractController(cc) {
  import WidgetForm._

  private val widgetSizes = scala.collection.immutable.HashMap[String, WidgetSize](
    "XS" -> WidgetSize("XS", "Extra Small: up to 0.10 mm"),
    "S" -> WidgetSize("S", "Small: 0.10 to 0.99 mm"),
    "M" -> WidgetSize("M", "Medium: 1.00 to 1.99 mm"),
    "L" -> WidgetSize("L", "Large: 2.00 - 3.99 mm"),
    "XL" -> WidgetSize("XL", "Extra Large: 4.00 mm and up")
  )

  // The URL to the widget size chooser.
  //  You can call this directly from the template, but it
  // can be more convenient to leave the template commpletely stateless i.e. all
  // of the "WidgetController" references are inside the .scala file.
  private val postUrl = routes.WidgetController.selectWidgetSize()

  def index = Action {
    Ok(views.html.index())
  }

  def listWidgetSizes = Action { implicit request: MessagesRequest[AnyContent] =>
    // Pass an unpopulated form to the template
    Ok(views.html.selectWidgetSize(widgetSizes.values.toIndexedSeq, form, postUrl))
  }

  // This will be the action that handles our form post
  def selectWidgetSize = Action { implicit request: MessagesRequest[AnyContent] =>
    val errorFunction = { formWithErrors: Form[Data] =>
      // This is the bad case, where the form had validation errors.
      // Let's show the user the form again, with the errors highlighted.
      // Note how we pass the form with errors to the template.
      BadRequest(views.html.selectWidgetSize(widgetSizes.values.toIndexedSeq, formWithErrors, postUrl))
    }

    val successFunction = { data: Data =>
      // This is the good case, where the form was successfully parsed as a Data.
      val sizeId = data.id
      val sizeDescription = widgetSizes.get(sizeId).get.description
      Ok(views.html.displaySelection("Success",s"Selected: ${sizeId} ${sizeDescription}"))
    }

    val formValidationResult = form.bindFromRequest
    formValidationResult.fold(errorFunction, successFunction)
  }

}