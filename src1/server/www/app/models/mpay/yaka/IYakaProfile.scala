package models.mpay.yaka

import models.mpay.MPayMode

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.03.17 11:33
  * Description: Модель конфигурации яндекс-кассы.
  */
trait IYakaProfile {

  /** URL экшена оплаты. */
  def eshopActionUrl: String

  /** HTTP-метод для экшена оплаты. Наверное всегда POST. */
  def eshopActionMethod: String


  /** id магазина. */
  def shopId: Long

  /** Режим работы платежной системы, соответствующий этой конфигурации. */
  def mode: MPayMode

  /** Является ли эта конфигурация демо-конфигурацией? */
  def isDemo = mode.isTest

  /** id витрины. */
  def scId: Long

  /** Пароль для md5-подписей.
    * @return None означает, что пароля не задано в конфигах. И при попытке посчитать md5 будет исключение.
    */
  def md5Password: Option[String]


  override def toString: String = {
    s"${getClass.getSimpleName}($mode,shop#$shopId,sc#$scId${md5Password.fold("")(_ => "," + Some("*").toString)})"
  }

}
