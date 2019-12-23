package io.suggest.id.login

import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.xplay.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.19 14:04
  * Description: поддержка моделей [[ILoginFormPages]] на стороне сервера.
  */
object LoginFormPagesJvm {

  /** qs-биндинг таба Login-страницы. */
  implicit def loginTabQsb: QueryStringBindable[MLoginTab] =
    EnumeratumJvmUtil.valueEnumQsb( MLoginTabs )


  /** qs-биндинг для Login-страницы. */
  implicit def loginPageQsb(implicit
                            loginTabB       : QueryStringBindable[MLoginTab],
                            stringOptB      : QueryStringBindable[Option[String]],
                           ): QueryStringBindable[ILoginFormPages.Login] = {
    new QueryStringBindableImpl[ILoginFormPages.Login] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ILoginFormPages.Login]] = {
        val k = key1F(key)
        val F = ILoginFormPages.Login.Fields
        for {
          loginTabE         <- loginTabB.bind ( k(F.CURR_TAB_FN),   params )
          returnUrlOptB     <- stringOptB.bind( k(F.RETURN_URL_FN), params )
        } yield {
          for {
            loginTab        <- loginTabE
            returnUrlOpt    <- returnUrlOptB
          } yield {
            ILoginFormPages.Login(
              currTab       = loginTab,
              returnUrl     = returnUrlOpt,
            )
          }
        }
      }

      override def unbind(key: String, value: ILoginFormPages.Login): String = {
        val k = key1F(key)
        val F = ILoginFormPages.Login.Fields
        _mergeUnbinded1(
          loginTabB.unbind ( k(F.CURR_TAB_FN),   value.currTab   ),
          stringOptB.unbind( k(F.RETURN_URL_FN), value.returnUrl ),
        )
      }

    }
  }


  /** Поддержка qs-биндинга для [[ILoginFormPages]]. */
  implicit def loginPagesQsb(implicit
                             loginPageB: QueryStringBindable[ILoginFormPages.Login]
                            ): QueryStringBindable[ILoginFormPages] = {
    new QueryStringBindableImpl[ILoginFormPages] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ILoginFormPages]] = {
        loginPageB
          .bind( key, params )
          .orElse {
            val r = Right( ILoginFormPages.Login.default )
            Some(r)
          }
      }

      override def unbind(key: String, value: ILoginFormPages): String = {
        value match {
          case l: ILoginFormPages.Login =>
            loginPageB.unbind(key, l)
        }
      }
    }
  }

}
