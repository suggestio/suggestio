package io.suggest.routes

import io.suggest.js.JsRoutesConst.GLOBAL_NAME
import io.suggest.proto.http.model.Route
import japgolly.univeq.UnivEq

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.03.15 11:16
 * Description: Роуты, ВОЗМОЖНО доступные из jsRoutes описываются здесь.
 *
 * Все js-роуты, используемые в sjs-проектах, объявлены здесь, но это не значит, что все эти роуты доступны
 * одновременно или всегда. В шаблоне, чей init-контроллер должен обращаться к какой-то роутере, должен присутствовать
 * вызов к сборке соответствующего javascriptRouter'а.
 */
object IJsRouter {
  @inline implicit def univEq[T <: IJsRouter]: UnivEq[T] = UnivEq.force
}

@js.native
trait IJsRouter extends js.Object {

  /** Все экспортированные контроллеры. */
  def controllers: Controllers = js.native

}

@JSGlobal( GLOBAL_NAME )
@js.native
object routes extends IJsRouter


/** Интерфейс routes.controllers с доступом к static-контроллеру. */
@js.native
sealed trait Controllers extends js.Object {

  /** Роуты для static-контроллера. */
  def Static: StaticCtlRoutes = js.native

  /** Роуты для img-контроллера. */
  def Img: ImgCtlRoutes = js.native

  /** Роуты для assets-контроллера. */
  def Assets: AssetsCtlRoutes = js.native

  /** Роуты для Ident-контроллера. */
  def Ident: IdentCtlRoutes = js.native

  /** Роуты для MarketLkAdn-контроллера. */
  def MarketLkAdn: MarketLkAdnCtlRoutes = js.native

  /** Доступ к HTTP-роутам до серверного контроллера LkAdEdit. */
  def LkAdEdit: LkAdEditCtlRoutes = js.native

  /** Доступ к роутам выдачи. */
  def Sc: ScCtlRoutes = js.native

  /** Объект с роутами серверного контроллера LkAdvGeo. */
  def LkAdvGeo: LkAdvGeoCtlRoutes = js.native

  /** Роуты LkNodes-контроллера. */
  def LkNodes: LkNodesCtlRoutes = js.native

  /** Роуты LkAds-контроллера. */
  def LkAds: LkAdsCtlRoutes = js.native

  /** Роуты LkAdnEdit-контроллера. */
  def LkAdnEdit: LkAdnEditCtlRoutes = js.native

  def LkBill2: LkBill2CtlRoutes = js.native

  def SysMdr: SysMdrCtlRoutes = js.native

  def SysMarket: SysMarketCtlRoutes = js.native

}


/** Интерфейс контроллера Static. */
@js.native
sealed trait StaticCtlRoutes extends js.Object {

  def popupCheckContent(): Route = js.native

  /** Роута для доступа к унифицированному websocket channel. */
  def wsChannel(ctxId: String): Route = js.native

  /** Роута до JSON-карты ресиверов.
    *
    * @param hashSum Для сервера - всегда обязательно.
    *                Но используется undefined для контр-аварийного кэширования в js (через cache.rewriteUrl).
    * @return Роута.
    */
  def advRcvrsMapJson(hashSum: js.UndefOr[Int]): Route = js.native

}


/** Интерфейс роутера ImgController'а. */
@js.native
sealed trait ImgCtlRoutes extends js.Object {

  /** Форма-окошко для кропа. */
  def imgCropForm(imgId: String, width: Int, height: Int): Route = js.native

  /** Запрос картинки-капчи. */
  def getCaptcha(): Route = js.native

}


@js.native
sealed trait AssetsCtlRoutes extends js.Object {

  def versioned(file: String): Route = js.native

  def at(file: String): Route = js.native

}


@js.native
sealed trait IdentCtlRoutes extends js.Object {

  def loginFormPage(args: js.Object = js.native): Route = js.native

  def mySioStartPage(): Route = js.native

  def rdrUserSomewhere(): Route = js.native

  def epw2LoginSubmit(r: js.UndefOr[String] = js.undefined): Route = js.native

  def epw2RegSubmit(): Route = js.native

  def idViaProvider(extServiceId: String, r: js.UndefOr[String] = js.undefined): Route = js.native

}


@js.native
sealed trait MarketLkAdnCtlRoutes extends js.Object {

  def lkList(): Route = js.native

  def showNodeAds(nodeId: String): Route = js.native

}


/** Интерфейс js-роутера для LkAdEdit-контроллера. */
@js.native
sealed trait LkAdEditCtlRoutes extends js.Object {

  def createAd(adId: String): Route = js.native

  def editAd(adId: String): Route = js.native

  def prepareImgUpload(adId: String = null, nodeId: String = null): Route = js.native

  def saveAdSubmit(adId: String = null, producerId: String = null): Route = js.native

  def deleteSubmit(adId: String): Route = js.native

}


/** Контроллер выдачи, а точнее его экшены. */
@js.native
sealed trait ScCtlRoutes extends js.Object {

  /** Ссылка на корень. */
  def geoSite(scJsState: js.Dictionary[js.Any] = js.native, siteQsArgs: js.Dictionary[js.Any] = js.native): Route = js.native

  /** Роута для доступа к pubApi. */
  def pubApi(args: js.Dictionary[js.Any]): Route = js.native

  /** Роута для автоматического сабмита ошибок на сервер. */
  def handleScError(): Route = js.native

}


/** Description: Интерфейс контроллер jsRouter'а с роутами экшенов LkAdvGeoTag контроллера. */
@js.native
sealed trait LkAdvGeoCtlRoutes extends js.Object {

  /** Роута постинга добавления нового тега. */
  def tagEditorAddTag(): Route = js.native

  /** Роута для поиска тегов  */
  def tagsSearch2(args: js.Dictionary[js.Any]): Route = js.native


  /** Роута для запроса ценника текущего размещения. */
  def getPriceSubmit(adId: String): Route = js.native

  /** Роута на страницу гео-размещения карточки. */
  def forAd(adId: String): Route = js.native

  /** Роута для итогового сабмита формы. */
  def forAdSubmit(adId: String): Route = js.native


  /** Роута получения содержимого попапа узла географической карты. */
  def rcvrMapPopup(adId: String, nodeId: String): Route = js.native


  /** Роута получения карты текущих георазмещений. */
  def existGeoAdvsMap(adId: String): Route = js.native

  /** Роута для получения содержимого попапа над указанной областью георазмещения. */
  def existGeoAdvsShapePopup(itemId: Double): Route = js.native

}


/** Доступные экшены контроллера LkNodes. */
@js.native
sealed trait LkNodesCtlRoutes extends js.Object {

  /** Роута списка под-узлов для указанного узла. */
  def nodeInfo(nodeId: String): Route = js.native

  /** Роута сабмита формы добавления нового узла. */
  def createSubNodeSubmit(parentId: String): Route = js.native

  /** Роута сабмита нового значения флага isEnabled. */
  def setNodeEnabled(nodeId: String, isEnabled: Boolean): Route = js.native

  /** Роута для удаления узла. */
  def deleteNode(nodeId: String): Route = js.native

  /** Сабмит редактирования узла. */
  def editNode(nodeId: String): Route = js.native

  /** Сабмит обновления данных размещения какой-то карточки на каком-то узле по rcvrKey. */
  def setAdv(adId: String, isEnabled: Boolean, onNodeRcvrKey: String): Route = js.native

  /** Сабмит обновлённых данных по тарификацию размещений на узле. */
  def setTfDaily(onNodeRcvrKey: String): Route = js.native

  def setAdvShowOpened(adId: String, isEnabled: Boolean, onNodeRcvrKey: String): Route = js.native

}


/** Роуты до LkAds-контроллера. */
@js.native
sealed trait LkAdsCtlRoutes extends js.Object {

  def adsPage(nodeKey: String): Route = js.native

  def getAds(rcvrKey: String, offset: Int): Route = js.native

}


/** Роуты до LkAdnEdit-контроллера. */
@js.native
sealed trait LkAdnEditCtlRoutes extends js.Object {

  def editNodePage(nodeId: String): Route = js.native

  def uploadImg(nodeId: String): Route = js.native

  def save(nodeId: String): Route = js.native

}


/** Роуты для LkBill2-контроллера. */
@js.native
sealed trait LkBill2CtlRoutes extends js.Object {

  def getOrder(orderId: js.UndefOr[Double]): Route = js.native

  def deleteItems( itemIds: js.Array[Double]): Route = js.native

  def cartSubmit(onNodeId: String): Route = js.native

  /** Получить бинарь с данными размещения по узлу. */
  def nodeAdvInfo(nodeId: String, forAdId: String = null): Route = js.native

}


/** Роуты для контроллера SysMdr. */
@js.native
sealed trait SysMdrCtlRoutes extends js.Object {

  def nextMdrInfo(args: js.Dictionary[js.Any]): Route = js.native

  def doMdr(mdrRes: js.Dictionary[js.Any]): Route = js.native

  def fixNode(nodeId: String): Route = js.native

}


/** Роуты для SysMarket. */
@js.native
trait SysMarketCtlRoutes extends js.Object {

  def showAdnNode(nodeId: String): Route = js.native

}
