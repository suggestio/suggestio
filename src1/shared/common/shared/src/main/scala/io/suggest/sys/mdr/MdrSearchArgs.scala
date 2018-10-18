package io.suggest.sys.mdr

import io.suggest.adv.rcvr.RcvrKey
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.18 16:37
  * Description: Аргументы модерации.
  */

object MdrSearchArgs {

  def default = MdrSearchArgs()


  object Fields {

    def OFFSET_FN               = "o"
    def FREE_ADV_IS_ALLOWED_FN  = "f"

    /**
      * Можно скрыть какую-нибудь карточку. Полезно скрывать только что отмодерированную, т.к. она
      * некоторое время ещё будет висеть на этой странице.
      */
    def HIDE_AD_ID_FN           = "h"

    def RCVR_KEY_FN             = "r"
  }


  /** Поддержка play-json. */
  implicit def mdrSearchArgsFormat: OFormat[MdrSearchArgs] = {
    val F = Fields
    (
      (__ \ F.OFFSET_FN).formatNullable[Int] and
      (__ \ F.FREE_ADV_IS_ALLOWED_FN).formatNullable[Boolean] and
      (__ \ F.HIDE_AD_ID_FN).formatNullable[String] and
      (__ \ F.RCVR_KEY_FN).formatNullable[RcvrKey]
    )(apply, unlift(unapply))
  }


  @inline implicit def univEq: UnivEq[MdrSearchArgs] = UnivEq.derive

}


case class MdrSearchArgs(
                          offsetOpt             : Option[Int]       = None,
                          isAllowed             : Option[Boolean]   = None,
                          hideAdIdOpt           : Option[String]    = None,
                          rcvrKey               : Option[RcvrKey]   = None,
                        ) {

  def offset  = offsetOpt.getOrElse(0)

}
