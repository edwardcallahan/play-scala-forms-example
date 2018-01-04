package controllers

import java.net.URI
import javax.inject._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

import akka.NotUsed
import akka.actor._
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.stream.Materializer
import akka.stream.scaladsl._
import akka.util.Timeout

import play.api.data._
import play.api.i18n._
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.streams.ActorFlow

import models.WidgetSize

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
class WidgetController @Inject()(@Named("processManagerActor") processManagerActor: ActorRef,
                                 cc: ControllerComponents)
                                (implicit system: ActorSystem, mat: Materializer,
                                ec: ExecutionContext,mcc: MessagesControllerComponents)
  extends MessagesAbstractController(mcc) {
  import WidgetForm._

  val logger = play.api.Logger(getClass)

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

  /**
    * Creates a websocket.  `acceptOrResult` is preferable here because it returns a
    * Future[Flow], which is required internally.
    *
    * @return a fully realized websocket.
    */
  def monitorWidgetCreation: WebSocket = WebSocket.acceptOrResult[JsValue, JsValue] {
    case rh if sameOriginCheck(rh) =>
      wsFutureFlow(rh).map { flow =>
        Right(flow)
      }.recover {
        case e: Exception =>
          logger.error("Cannot create websocket", e)
          val jsError = Json.obj("error" -> "Cannot create websocket")
          val result = InternalServerError(jsError)
          Left(result)
      }

    case rejected =>
      logger.error(s"Request ${rejected} failed same origin check")
      Future.successful {
        Left(Forbidden("forbidden"))
      }
  }

  /**
    * Creates a Future containing a Flow of JsValue in and out.
    */
  private def wsFutureFlow(request: RequestHeader): Future[Flow[JsValue, JsValue, NotUsed]] = {
    // Use guice assisted injection to instantiate and configure the child actor.
    implicit val timeout = Timeout(1.second) // the first run in dev can take a while :-(
    val future: Future[Any] = processManagerActor ? ProcessManagerActor.Runit(request.id.toString)
    val futureFlow: Future[Flow[JsValue, JsValue, NotUsed]] = future.mapTo[Flow[JsValue, JsValue, NotUsed]]
    futureFlow
  }

/*  def monitorWidgetCreation = WebSocket.accept[String, String] { request =>
  /*  val in = Sink.ignore
    val out = procMan.test//Source.single("Howdy")
    Flow.fromSinkAndSource(in, out)*/
    ActorFlow.actorRef { out =>
      PprocessManagerActor.props(out)
    }
  }*/

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
      Ok(views.html.displaySelection("Success", s"Selected: ${sizeId} ${sizeDescription}"))
    }

    val formValidationResult = form.bindFromRequest
    formValidationResult.fold(errorFunction, successFunction)
  }

    /**
      * Checks that the WebSocket comes from the same origin.  This is necessary to protect
      * against Cross-Site WebSocket Hijacking as WebSocket does not implement Same Origin Policy.
      *
      * See https://tools.ietf.org/html/rfc6455#section-1.3 and
      * http://blog.dewhurstsecurity.com/2013/08/30/security-testing-html5-websockets.html
      */
    private def sameOriginCheck(implicit rh: RequestHeader): Boolean = {
    // The Origin header is the domain the request originates from.
    // https://tools.ietf.org/html/rfc6454#section-7
    logger.debug("Checking the ORIGIN ")

    rh.headers.get("Origin") match {
      case Some(originValue) if originMatches(originValue) =>
        logger.debug(s"originCheck: originValue = $originValue")
        true

      case Some(badOrigin) =>
        logger.error(s"originCheck: rejecting request because Origin header value ${badOrigin} is not in the same origin")
        false

      case None =>
        logger.error("originCheck: rejecting request because no Origin header found")
        false
    }
  }

    /**
      * Returns true if the value of the Origin header contains an acceptable value.
      */
    private def originMatches(origin: String): Boolean = {
      try {
        val url = new URI(origin)
        url.getHost == "localhost" &&
          (url.getPort match { case 9000 | 19001 => true; case _ => false })
      } catch {
        case e: Exception => false
      }
    }

}