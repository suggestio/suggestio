package io.suggest.req

import javax.inject.{Inject, Singleton}
import io.suggest.util.logs.MacroLogsImpl
import play.api.mvc._

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.17 22:20
  * Description: Утиль для какой-то обобщённой работы с реквестами.
  */
@Singleton
final class ReqUtil @Inject() (
                                parse                   : PlayBodyParsers,
                                implicit private val ec : ExecutionContext,
                              )
  extends MacroLogsImpl
{

  /**
    * Для снижения кодогенерации компилятором, используем этот класс вместо трейтов ActionBuilder'а.
    * ActionBuilderImpl не котируем, т.к. завязан на ванильный Request вместо IReq[A].
    *
    * Дефолтовый BodyParser и ExecutionContext просто пробрасываются тут снаружи.
    */
  abstract class SioActionBuilderImpl[R[_]] extends ActionBuilder[R, AnyContent] {

    override def parser: BodyParser[AnyContent] = parse.default

    override protected def executionContext = ec

  }


  /** Дефолтовая реализация трейта ActionTransformer для снижения кодогенерации компилятором
    * и повышения компактности кода. */
  abstract class ActionTransformerImpl[-R[_], +P[_]] extends ActionTransformer[R, P] {
    override protected def executionContext = ec
  }

}
