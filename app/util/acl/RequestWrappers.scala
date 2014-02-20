package util.acl

import util.acl.PersonWrapper._
import play.api.mvc.{WrappedRequest, Request}
import models.MDomainAuthzT

/*
  Используется комбинация из абстрактных классов и их реализаций case class'ов. Это необходимо из-за невозможности
  сделать case class -> case class наследование ( http://stackoverflow.com/a/12706475 ). Таким убогим образом в scala
  можно обозначить наследование между двумя case class'ами: RequestWithPwOpt -> RequestWithPDAuthz.
  Это поможет генерить контексты одной и той же функцией.
 */


abstract class AbstractRequestWithPwOpt[A](request: Request[A]) extends WrappedRequest(request) {
  def pwOpt: PwOpt_t
  def isSuperuser = PersonWrapper isSuperuser pwOpt
  def isAuth = pwOpt.isDefined
}

/**
 * Wrapped-реквест для передачи pwOpt.
 * @param pwOpt Опциональный PersonWrapper.
 * @param request Исходнный реквест.
 * @tparam A Подтип реквеста.
 */
case class RequestWithPwOpt[A](pwOpt: PwOpt_t, request: Request[A]) extends AbstractRequestWithPwOpt(request)



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
case class RequestWithDAuthz[A](pwOpt: PwOpt_t, dAuthz: MDomainAuthzT, request: Request[A]) extends AbstractRequestWithDAuthz(request)


/** Админство магазина. */
abstract class AbstractRequestForShopAdm[A](request: Request[A]) extends AbstractRequestWithPwOpt(request) {
  def shopId: Int
}
case class RequestForShopAdm[A](shopId: Int, pwOpt:PwOpt_t, request: Request[A]) extends AbstractRequestForShopAdm(request)


/** Админство промо-оффера в магазине. */
abstract class AbstractRequestForPromoOfferAdm[A](request: Request[A]) extends AbstractRequestForShopAdm(request) {
  def offerId: String
}
case class RequestForPromoOfferAdm[A](shopId:Int, offerId:String, pwOpt:PwOpt_t, request: Request[A]) extends AbstractRequestForPromoOfferAdm(request)