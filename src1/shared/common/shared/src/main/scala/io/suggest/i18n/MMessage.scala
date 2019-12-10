package io.suggest.i18n

import io.suggest.common.empty.EmptyUtil
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.12.16 15:06
  * Description: Модель сообщения, подлежащего client-side локализации перед рендером.
  * Внутри play есть встроенная похожая модель.
  *
  * 2016.dec.19: Из-за интеграции с boopickle пришлось пока отказаться от args: Any* в пользу явного Seq[Int].
  */
object MMessage {

  @inline implicit def univEq: UnivEq[MMessage] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }


  implicit def messageJson: OFormat[MMessage] = (
    (__ \ "m").format[String] and
    (__ \ "a").formatNullable[JsArray]
      .inmap[JsArray](
        EmptyUtil.opt2ImplEmpty1F( JsArray.empty ),
        args => if (args.value.isEmpty) None else Some(args)
      )
  )(apply, unlift(unapply))

}


/** Инстанс модели одного нелокализованного параметризованного сообщения об ошибке. */
case class MMessage(
                     message : String,
                     args    : JsArray = JsArray.empty,
                   )

