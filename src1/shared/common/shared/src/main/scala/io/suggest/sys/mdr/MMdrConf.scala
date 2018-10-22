package io.suggest.sys.mdr

import io.suggest.adv.rcvr.RcvrKey
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.10.18 16:01
  * Description: Модель конфига формы модерации.
  */
object MMdrConf {

  object Fields {
    val IS_SU_FN = "s"
    val ON_NODE_KEY_FN = "n"
  }

  /** Поддержка play-json. */
  implicit def mMdrConfFormat: OFormat[MMdrConf] = {
    val F = Fields
    (
      (__ \ F.IS_SU_FN).format[Boolean] and
      (__ \ F.ON_NODE_KEY_FN).formatNullable[RcvrKey]
    )(apply, unlift(unapply))
  }


  /** Сборка вариантов модели. */
  object Variants {

    /** Системный конфиг. */
    def sys = apply(isSu = true, onNodeKey = None)

    /** Запуск в личном кабинете. */
    def lk(rcvrKey: RcvrKey, isSu: Boolean = false) = apply(isSu = isSu, onNodeKey = Some(rcvrKey))

  }


  implicit def univEq: UnivEq[MMdrConf] = UnivEq.derive

}


/** Контейнер данных конфигурации формы модерации.
  *
  * @param isSu Режим суперюзера.
  * @param onNodeKey Данные по узлу, в рамках которого запущена форма:
  */
case class MMdrConf(
                     isSu       : Boolean,
                     onNodeKey  : Option[RcvrKey]
                   ) {

  lazy val rcvrIdOpt = onNodeKey.flatMap(_.lastOption)

}
