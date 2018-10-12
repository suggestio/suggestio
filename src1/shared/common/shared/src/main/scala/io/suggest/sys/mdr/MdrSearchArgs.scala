package io.suggest.sys.mdr

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

    def PRODUCER_ID_FN          = "p"
    def OFFSET_FN               = "o"
    def FREE_ADV_IS_ALLOWED_FN  = "f"

    /**
      * Можно скрыть какую-нибудь карточку. Полезно скрывать только что отмодерированную, т.к. она
      * некоторое время ещё будет висеть на этой странице.
      */
    def HIDE_AD_ID_FN           = "h"

  }


  /** Поддержка play-json. */
  implicit def mdrSearchArgsFormat: OFormat[MdrSearchArgs] = {
    val F = Fields
    (
      (__ \ F.OFFSET_FN).formatNullable[Int] and
      (__ \ F.PRODUCER_ID_FN).formatNullable[String] and
      (__ \ F.FREE_ADV_IS_ALLOWED_FN).formatNullable[Boolean] and
      (__ \ F.HIDE_AD_ID_FN).formatNullable[String]
    )(apply, unlift(unapply))
  }


  @inline implicit def univEq: UnivEq[MdrSearchArgs] = UnivEq.derive

}


case class MdrSearchArgs(
                          offsetOpt             : Option[Int]       = None,
                          producerId            : Option[String]    = None,
                          isAllowed             : Option[Boolean]   = None,
                          hideAdIdOpt           : Option[String]    = None,
                        ) {

  def offset  = offsetOpt.getOrElse(0)

}
