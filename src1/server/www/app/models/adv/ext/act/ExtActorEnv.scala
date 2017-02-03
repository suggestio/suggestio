package models.adv.ext.act

import io.suggest.text.util.UrlUtil
import io.suggest.util.logs.IMacroLogs
import models.adv._
import models.adv.ext.MExtServiceInfo
import models.mctx.IContextUtilDi
import models.mext.MExtService
import util.ext.IExtServicesUtilDi
import util.n2u.IN2NodesUtilDi

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.03.15 15:03
 * Description: Описание окружения одного из акторов внешнего размещения.
 */

trait ExtActorEnv
  extends IMacroLogs
  with IExtServicesUtilDi
{

  /** Базовые аргументы ext-акторов. */
  def args: IExtActorArgs with WsMediatorRef

  /** Адресат, которому клиент должен адресовать сообщения. */
  def replyTo: String

  /** Текущий сервис, в котором задействован текущий актор. */
  def service: MExtService

  /** Инстанс хелпера сервиса. */
  lazy val serviceHelper = extServicesUtil.helperFor(service).get

  def serviceInfo: MExtServiceInfo = {
    MExtServiceInfo(
      name  = service.strId,
      appId = serviceHelper.APP_ID_OPT
    )
  }

}


/** Окружение service-актора. */
trait ExtServiceActorEnv extends ExtActorEnv {

  def args: IExtAdvServiceActorArgs

  override def service = args.service
}


/**
 * Базовое описание окружения актора [[ExtTargetActor]].
 * Нужно для описания внутренних моделей, имеющие доступ к этому окружению.
 */
trait ExtTargetActorEnv
  extends ExtActorEnv
    with IN2NodesUtilDi
    with IContextUtilDi
{

  /** Параметры вызова этого актора. */
  def args: IExtAdvTargetActorArgs

  /** [Основная] рекламная карточка. В args список карточек, но поддержка множества карточек пока не реализована. */
  protected def mad = args.request.mad

  /** Определить домен, в котором идёт публикация. Домен соц.сети по сути. */
  def getDomain: String = UrlUtil.url2dkey(args.target.target.url)

  /**
   * Генерация абс.ссылок на выдачу s.io.
   * @param ret Куда делается возврат.
   * @return Абсолютный URL в виде строки.
   */
  def getScUrl(ret: MExtReturn): String = {
    val b = ret.builder()
      .setAdnId( args.target.target.adnId )
      .setFocusedAdId( mad.idOrNull )
    for (prodId <- n2NodesUtil.madProducerId(mad)) {
      b.setFocusedProducerId(prodId)
    }
    ctxUtil.toScAbsUrl( b.toCall )
  }


  /** Генерация имени файла для отрендеренной карточки. */
  def ad2imgFileName: String = {
    args.qs.adId + "-" + args.request.mad.versionOpt.getOrElse(0L) + "." + serviceHelper.imgFmt.name
  }

}
