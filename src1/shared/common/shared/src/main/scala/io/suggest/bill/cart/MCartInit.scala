package io.suggest.bill.cart

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.18 15:55
  * Description: Кросс-платформенная модель инициализации компонента корзины.
  */
object MCartInit {

  /** Поддержка play-json. */
  implicit def mCartInitFormat: OFormat[MCartInit] = {
    (__ \ "c")
      .format[MCartConf]
      .inmap[MCartInit]( apply, _.conf )
  }

  @inline implicit def univEq: UnivEq[MCartInit] = UnivEq.derive

}


/** Класс-контейнер модели данных инициализации компонента корзины..
  *
  * @param conf Конфигурация.
  */
case class MCartInit(
                      conf: MCartConf
                    )
