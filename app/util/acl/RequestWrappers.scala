package util.acl

import util.acl.PersonWrapper._
import play.api.mvc._
import models._
import scala.concurrent.{Future, future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.db.DB
import play.api.Play.current

/*
  Используется комбинация из абстрактных классов и их реализаций case class'ов. Это необходимо из-за невозможности
  сделать case class -> case class наследование ( http://stackoverflow.com/a/12706475 ). Таким убогим образом в scala
  можно обозначить наследование между двумя case class'ами: RequestWithPwOpt -> RequestWithPDAuthz.
  Это поможет генерить контексты одной и той же функцией.
 */


abstract class AbstractRequestWithPwOpt[A](request: Request[A])
  extends WrappedRequest(request) {
  def pwOpt: PwOpt_t
  def sioReqMd: SioReqMd
  def isSuperuser = PersonWrapper isSuperuser pwOpt
  def isAuth = pwOpt.isDefined
}

/**
 * Wrapped-реквест для передачи pwOpt.
 * @param pwOpt Опциональный PersonWrapper.
 * @param request Исходнный реквест.
 * @tparam A Подтип реквеста.
 */
case class RequestWithPwOpt[A](pwOpt: PwOpt_t, request: Request[A], sioReqMd: SioReqMd)
  extends AbstractRequestWithPwOpt(request)



abstract class AbstractRequestWithDAuthz[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def dAuthz: MDomainAuthzT
  def dkey = dAuthz.dkey
}

/**
 * При ограничении прав в рамках домена используется сий класс, как бы родственный и расширенный по
 * отношению к RequestWithPwOpt.
 * @param pwOpt Данные о текущем юзере.
 * @param dAuthz Данные об авторизации в рамках домена.
 * @param request Реквест.
 * @tparam A Подтип реквеста.
 */
case class RequestWithDAuthz[A](pwOpt: PwOpt_t, dAuthz: MDomainAuthzT, request: Request[A], sioReqMd: SioReqMd)
  extends AbstractRequestWithDAuthz(request)


/** Админство магазина. */
abstract class AbstractRequestForShopAdm[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def shopId: String
}


/** Метаданные, относящиеся запросу. Сюда попадают данные, которые необходимы везде и требует асинхронных действий.
  * @param usernameOpt Отображаемое имя юзера, если есть. Формируются на основе данных сессии и данных из
  *                    [[models.MPerson]] и [[models.MPersonIdent]].
  */
case class SioReqMd(
  usernameOpt: Option[String] = None,
  billBallanceOpt: Option[MBillBalance] = None
)
object SioReqMd {
  /** Простая генерация srm на основе юзера. */
  def fromPwOpt(pwOpt: PwOpt_t): Future[SioReqMd] = {
    PersonWrapper.findUserName(pwOpt) map { usernameOpt =>
      SioReqMd(usernameOpt = usernameOpt)
    }
  }

  /** Генерация srm для юзера в рамках личного кабинета. */
  def fromPwOptAdn(pwOpt: PwOpt_t, adnId: String): Future[SioReqMd] = {
    val bbOptFut = future {
      DB.withConnection { implicit c =>
        MBillBalance.getByAdnId(adnId)
      }
    }
    for {
      usernameOpt <- PersonWrapper.findUserName(pwOpt)
      bbOpt       <- bbOptFut
    } yield {
      SioReqMd(usernameOpt, bbOpt)
    }
  }

}
