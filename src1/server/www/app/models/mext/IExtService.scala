package models.mext

import akka.actor.Actor
import io.suggest.proto.http.HttpConst
import util.adv.ext.{AdvExtServiceActorFactory, IApplyServiceActor}
import util.ext.IExtServiceHelper

import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 19:09
 * Description: Интерфейс одного экземпляра мега-модели внешних сервисов.
 */

trait IExtService {

  /** Вернуть AdvExt API сервиса или exception, если API не доступно.
    * По-хорошему, здесь должен быть Option, но нормально отрефакторить - пока иные приоритеты.
    *
    * @see MExtService.hasAdvExt для проверки доступности этого опционального API.
    * @throws UnsupportedOperationException если данный сервис не поддерживает данное API.
    */
  def advExt: IAdvExtService =
    throw new UnsupportedOperationException(s"Service ${getClass.getSimpleName} does not support AdvExt")


  /**
    * Если логин через этот сервис поддерживается, то тут API.
    *
    * @return Some() если логин на suggest.io возможен через указанный сервис.
    */
  def ssLoginProvider: Option[ISsLoginProvider] = None

  /** css-класс для отображаемой кнопки логина. */
  def loginBtnCssClass: Option[String] =
    ssLoginProvider.map(_.ssProvName)

}


trait IJsActorExtService extends IAdvExtService {
  override def extAdvServiceActorFactoryCt = ClassTag( classOf[AdvExtServiceActorFactory] )
}


/** Интерфейс для сервиса внешнего размещения.
  * Изначально было прямо внутри [[IExtService]], но после необходимости подключения гос.услуг,
  * поддержка внешнего размещения стало опциональным, и вынесено на отдельный интерфейс.
  */
trait IAdvExtService {

  /** Дефолтовая цель размещения, если есть. При создании узлов дефолтовые цели создаются автоматом. */
  def dfltTargetUrl: Option[String]

  /** Class tag для доступа к классу необходимой factory, собирающей service-акторы. */
  def extAdvServiceActorFactoryCt: ClassTag[IApplyServiceActor[Actor]]

  /** Класс хелпера этого сервиса для получения его через DI. В этот класс уехала вся логика из модели. */
  def helperCt: ClassTag[IExtServiceHelper]

  /** CSP: Список разрешённых доменов-источников (или масок доменов). Вставляется в разные CSP *-src. */
  def cspSrcDomains: Iterable[String]

  /** CSP: Список поддерживаемых протоколов для обычных запросов. */
  def cspSrcProtos = HttpConst.Proto.HTTPS :: Nil

  /** CSP: Список поддерживаемых протоколов для коннекта из js. */
  //def cspConnectProtos = "wss" :: Nil   // wss:// на всякий случай. Изначально он нигде не фигурировал.

}