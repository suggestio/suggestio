package io.suggest.id.login

import io.suggest.spa.DAction
import japgolly.univeq.UnivEq
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.19 10:54
  * Description: Модель страниц spa-роутера формы логина.
  */

sealed trait ILoginFormPages extends DAction


/** Статическая поддержка SPArouter-экшенов. */
object ILoginFormPages {

  /** Класс-контейнер данных URL текущей формы.
    *
    * @param currTab Текущий открытый таб.
    * @param returnUrl Значение "?r=..." в ссылке.
    */
  final case class Login(
                          currTab       : MLoginTab       = MLoginTabs.default,
                          returnUrl     : Option[String]  = None,
                        )
    extends ILoginFormPages

  object Login {

    def default = apply()

    object Fields {
      def CURR_TAB_FN   = "t"
      def RETURN_URL_FN = "r"
    }

    def currTab = GenLens[Login](_.currTab)
    def returnUrl = GenLens[Login](_.returnUrl)

  }

  @inline implicit def univEq: UnivEq[ILoginFormPages] = UnivEq.derive

}

