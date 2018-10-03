package io.suggest.sys.mdr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.18 16:37
  * Description: Аргументы модерации.
  */

object MdrSearchArgs {

  object Fields {

    def PRODUCER_ID_FN          = "prodId"
    def OFFSET_FN               = "o"
    def FREE_ADV_IS_ALLOWED_FN  = "f"

    /**
      * Можно скрыть какую-нибудь карточку. Полезно скрывать только что отмодерированную, т.к. она
      * некоторое время ещё будет висеть на этой странице.
      */
    def HIDE_AD_ID_FN           = "h"

  }

  def default = MdrSearchArgs()

}


case class MdrSearchArgs(
  offsetOpt             : Option[Int]       = None,
  producerId            : Option[String]    = None,
  isAllowed             : Option[Boolean]   = None,
  hideAdIdOpt           : Option[String]    = None,
  // limit не задаваем через qs, не было такой необходимости.
  limitOpt              : Option[Int]       = None
) {

  def offset  = offsetOpt.getOrElse(0)
  def limit   = limitOpt.getOrElse(12)      // 3 ряда по 4 карточки.

}
