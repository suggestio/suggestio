package cordova.plugins.inappbrowser

import scala.scalajs.js
import scala.scalajs.js.|

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.11.2020 13:10
  * Description: Интерфейс для InAppBrowser.
  */
@js.native
sealed trait InAppBrowser extends js.Object {

  def open(url: String,
           target: InAppBrowser.Target = js.native,
           options: js.UndefOr[String] = js.native,
          ): InAppBrowserRef | Unit = js.native

}
object InAppBrowser {

  type Target <: String
  object Target {
    final def BLANK = "_blank".asInstanceOf[Target]
    final def SELF = "_self".asInstanceOf[Target]
    final def SYSTEM = "_system".asInstanceOf[Target]
  }


  implicit final class ApiExt( private val api: InAppBrowser ) extends AnyVal {

    def openInApp(url: String, options: js.UndefOr[String] = js.undefined): InAppBrowserRef =
      api.open(url, Target.BLANK, options).asInstanceOf[InAppBrowserRef]

  }


  case class OpenOption(k: String, v: String)
  type OpenOptions = List[OpenOption]
  object OpenOption {

    def YES = "yes"
    def NO = "no"
    def yesNo(isYes: Boolean) = if (isYes) YES else NO


    type BeforeLoad <: String
    object BeforeLoad {
      final def GET = "get".asInstanceOf[BeforeLoad]
      final def POST = "post".asInstanceOf[BeforeLoad]
      final def YES = OpenOption.YES.asInstanceOf[BeforeLoad]
    }


    implicit final class OptExt( private val openOptionsAcc: OpenOptions ) extends AnyVal {

      def locationBar(withLocationBar: Boolean): OpenOptions =
        withOption( "location", yesNo(withLocationBar) )

      // Android supports these additional options:
      def hidden(isHidden: Boolean): OpenOptions =
        withOption( "hidden", yesNo(isHidden ) )

      def beforeLoad(methodOrYes: BeforeLoad): OpenOptions =
        withOption( "beforeload", methodOrYes.toString )

      def clearCache(isClear: Boolean): OpenOptions =
        withOption( "clearcache", yesNo(isClear) )

      def clearSessionCache( isClear: Boolean ): OpenOptions =
        withOption( "clearsessioncache", yesNo(isClear) )

      def closeButtonCaption(isClear: Boolean): OpenOptions =
        withOption( "closebuttoncaption", yesNo(isClear) )

      def closeButtonColor( colorHex: String ): OpenOptions =
        withOption( "closebuttoncolor", colorHex )

      // TODO Продолжить оставшиеся опции в https://github.com/apache/cordova-plugin-inappbrowser#cordovainappbrowseropen

      def withOption( k: String, value: String ) =
        OpenOption(k, value) :: openOptionsAcc

      def toOptionsString(): String = {
        openOptionsAcc
          .iterator
          .map { o =>
            o.k + "=" + o.v
          }
          .mkString(",")
      }

    }
  }

  def options() = List.empty[OpenOption]

}


@js.native
trait InAppBrowserRef extends js.Object {

  def addEventListener(eventType: String, callback: js.Function1[InAppBrowserEvent, _]): Unit = js.native

  def removeEventListener(eventType: String, callback: js.Function1[InAppBrowserEvent, _]): Unit = js.native

  def close(): Unit = js.native

  def show(): Unit = js.native

  def executeScript(injectDetails: InjectDetails,
                    callback: js.Function1[js.Array[js.Any], _] = js.native): Unit = js.native

  def insertCSS(details: InjectDetails,
                callback: js.Function0[_] = js.native): Unit = js.native

}


trait InjectDetails extends js.Object {
  def file: js.UndefOr[String] = js.undefined
  def code: js.UndefOr[String] = js.undefined
}


@js.native
trait InAppBrowserEvent extends js.Object {
  val `type`: String
  val url: String
  val code: js.UndefOr[Int]
  val message: js.UndefOr[String]
  val data: js.UndefOr[String]
}
object InAppBrowserEvent {

  object Types {
    final def LOAD_START = "loadstart"
    final def LOAD_STOP = "loadstop"
    final def LOAD_ERROR = "loaderror"
    final def EXIT = "exit"
    final def BEFORE_LOAD = "beforeload"
    final def MESSAGE = "message"
  }

}
