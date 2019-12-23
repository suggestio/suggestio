package models.usr.esia

import java.util.UUID

import io.suggest.xplay.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.03.19 21:43
  * Description: ЕСИА вернула юзера назад по redirect_uri, но дописала в URL qs какие-то данные.
  */
object MEsiaAuthReturnQs {

  object Fields {
    def CODE_FN  = "code"
    def STATE_FN = "state"
    def ERROR_FN = "error"
  }

  implicit def esiaAuthReturnQsQsb(implicit
                                   strOptB: QueryStringBindable[Option[String]],
                                   uuidOptB: QueryStringBindable[Option[UUID]],
                                  ): QueryStringBindable[MEsiaAuthReturnQs] = {
    new QueryStringBindableImpl[MEsiaAuthReturnQs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MEsiaAuthReturnQs]] = {
        val F = Fields
        for {
          codeOptE    <- strOptB .bind( F.CODE_FN,   params )
          errorOptE   <- strOptB .bind( F.ERROR_FN,  params )
          // Фильтровать, чтобы code || error были обязательно заданы: TODO Сделать по-красивее этот if.
          if codeOptE.exists(_.nonEmpty) || errorOptE.exists(_.nonEmpty)
          // state не особо нужен, поэтому просто биндим вслепую.
          stateOptE   <- uuidOptB.bind( F.STATE_FN,  params )
        } yield {
          for {
            codeOpt   <- codeOptE
            stateOpt  <- stateOptE
            errorOpt  <- errorOptE
          } yield {
            MEsiaAuthReturnQs(
              code  = codeOpt,
              state = stateOpt,
              error = errorOpt,
            )
          }
        }
      }

      override def unbind(key: String, value: MEsiaAuthReturnQs): String = {
        // Вероятно, этот код не вызывается никогда.
        val F = Fields
        _mergeUnbinded1(
          strOptB.unbind  ( F.CODE_FN,    value.code ),
          uuidOptB.unbind ( F.STATE_FN,   value.state ),
          strOptB.unbind  ( F.ERROR_FN,   value.error ),
        )
      }
    }
  }

}


/** Контейнер qs-данных ссылки для возвращаемого юзера.
  *
  * @param code Авторизационный код для залогиненного юзера, если всё ок.
  * @param state Состояние (рандом), если всё ок.
  * @param error Код ошибки, если логин не удался.
  */
case class MEsiaAuthReturnQs(
                              code    : Option[String],
                              state   : Option[UUID],
                              error   : Option[String],
                            )
