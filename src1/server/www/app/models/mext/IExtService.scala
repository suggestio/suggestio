package models.mext

import akka.actor.Actor
import io.suggest.primo.IStrId
import util.adv.ext.{AdvExtServiceActorFactory, IApplyServiceActor}
import util.ext.IExtServiceHelper

import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:09
 * Description: Интерфейс одного экземпляра мега-модели внешних сервисов.
 */

trait IExtService extends IStrId {

  /** URL главной страницы сервиса. */
  def mainPageUrl: String

  /** Отображамое имя, заданное через код в messages. */
  def nameI18N: String

  /** Дефолтовая цель размещения, если есть. При создании узлов дефолтовые цели создаются автоматом. */
  def dfltTargetUrl: Option[String]

  /** Код локализованного предложения "Я в фейсбук" */
  def iAtServiceI18N: String = "adv.ext.i.at." + strId

  /** Class tag для доступа к классу необходимой factory, собирающей service-акторы. */
  def extAdvServiceActorFactoryCt: ClassTag[IApplyServiceActor[Actor]]

  /** Человекочитабельный юзернейм (id страницы) suggest.io на стороне сервиса. */
  def myUserName: Option[String] = None

  /** Класс хелпера этого сервиса для получения его через DI. В этот класс уехала вся логика из модели. */
  def helperCt: ClassTag[IExtServiceHelper]


  // TODO Далее логика, которую надо перенести в util под покров DI.

  /**
    * Если логин через этот сервис поддерживается, то тут API.
    *
    * @return Some() если логин на suggest.io возможен через указанный сервис.
    */
  def loginProvider: Option[ILoginProvider] = None

}


trait IJsActorExtService extends IExtService {
  override def extAdvServiceActorFactoryCt  = ClassTag( classOf[AdvExtServiceActorFactory] )
}