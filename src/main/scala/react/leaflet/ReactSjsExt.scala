package react.leaflet

import japgolly.scalajs.react.React

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.12.16 14:35
  * Description: Extended APIs, not implemented in scalajs-react.
  */

// TODO Вынести в react-common-sjs

@js.native
trait ReactSjsExt extends js.Object {

  val PropTypes: PropTypesFacace = js.native

}

@js.native
trait PropTypesFacace extends js.Object {

  def instanceOf(v: js.Any): js.Function = js.native

}


object ReactSjsExt {

  import scala.language.implicitConversions

  implicit def apply(react: React): ReactSjsExt = {
    react.asInstanceOf[ReactSjsExt]
  }

}
